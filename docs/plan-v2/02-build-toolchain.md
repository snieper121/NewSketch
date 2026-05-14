# 02 — Build Toolchain

## Обзор

On-device сборка Compose-приложений без Gradle. Все инструменты — Java/Kotlin JARs (запускаются in-process через DexClassLoader) или native бинарники (запускаются через ProcessBuilder из app-private dir).

## Инструменты

| Инструмент | Тип | Размер | Назначение |
|---|---|---|---|
| kotlinc.jar (fat-jar) | Java | ~59 MB | Kotlin → .class |
| compose-compiler-plugin-embeddable.jar | Java | ~5 MB | @Composable transform |
| **ecj.jar** (Eclipse JDT compiler) | Java | ~2 MB | **R.java → .class** |
| android.jar (API 35) | Stubs | ~30 MB | Platform API stubs для classpath |
| Compose + AndroidX fat-classpath.jar | Java | ~25 MB | Runtime classpath |
| core-lambda-stubs.jar | Java | ~1 MB | Desugaring lambdas stubs |
| desugar-lib.jar | Java | ~1.5 MB | java.time/java.util.stream для API < 26 |
| aapt2 | Native | ~5 MB | Resource compilation + linking |
| d8.jar (R8) | Java | ~5 MB | .class → .dex, multidex, desugaring |
| zipalign | Native | ~1 MB | APK alignment |
| apksigner.jar | Java | ~2 MB | APK signing |
| **Итого** | | **~136 MB** | |

## Почему ecj для R.java

aapt2 генерирует `R.java` — это **Java-файл**, не Kotlin. kotlinc не компилирует .java корректно в production (есть `-Xallow-java-sources`, но это экспериментально и ненадёжно для R-class).

**Решение:** Eclipse JDT Compiler (ecj). Причины:
1. **Чистая Java** — нет native-зависимостей, работает через DexClassLoader
2. **Маленький** — ~2 MB против ~50 MB javac
3. **Standalone** — не требует JDK
4. **Apache 2.0 compatible** (EPL, но для ecj можно использовать)

Maven coordinates: `org.eclipse.jdt:ecj:3.40.0` (или последняя стабильная).

**Альтернатива:** можно скомпилировать R.java через **d8 как .dex напрямую** (d8 принимает .java через wrapper), но это хуже для incremental builds.

## Execution Model (API 29–36)

### Java-инструменты → In-Process через DexClassLoader

На Android нет системного `java`. ProcessBuilder с `java -jar` невозможен. Решение — загрузка JAR'ов как DEX в процесс IDE.

**Подготовка (один раз при скачивании):**
```
kotlinc.jar → d8 → kotlinc.dex (pre-dexed)
ecj.jar → d8 → ecj.dex
d8.jar → d8 → d8-tool.dex
apksigner.jar → d8 → apksigner.dex
```

Pre-dex выполняется на CDN/CI.

**Вызов в runtime:**
```kotlin
class InProcessCompiler(private val toolchainDir: File) {

    private val kotlincClassLoader = DexClassLoader(
        File(toolchainDir, "kotlinc.dex").absolutePath,
        toolchainDir.resolve("oat").absolutePath,
        null,
        this::class.java.classLoader
    )

    private val ecjClassLoader = DexClassLoader(
        File(toolchainDir, "ecj.dex").absolutePath,
        toolchainDir.resolve("oat").absolutePath,
        null,
        this::class.java.classLoader
    )

    fun compileKotlin(args: List<String>): Int {
        val clazz = kotlincClassLoader.loadClass(
            "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
        )
        val instance = clazz.getDeclaredConstructor().newInstance()
        val execMethod = clazz.getMethod("exec", Class.forName("[Ljava.lang.String;"))
        val result = execMethod.invoke(instance, args.toTypedArray())
        return result.toString().let { if (it == "OK") 0 else 1 }
    }

    fun compileJava(args: List<String>): Int {
        val clazz = ecjClassLoader.loadClass(
            "org.eclipse.jdt.internal.compiler.batch.Main"
        )
        // ecj Main.compile(String[] args, PrintWriter out, PrintWriter err, CompilationProgress progress)
        val mainInstance = clazz.getDeclaredConstructor(
            PrintWriter::class.java, PrintWriter::class.java, Boolean::class.javaPrimitiveType
        ).newInstance(PrintWriter(System.out), PrintWriter(System.err), false)
        val compileMethod = clazz.getMethod("compile", Class.forName("[Ljava.lang.String;"))
        return if (compileMethod.invoke(mainInstance, args.toTypedArray()) as Boolean) 0 else 1
    }
}
```

**Почему не ProcessBuilder для Java-тулов:**
- Android не имеет `java` binary
- API 34+ W^X запрещает writable+executable memory regions
- In-process DexClassLoader работает на всех API 29–36

### Native-инструменты → ProcessBuilder из app-private

`aapt2` и `zipalign` — native ELF бинарники. Запускаются через ProcessBuilder:

```kotlin
fun runAapt2(args: List<String>): ProcessResult {
    val aapt2 = File(toolchainDir, "aapt2")
    aapt2.setExecutable(true)
    val process = ProcessBuilder(listOf(aapt2.absolutePath) + args)
        .directory(projectBuildDir)
        .redirectErrorStream(true)
        .start()
    return ProcessResult(
        exitCode = process.waitFor(),
        output = process.inputStream.bufferedReader().readText()
    )
}
```

**API 35+ требование:** native бинарники должны быть собраны с 16-KB page alignment (`-Wl,-z,max-page-size=16384`). Использовать SDK Build-Tools ≥ 35.0.0.

## Multidex (обязательно)

Compose-приложения легко превышают лимит 65536 методов в dex. Нужен multidex.

```kotlin
val d8Args = listOf(
    "--release",
    "--min-api", project.minSdk.toString(),
    "--output", dexDir.absolutePath,
    "--main-dex-rules", mainDexRulesFile.absolutePath,
) + classFiles + classpathJars
```

**main-dex-rules:**
```
-keep class ${packageName}.MainActivity
-keep class androidx.multidex.** { *; }
-keep class * extends android.app.Application
```

Если minSdk ≥ 21 — multidex нативный (несколько classes.dex, classes2.dex, classes3.dex). Если < 21 — нужна androidx.multidex library (но в наш minSdk 21+ generated apps входит).

## Desugaring

Generated apps с minSdk < 26 не имеют `java.time`, `java.util.stream`, `Optional`. d8 умеет desugar'ить через специальный JSON spec.

```kotlin
val d8Args = listOf(
    "--release",
    "--min-api", minSdk.toString(),
    "--desugared-lib", desugarLibJson.absolutePath,
    "--desugared-lib-pg-conf-output", keepRulesFile.absolutePath,
    "--lib", androidJar.absolutePath,
    // ...
)
```

Артефакты:
- `desugar_jdk_libs_configuration-<version>.json` — спецификация desugar
- `desugar_jdk_libs-<version>.jar` — runtime library (добавляется в classpath)

**Поведение:**
- minSdk ≥ 26: desugaring НЕ нужен, skip
- minSdk < 26: d8 делает desugar, добавляется desugar_jdk_libs в APK

## Classpath для Compose-компиляции

kotlinc требует полный classpath всех зависимостей:

```
kotlin-stdlib.jar
kotlinx-coroutines-core.jar
kotlinx-coroutines-android.jar
android.jar (platform stubs)
compose-runtime.jar
compose-ui.jar
compose-ui-geometry.jar
compose-foundation.jar
compose-material3.jar
compose-animation.jar
navigation-compose.jar
navigation-runtime.jar
activity-compose.jar
lifecycle-runtime-compose.jar
lifecycle-viewmodel-compose.jar
core-ktx.jar
```

**Решение:** один pre-built `compose-classpath.jar` (fat-jar из всех AAR classes.jar). Обновляется вместе с toolchain.

## Download Strategy

### Фазы загрузки (lazy)

| Фаза | Что | Когда |
|------|-----|-------|
| Phase 0 | android.jar + kotlin-stdlib | Первый запуск |
| Phase 1 | kotlinc.dex + compose-plugin + ecj.dex | Первый Build |
| Phase 2 | compose-classpath.jar | Первый Build |
| Phase 3 | aapt2 + d8 + zipalign + apksigner | Первый Build |
| Phase 4 | desugar libs (если minSdk < 26) | Первый Build для API 21–25 |

### Источники

Все URLs версионируются через toolchain manifest. Pre-dex'нутые файлы с собственного CDN, non-dex — с Maven Central / Google Maven как fallback.

```kotlin
val TOOL_URLS = mapOf(
    "kotlinc.dex" to "https://our-cdn.example/toolchain/v1/kotlinc-2.1.0.dex",
    "compose-plugin.dex" to "https://our-cdn.example/toolchain/v1/compose-plugin-2.1.0.dex",
    "ecj.dex" to "https://our-cdn.example/toolchain/v1/ecj-3.40.0.dex",
    "compose-classpath.jar" to "https://our-cdn.example/toolchain/v1/compose-classpath-bom-2024.12.jar",
    "android.jar" to "https://our-cdn.example/toolchain/v1/android-35.jar",
    "aapt2" to "https://our-cdn.example/toolchain/v1/aapt2-35.0.0-linux-aarch64",
    "d8.dex" to "https://our-cdn.example/toolchain/v1/d8-8.7.dex",
    "zipalign" to "https://our-cdn.example/toolchain/v1/zipalign-35.0.0-linux-aarch64",
    "apksigner.dex" to "https://our-cdn.example/toolchain/v1/apksigner-35.0.0.dex",
    "desugar-lib.jar" to "https://our-cdn.example/toolchain/v1/desugar_jdk_libs-2.1.0.jar",
    "desugar-lib-spec.json" to "https://our-cdn.example/toolchain/v1/desugar_jdk_libs_configuration.json",
)
```

### Integrity

Каждый файл проверяется SHA-256 checksum из manifest.json на CDN:
```json
{
  "version": 1,
  "toolchain_version": "2024.12",
  "kotlin_version": "2.1.0",
  "compose_compiler_version": "2.1.0",
  "compose_bom_version": "2024.12.01",
  "build_tools_version": "35.0.0",
  "android_platform": 35,
  "ecj_version": "3.40.0",
  "tools": {
    "kotlinc.dex": { "sha256": "abc123...", "size": 62000000 },
    "ecj.dex": { "sha256": "def456...", "size": 2100000 }
  }
}
```

### Verification on each load (обязательно)

**После скачивания** SHA-256 проверяется один раз. Но на API 34+ с W^X enforcement файл может быть подменён (другое приложение с root, malware через USB). Поэтому **SHA-256 проверяется ПЕРЕД КАЖДОЙ загрузкой** через DexClassLoader:

```kotlin
class SecureDexLoader(private val manifest: ToolchainManifest) {
    private val cache = mutableMapOf<String, DexClassLoader>()

    fun load(toolName: String): DexClassLoader {
        val file = File(toolchainDir, toolName)
        val expected = manifest.tools[toolName]?.sha256
            ?: throw ToolchainError.Unknown(toolName)

        // Верификация перед load (каждый раз)
        val actual = computeSha256(file)
        if (actual != expected) {
            file.delete()
            throw ToolchainError.IntegrityMismatch(toolName, expected, actual)
        }

        return cache.getOrPut(toolName) {
            DexClassLoader(
                file.absolutePath,
                File(toolchainDir, "oat").absolutePath,
                null,
                javaClass.classLoader
            )
        }
    }
}
```

Производительность: SHA-256 для 60 MB файла ~300 мс на среднем устройстве. Выполняется один раз за build (не в цикле), overhead приемлемый.

**После записи** — выставить read-only permission:
```kotlin
file.setReadOnly()
file.setWritable(false, false) // не writable никому
```

### Cert pinning для CDN

Toolchain download — **обязательно с certificate pinning**. MitM → RCE.

```kotlin
val client = OkHttpClient.Builder()
    .certificatePinner(
        CertificatePinner.Builder()
            .add("toolchain.ai-ide.example", "sha256/PRIMARY_CERT_HASH")
            .add("toolchain.ai-ide.example", "sha256/BACKUP_CERT_HASH")
            .build()
    )
    .build()
```

Два pin'а (primary + backup) — обязательно для смены сертификата без breaking existing clients.

## Incremental Compilation

Полная пересборка 5-экранного проекта — 5–15 мин. Инкрементальная — 30–90 сек.

**Стратегия:**
1. Хранить `.class` файлы от предыдущей сборки
2. При изменении `screens/main.json` → перегенерировать только `MainScreen.kt` → перекомпилировать только его
3. Re-dex только изменённые .class файлы (d8 поддерживает partial dex merge)
4. aapt2 compile — только изменённые ресурсы (aapt2 работает с отдельными файлами)

**Кэш:**
```
build/
├── classes/          ← .class файлы (сохраняются между сборками)
├── dex/              ← classes.dex, classes2.dex (multidex)
├── res-compiled/     ← .flat файлы от aapt2
├── intermediates/    ← R.java, merged manifest
└── output/           ← final APK
```

## Версионирование Toolchain

Compose Compiler Plugin **жёстко привязан** к версии Kotlin:
- Kotlin 2.1.0 → Compose Plugin 2.1.0
- Нельзя mix-and-match

**Toolchain manifest** на CDN определяет совместимые наборы. IDE проверяет обновления toolchain раз в неделю (настраиваемо).

## Ограничения и workarounds

| Проблема | Решение |
|----------|---------|
| RAM < 4 GB → OOM при kotlinc | Ограничить heap `-Xmx1g`, компилировать по 1–2 файла за раз |
| Thermal throttling | Пауза между фазами, мониторинг через `BatteryManager` |
| API 34+ W^X | In-process DexClassLoader (не exec) |
| API 35+ 16-KB pages | Native tools из Build-Tools ≥ 35.0.0 |
| Нет `java` binary | Всё через DexClassLoader |
| R.java компиляция | ecj через DexClassLoader |
| 65k method limit | d8 multidex (`--main-dex-rules`) |
| Java 8+ API на API 21–25 | d8 desugaring (`--desugared-lib`) |
| 130+ MB download | Lazy phases + resume support |
| Compose BOM updates | Toolchain manifest versioning |
| Native libraries (.so) пользовательских dependencies | MVP: не поддерживается. v1.0+: extract и copy в APK |
| Resource merging с библиотеками | MVP: только свои ресурсы. v1.0+: aapt2 merge pipeline |
