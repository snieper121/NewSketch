# 06 — Build Pipeline

## Обзор

Сборка APK на устройстве без Gradle. 9 фаз, выполняются последовательно в Foreground Service.

```
JSON → codegen → kotlinc → aapt2 compile → aapt2 link → ecj (R.java)
    → d8 (dex + multidex + desugar) → merge → zipalign → sign → install
```

## Foreground Service (API 29–36)

Сборка длится 5–15 мин. Без foreground service система убьёт процесс.

```kotlin
class BuildService : Service() {
    private var buildJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val projectId = intent?.getStringExtra("projectId") ?: return START_NOT_STICKY
        val notification = buildNotification("Сборка...", 0)

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        buildJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            buildPipeline.build(projectId).collect { progress ->
                updateNotification(progress)
                if (progress is BuildProgress.Success || progress is BuildProgress.Error) {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        buildJob?.cancel()
        super.onDestroy()
    }
}
```

Manifest:
```xml
<service
    android:name=".build.BuildService"
    android:foregroundServiceType="dataSync"
    android:exported="false" />
```

## Cancellation

Пользователь может отменить сборку в любой момент.

**Как это работает:**
1. UI отправляет `Intent` с action `ACTION_CANCEL` в BuildService
2. `BuildService.onStartCommand()` проверяет action
3. `buildJob?.cancel(CancellationException("Отменено пользователем"))`
4. `CoroutineScope` cancellation распространяется по всем корутинам pipeline
5. Каждая фаза проверяет `coroutineContext.ensureActive()` в loop'ах
6. Cleanup: удаляем частично созданные файлы в `build/tmp/`

```kotlin
suspend fun runPhase(phase: String, action: suspend () -> Unit) {
    coroutineContext.ensureActive() // до начала
    try {
        action()
    } finally {
        cleanupTempFiles(phase)
    }
}
```

Native-процессы (aapt2, zipalign) кидаются через `Process.destroy()` при cancellation.

## 9 фаз сборки

### Phase 1: Code Generation

```
design/*.json → generated/src/**/*.kt
```

Время: < 1 сек. Выполняется в main process до запуска service.

### Phase 2: Kotlin Compilation

```kotlin
val args = listOf(
    "-no-stdlib",  // stdlib явно в classpath
    "-Xplugin=${toolchain.composePlugin.absolutePath}",
    "-P", "plugin:androidx.compose.compiler.plugins.kotlin:generateFunctionKeyMetaClasses=false",
    "-classpath", classpath.joinToString(File.pathSeparator),
    "-d", classesDir.absolutePath,
    "-jvm-target", "17"
) + sourceFiles.map { it.absolutePath }

val exitCode = inProcessCompiler.compileKotlin(args)
```

Classpath: kotlin-stdlib, android.jar, compose-classpath.jar.

Время: 2–10 мин (зависит от количества файлов и устройства).

### Phase 3: Resource Compilation (aapt2 compile)

```bash
aapt2 compile res/values/strings.xml -o build/res-compiled/
aapt2 compile res/values/themes.xml -o build/res-compiled/
aapt2 compile res/mipmap-hdpi/ic_launcher.png -o build/res-compiled/
```

Каждый ресурс компилируется отдельно → `.flat` файлы. Incremental: только изменённые ресурсы.

### Phase 4: Resource Linking (aapt2 link)

```bash
aapt2 link \
  build/res-compiled/*.flat \
  -I android.jar \
  --manifest AndroidManifest.xml \
  --java build/gen-src/ \
  -o build/res.apk
```

Выход:
- `R.java` в `build/gen-src/<package>/R.java`
- `res.apk` (ресурсы в APK-формате, без dex)

### Phase 5: Compile R.java через ecj

**Важно:** НЕ kotlinc. kotlinc не компилирует .java корректно в production.

```kotlin
val args = listOf(
    "-source", "17",
    "-target", "17",
    "-nowarn",
    "-d", classesDir.absolutePath,
    "-classpath", androidJar.absolutePath,
    rJavaFile.absolutePath
)
val exitCode = inProcessCompiler.compileJava(args)
```

Время: < 5 сек (один мелкий файл).

### Phase 6: Dex (d8) + Multidex + Desugaring

```kotlin
val d8Args = mutableListOf(
    "--release",
    "--min-api", project.minSdk.toString(),
    "--output", dexDir.absolutePath
)

// Multidex — всегда для Compose приложений
d8Args += listOf("--main-dex-rules", mainDexRulesFile.absolutePath)

// Desugaring — если minSdk < 26
if (project.minSdk < 26) {
    d8Args += listOf(
        "--desugared-lib", desugarLibJson.absolutePath,
        "--desugared-lib-pg-conf-output", keepRulesFile.absolutePath,
        "--lib", androidJar.absolutePath
    )
}

d8Args += allClassFiles
d8Args += classpathJars  // kotlin-stdlib, compose-runtime, etc

inProcessD8.run(d8Args)
```

Выход: `classes.dex`, возможно `classes2.dex`, `classes3.dex` (multidex).

### Phase 7: Merge into APK

Берём `res.apk` из Phase 4, добавляем dex:

```kotlin
fun mergeIntoApk(resApk: File, dexDir: File, outputApk: File) {
    ZipOutputStream(outputApk.outputStream()).use { zos ->
        // Копируем всё из res.apk (resources + manifest)
        ZipFile(resApk).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                zos.putNextEntry(ZipEntry(entry.name))
                zip.getInputStream(entry).copyTo(zos)
                zos.closeEntry()
            }
        }
        // Добавляем все classes*.dex
        dexDir.listFiles { _, name -> name.matches(Regex("classes\\d*\\.dex")) }
            ?.forEach { dex ->
                zos.putNextEntry(ZipEntry(dex.name))
                dex.inputStream().copyTo(zos)
                zos.closeEntry()
            }
        // Desugar lib (если minSdk < 26)
        if (project.minSdk < 26) {
            // desugar_jdk_libs classes уже включены в classes*.dex от d8
        }
    }
}
```

**Альтернатива:** использовать `com.android.tools.apkzlib` для более корректной работы с zip-entries (alignment, no compression для arsc, etc). Но в MVP достаточно ZipOutputStream.

### Phase 8: Zipalign

```bash
zipalign -f -p 4 build/app-unsigned.apk build/app-aligned.apk
```

`-p 4` — page-align для .so (на будущее). `-f` — force overwrite.

### Phase 9: Sign (APK Signature Scheme v2+)

```kotlin
val signArgs = listOf(
    "sign",
    "--ks", debugKeystore.absolutePath,
    "--ks-pass", "pass:android",
    "--key-pass", "pass:android",
    "--v1-signing-enabled", "true",   // для старых API
    "--v2-signing-enabled", "true",
    "--v3-signing-enabled", "true",
    "--in", alignedApk.absolutePath,
    "--out", signedApk.absolutePath
)
inProcessApkSigner.run(signArgs)
```

Debug keystore генерируется автоматически при первой сборке:
```kotlin
class KeystoreGenerator {
    fun generateDebugKeystore(keystore: File) {
        // Через Java KeyStore API, не keytool
        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, "android".toCharArray())
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val cert = generateSelfSignedCert(keyPair, "CN=AiIDE Debug, O=Android")
        ks.setKeyEntry("androiddebugkey", keyPair.private, "android".toCharArray(), arrayOf(cert))
        keystore.outputStream().use { ks.store(it, "android".toCharArray()) }
    }
}
```

## Incremental Build

При повторной сборке:

1. **Codegen:** перегенерировать только изменённые screens (по timestamp JSON)
2. **kotlinc:** компилировать только изменённые .kt файлы (сравнение hash)
3. **aapt2 compile:** только изменённые ресурсы
4. **aapt2 link:** всегда (быстрый)
5. **ecj (R.java):** всегда (1 файл, < 5 сек)
6. **d8:** partial dex merge (только новые .class)
7. **Phases 7–9:** всегда выполняются (быстрые, < 5 сек total)

```kotlin
data class BuildCache(
    val sourceHashes: Map<String, String>,   // file path → SHA-256 of content
    val lastBuildTimestamp: Long,
    val resolvedDependencies: List<String>   // classpath snapshot
)
```

Incremental build: 30–90 сек vs 5–15 мин full.

## Error Handling

```kotlin
sealed class BuildError(
    val phase: String,
    override val message: String,
    override val cause: Throwable? = null
) : Throwable(message, cause) {

    class CompilationError(
        val file: String,
        val line: Int,
        val column: Int,
        val error: String
    ) : BuildError("kotlinc", "$file:$line:$column: $error")

    class JavaCompilationError(val error: String)
        : BuildError("ecj", error)

    class ResourceError(val resource: String, val error: String)
        : BuildError("aapt2", "$resource: $error")

    class DexError(val error: String)
        : BuildError("d8", error)

    class SigningError(val error: String)
        : BuildError("apksigner", error)

    class OutOfMemory(phase: String)
        : BuildError(phase, "Недостаточно памяти. Закройте другие приложения.")

    class Cancelled
        : BuildError("user", "Сборка отменена")

    class ToolchainMissing(val tool: String)
        : BuildError("toolchain", "Toolchain component missing: $tool")
}
```

Ошибки компиляции парсятся из stdout kotlinc → показываются с номером строки и файлом в UI.

## OOM Protection

```kotlin
fun checkMemoryBeforeBuild(): BuildError? {
    val runtime = Runtime.getRuntime()
    val availableMb = (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / (1024 * 1024)
    val requiredMb = 512L // минимум для kotlinc
    return if (availableMb < requiredMb) {
        BuildError.OutOfMemory("pre-check")
    } else null
}
```

Если памяти мало → предупреждение пользователю до старта. При OOM во время сборки → graceful catch, сообщение «закройте приложения».

## Thermal Throttling

```kotlin
fun getThermalStatus(): Int {
    if (Build.VERSION.SDK_INT >= 29) {
        val powerManager = getSystemService(PowerManager::class.java)
        return powerManager.currentThermalStatus
    }
    return 0
}

// Pipeline:
suspend fun throttleIfHot() {
    if (getThermalStatus() >= PowerManager.THERMAL_STATUS_SEVERE) {
        Timber.w("Thermal throttling — pause 30s")
        delay(30_000)
    }
}
```

Вызов между фазами.

## Install & Launch

```kotlin
fun installApk(apkFile: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        apkFile
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
```

На API 30+ нужен `<queries>` в manifest (см. 01-architecture).

### Silent install (будущее)

Для v1.0+ — `PackageInstaller` API с permission `REQUEST_INSTALL_PACKAGES`. Пользователь всё ещё подтверждает в system UI, но UX лучше.

## Progress Reporting

```kotlin
sealed class BuildProgress {
    data class Phase(
        val name: String,
        val current: Int,
        val total: Int,
        val subtask: String = ""
    ) : BuildProgress()

    data class Log(
        val line: String,
        val level: LogLevel = LogLevel.INFO
    ) : BuildProgress()

    data class Error(val error: BuildError) : BuildProgress()

    data class Success(
        val apkFile: File,
        val durationMs: Long,
        val apkSizeBytes: Long
    ) : BuildProgress()

    enum class LogLevel { DEBUG, INFO, WARN, ERROR }
}

// Передаётся через StateFlow из Service → UI
val progress: SharedFlow<BuildProgress>
```

Notification обновляется на каждой фазе с процентом.

## Ограничения (MVP)

| Что | Ограничение | Когда снимаем |
|------|-------------|---------------|
| Native libraries (.so) | НЕ поддерживаются | v1.0+ (extract + copy pipeline) |
| Custom dependencies | Только whitelist Compose BOM | v0.2 (Maven resolver) |
| Resource merging с AAR библиотеками | НЕТ | v1.0+ (aapt2 merge chain) |
| Release signing (user keystore) | debug only | v0.2+ (через SAF) |
| ProGuard/R8 для generated apps | НЕТ | v0.2+ (опциональный шаг между Phase 2 и 6) |
| Watch mode (continuous build) | НЕТ | v1.0+ |
| Flavors / build variants | НЕТ | v1.0+ |

## Performance Targets

| Метрика | Target | Acceptable |
|---------|--------|-----------|
| Hello World (1 screen) full | < 3 min | < 5 min |
| 5 screens full | < 8 min | < 15 min |
| 5 screens incremental | < 90 sec | < 3 min |
| Phase 1 (codegen) | < 500 ms | < 2 sec |
| Phase 2 (kotlinc, 1 file) | < 30 sec | < 60 sec |
| Phase 5 (ecj R.java) | < 5 sec | < 15 sec |
| Phases 7-9 (merge+sign) | < 5 sec | < 15 sec |
| Build RAM peak | < 1.2 GB | < 1.5 GB |
