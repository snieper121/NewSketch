# 13 — Toolchain Sources & Implementation Order

## Этот файл — точка входа для разработки. Читать ПЕРВЫМ.

---

## Часть 1: Toolchain Sources (зафиксировать ДО кода)

### Откуда качать каждый инструмент

| Tool | Source | URL pattern | Pre-dex? |
|------|--------|-------------|----------|
| kotlinc | Maven Central | `repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-compiler-embeddable/2.1.0/kotlin-compiler-embeddable-2.1.0.jar` | Да → .dex |
| compose-compiler-plugin | Maven Central | `repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-compose-compiler-plugin-embeddable/2.1.0/kotlin-compose-compiler-plugin-embeddable-2.1.0.jar` | Да → .dex |
| ecj | Maven Central | `repo1.maven.org/maven2/org/eclipse/jdt/ecj/3.39.0/ecj-3.39.0.jar` | Да → .dex |
| kotlin-stdlib | Maven Central | `repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.1.0/kotlin-stdlib-2.1.0.jar` | Нет (classpath) |
| aapt2 | Google Maven | `dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/8.10.0-12006003/aapt2-8.10.0-12006003-linux.jar` (внутри — native binary) | Extract binary |
| d8 (R8) | Google Maven | `dl.google.com/dl/android/maven2/com/android/tools/r8/8.10.22/r8-8.10.22.jar` | Да → .dex |
| android.jar | Android SDK | `dl.google.com/android/repository/platform-35_r02.zip` (внутри `android.jar`) | Нет (classpath) |
| apksigner | Build-Tools | `dl.google.com/android/repository/build-tools_r35-linux.zip` (внутри `lib/apksigner.jar`) | Да → .dex |
| zipalign | Build-Tools | Тот же zip (native binary `zipalign`) | Extract binary |

### SHA-256 (вычислить при первой загрузке, зафиксировать в коде)

```kotlin
object ToolchainChecksums {
    // TODO: вычислить реальные SHA-256 при первом скачивании
    // Формат: filename → sha256
    val CHECKSUMS = mapOf(
        "kotlinc-embeddable-2.1.0.jar" to "TODO_COMPUTE_ON_FIRST_DOWNLOAD",
        "kotlin-compose-compiler-plugin-embeddable-2.1.0.jar" to "TODO_COMPUTE",
        "ecj-3.39.0.jar" to "TODO_COMPUTE",
        "kotlin-stdlib-2.1.0.jar" to "TODO_COMPUTE",
        "r8-8.10.22.jar" to "TODO_COMPUTE",
        "aapt2" to "TODO_COMPUTE",
        "android-35.jar" to "TODO_COMPUTE",
        "apksigner.jar" to "TODO_COMPUTE",
        "zipalign" to "TODO_COMPUTE",
    )
}
```

**Как получить SHA-256:**
```bash
curl -sL <URL> | sha256sum
```

### Pre-dex pipeline (выполняется на CI/dev machine, не на устройстве)

```bash
# 1. Скачать JAR
wget https://repo1.maven.org/.../kotlin-compiler-embeddable-2.1.0.jar

# 2. Конвертировать в DEX через d8
java -jar d8.jar --release --output kotlinc-2.1.0.dex kotlin-compiler-embeddable-2.1.0.jar

# 3. Вычислить SHA-256
sha256sum kotlinc-2.1.0.dex > kotlinc-2.1.0.dex.sha256

# 4. Загрузить на CDN (или использовать GitHub Releases)
```

### CDN варианты (от простого к сложному)

1. **GitHub Releases** (бесплатно, просто) — создать repo `ai-ide-toolchain`, загрузить pre-dex'нутые файлы как release assets
2. **Cloudflare R2** (бесплатно до 10 GB/мес) — S3-compatible, быстрый
3. **Собственный сервер** — полный контроль, но maintenance

**Рекомендация для MVP:** GitHub Releases. URL стабильный, бесплатный, SHA-256 в release notes.

---

## Часть 2: Порядок разработки (пошагово)

### Фаза 0: Подготовка toolchain (1-2 дня, на ПК)

```
0.1. Скачать все JAR'ы с Maven Central / Google Maven
0.2. Pre-dex каждый через d8 → .dex файлы
0.3. Вычислить SHA-256 для каждого .dex и native binary
0.4. Загрузить на GitHub Releases (или CDN)
0.5. Заполнить ToolchainChecksums.kt реальными хешами
0.6. Проверить что URL'ы доступны (curl)
```

### Фаза 1: M0 PoC — Build Pipeline (3-5 недель)

```
1.1.  build/ToolchainConfig.kt — URLs, versions, checksums (const)
1.2.  build/ToolchainDownloader.kt — HTTP download + SHA-256 verify + setReadOnly
1.3.  build/ToolchainManager.kt — ensureInstalled(), paths, status
1.4.  build/SecureDexLoader.kt — SHA-256 check before each DexClassLoader load
1.5.  build/KotlinCompiler.kt — interface
1.6.  build/InProcessKotlinCompiler.kt — DexClassLoader + K2JVMCompiler.exec()
1.7.  build/JavaCompiler.kt — interface
1.8.  build/EcjJavaCompiler.kt — DexClassLoader + ecj Main
1.9.  build/NativeToolRunner.kt — ProcessBuilder для aapt2/zipalign
1.10. build/KeystoreGenerator.kt — Java KeyStore API
1.11. build/BuildPhase.kt — interface
1.12. build/phases/CodegenPhase.kt — hardcoded Hello World .kt
1.13. build/phases/KotlinCompilePhase.kt
1.14. build/phases/ResourceCompilePhase.kt — aapt2 compile
1.15. build/phases/ResourceLinkPhase.kt — aapt2 link → R.java
1.16. build/phases/RJavaCompilePhase.kt — ecj
1.17. build/phases/DexPhase.kt — d8 + multidex
1.18. build/phases/MergePhase.kt — zip dex + resources
1.19. build/phases/AlignPhase.kt — zipalign
1.20. build/phases/SignPhase.kt — apksigner
1.21. build/BuildPipeline.kt — chain of phases
1.22. build/BuildProgress.kt — sealed class
1.23. build/BuildError.kt — sealed class
1.24. build/BuildService.kt — foreground service + cancellation
1.25. build/ApkInstaller.kt — FileProvider + Intent
1.26. ui/screens/build/BuildViewModel.kt
1.27. ui/screens/build/BuildScreen.kt
1.28. Добавить route BUILD в AppNavHost
1.29. Тестовая кнопка в Home → Build Hello World
```

### Фаза 2: JSON-интеграция (после M0 success)

```
2.1.  data/JsonModule.kt
2.2.  Расширить ProjectStorage: readJson/writeJson
2.3.  data/repository/ProjectModelRepository.kt
2.4.  Создание проекта → пишет design/*.json
2.5.  domain/SchemaValidator.kt
2.6.  domain/SchemaMigrator.kt
```

### Фаза 3: Codegen (KotlinPoet)

```
3.1.  Добавить kotlinpoet в app/build.gradle.kts dependencies
3.2.  codegen/ProjectGenerator.kt
3.3.  codegen/ManifestGen.kt
3.4.  codegen/MainActivityGen.kt
3.5.  codegen/ThemeCodeGen.kt
3.6.  codegen/NavigationCodeGen.kt (Nav 2.9 type-safe)
3.7.  codegen/ScreenGenerator.kt
3.8.  codegen/ExpressionParser.kt + blacklist
3.9.  codegen/ActionRegistry.kt
3.10. codegen/StateBindingGen.kt (rememberSaveable)
3.11. codegen/PreserveBlock.kt
3.12. codegen/GenCache.kt
3.13. Интеграция: заменить hardcoded Hello World на generated
```

### Фаза 4: Editor

```
4.1.  Удалить Widget.kt enum → WidgetCatalog object
4.2.  EditorViewModel с ScreenModel + UndoManager
4.3.  DesignCanvas (рекурсивный рендер WidgetNode)
4.4.  PropertyEditor bottom sheet
4.5.  WidgetPalette → addWidget
4.6.  Auto-save (debounce 500ms)
4.7.  Кнопка Build → BuildScreen
```

### Фаза 5: Качество

```
5.1.  common/Result.kt, AppError.kt, UiEvent.kt
5.2.  EncryptedSharedPreferences для API key
5.3.  res/xml/network_security_config.xml (localhost для Ollama)
5.4.  Unit-тесты: expression parser, codegen golden files
5.5.  Sample project в assets/
5.6.  README, LICENSE, CONTRIBUTING
```

### Фаза 6: AI v2

```
6.1-6.10 (см. 11-next-steps.md Шаг 6)
```

### Фаза 7: Polish + Release

```
7.1-7.7 (см. 11-next-steps.md Шаг 7)
```

---

## Часть 3: Первый файл для написания

После подготовки toolchain (Фаза 0) — первый файл кода:

**`app/src/main/java/my/company/ai/build/ToolchainConfig.kt`**

```kotlin
package my.company.ai.build

object ToolchainConfig {
    const val KOTLIN_VERSION = "2.1.0"
    const val COMPOSE_COMPILER_VERSION = "2.1.0"
    const val ECJ_VERSION = "3.39.0"
    const val BUILD_TOOLS_VERSION = "35.0.0"
    const val PLATFORM_API = 35
    const val R8_VERSION = "8.10.22"

    // CDN base URL (GitHub Releases)
    const val CDN_BASE = "https://github.com/user/ai-ide-toolchain/releases/download/v1/"

    val TOOLS = listOf(
        Tool("kotlinc.dex", "${CDN_BASE}kotlinc-${KOTLIN_VERSION}.dex", "SHA256_HERE", 62_000_000L),
        Tool("compose-plugin.dex", "${CDN_BASE}compose-plugin-${COMPOSE_COMPILER_VERSION}.dex", "SHA256_HERE", 5_000_000L),
        Tool("ecj.dex", "${CDN_BASE}ecj-${ECJ_VERSION}.dex", "SHA256_HERE", 2_100_000L),
        Tool("r8.dex", "${CDN_BASE}r8-${R8_VERSION}.dex", "SHA256_HERE", 5_000_000L),
        Tool("apksigner.dex", "${CDN_BASE}apksigner-${BUILD_TOOLS_VERSION}.dex", "SHA256_HERE", 2_000_000L),
        Tool("aapt2", "${CDN_BASE}aapt2-${BUILD_TOOLS_VERSION}-arm64", "SHA256_HERE", 5_000_000L),
        Tool("zipalign", "${CDN_BASE}zipalign-${BUILD_TOOLS_VERSION}-arm64", "SHA256_HERE", 1_000_000L),
        Tool("android.jar", "${CDN_BASE}android-${PLATFORM_API}.jar", "SHA256_HERE", 30_000_000L),
        Tool("kotlin-stdlib.jar", "${CDN_BASE}kotlin-stdlib-${KOTLIN_VERSION}.jar", "SHA256_HERE", 2_000_000L),
        Tool("compose-classpath.jar", "${CDN_BASE}compose-classpath-bom-2024.12.jar", "SHA256_HERE", 25_000_000L),
    )

    data class Tool(
        val filename: String,
        val url: String,
        val sha256: String,
        val expectedSize: Long,
    )
}
```
