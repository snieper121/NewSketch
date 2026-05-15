---
name: ai-ide-build-pipeline
description: "Ai IDE build pipeline: 9 фаз сборки APK на устройстве, BuildPhase interface, BuildPipeline оркестратор, BuildService. Триггеры: build pipeline, build phase, compile APK, on-device build, BuildService, BuildPipeline, dex, aapt2, zipalign, apksigner."
---

# Ai IDE — Build Pipeline

## Архитектура

On-device сборка APK без Gradle. 9 фаз выполняются последовательно через `BuildPipeline`.

### Ключевые файлы

| Файл | Роль |
|------|------|
| `build/BuildPhase.kt` | Interface: `name` + `suspend fun execute(context): PhaseResult` |
| `build/BuildPipeline.kt` | Оркестратор: запускает фазы, публикует progress/logs/result через SharedFlow |
| `build/model/BuildModels.kt` | BuildContext, BuildProgress, PhaseResult (sealed) |
| `build/BuildService.kt` | ForegroundService с cancellation support |
| `build/KeystoreGenerator.kt` | Генерация debug keystore через Java KeyStore API |
| `build/ApkInstaller.kt` | Установка APK через FileProvider + Intent |

### 9 фаз сборки

```
1. CodeGenPhase        — генерирует .kt файлы (сейчас hardcoded Hello World)
2. KotlinCompilePhase  — kotlinc через DexClassLoader → .class файлы
3. ResourceCompilePhase — aapt2 compile ресурсов → .flat файлы
4. ResourceLinkPhase   — aapt2 link → R.java + resources.arsc
5. RJavaCompilePhase   — ecj компилирует R.java → .class
6. DexPhase            — d8 конвертирует .class → .dex (multidex)
7. PackagePhase        — zip dex + resources → unsigned APK
8. AlignPhase          — zipalign (native binary через ProcessBuilder)
9. SignPhase           — apksigner (через DexClassLoader)
```

### BuildContext

```kotlin
data class BuildContext(
    val buildDir: File,          // tmp директория для артефактов
    val packageName: String,     // my.company.ai.sample
    val sourceCode: Map<String, String>,  // filename → content
    val resources: Map<String, ByteArray>, // res path → data
    val toolchainDir: File,      // где лежат .dex и native binaries
    val keystoreFile: File,      // debug keystore
    val outputApk: File,         // итоговый APK
)
```

### PhaseResult

```kotlin
sealed class PhaseResult {
    data object Success : PhaseResult()
    data class Failure(val message: String, val cause: Throwable? = null) : PhaseResult()
}
```

### BuildPipeline — progress и logs

```kotlin
class BuildPipeline(private val phases: List<BuildPhase>) {
    val progress: Flow<BuildProgress>   // имя фазы, шаг, total
    val logs: Flow<String>              // текстовые логи
    val result: Flow<PhaseResult>       // итог (Success/Failure)
    
    suspend fun run(context: BuildContext)
}
```

### Интеграция с UI

- `BuildViewModel` запускает `BuildService.startBuild()`
- `BuildScreen` отображает логи и progress bar
- `BuildService` — ForegroundService с notification

### Текущее состояние (M0)

- Фазы 1-6 implemented
- Фазы 7-9 в `PackagePhase` (объединены)
- CodeGenPhase — hardcoded SampleProject.kt
- SHA-256 checksums = "TODO" (нужно вычислить)

### Риски

- DexClassLoader + W^X на API 34+ → тестировать
- aapt2 native binary может не работать на некоторых устройствах
- OOM при компиляции больших проектов → `-Xmx1g`, chunked compilation
