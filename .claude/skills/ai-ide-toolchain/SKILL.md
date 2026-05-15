---
name: ai-ide-toolchain
description: "Ai IDE toolchain: загрузка, pre-dex, CDN, версии, SHA-256, DexClassLoader. Триггеры: toolchain, download, pre-dex, CDN, kotlinc, ecj, d8, aapt2, zipalign, apksigner, DexClassLoader, SHA-256, keystore."
---

# Ai IDE — Toolchain Management

## Обзор

Toolchain — набор инструментов для компиляции Android APK на устройстве.
Скачивается при первом build, хранится в app-private директории.

## ToolchainConfig

| Tool | Source | Size | Pre-dex? |
|------|--------|------|----------|
| kotlinc | kotlin-compiler-embeddable JAR → .dex | ~62 MB | Да |
| compose-plugin | compose-compiler-plugin JAR → .dex | ~5 MB | Да |
| ecj | Eclipse JDT Compiler JAR → .dex | ~2.1 MB | Да |
| r8 | R8 JAR → .dex | ~5 MB | Да |
| apksigner | apksigner JAR → .dex | ~2 MB | Да |
| aapt2 | Native binary (из Google Maven) | ~5 MB | Extract |
| zipalign | Native binary (из build-tools) | ~1 MB | Extract |
| android.jar | Android SDK platform | ~30 MB | Нет (classpath) |
| kotlin-stdlib | Kotlin stdlib JAR | ~2 MB | Нет (classpath) |

**Итого:** ~115 MB скачиваемых файлов.

## CDN

GitHub Releases: `https://github.com/snieper121/ai-ide-toolchain/releases/download/v1/`

Pre-dex pipeline (на ПК/CI):
1. Скачать JAR с Maven Central
2. Конвертировать в DEX через d8: `d8 --release --output tool.dex tool.jar`
3. Вычислить SHA-256: `sha256sum tool.dex`
4. Загрузить на GitHub Releases

## ToolchainDownloader

```kotlin
class ToolchainDownloader(private val client: OkHttpClient) {
    suspend fun download(tool: Tool, targetDir: File, onProgress: (Long, Long) -> Unit)
    // HTTP Range headers для resume download
    // SHA-256 verification после загрузки
    // setReadOnly() на записанных файлах
}
```

## SecureDexLoader

```kotlin
class SecureDexLoader(private val toolchainDir: File) {
    fun loadClass(dexFilename: String, className: String): Class<*>
    // 1. Проверить SHA-256 перед загрузкой
    // 2. Кэшировать DexClassLoader
    // 3. При mismatch — удалить файл, бросить ошибку
}
```

## Использование в фазах

```kotlin
// KotlinCompilePhase
val compilerClass = dexLoader.loadClass("kotlinc.dex", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
val compiler = compilerClass.getDeclaredConstructor().newInstance()
val exitCode = compiler.exec(messages, args.toTypedArray())

// EcjJavaCompiler
val ecjClass = dexLoader.loadClass("ecj.dex", "org.eclipse.jdt.internal.compiler.batch.Main")
ecjClass.getMethod("compile", Array<String>::class.java).invoke(null, args.toTypedArray())

// aapt2, zipalign — native binaries через ProcessBuilder
val process = ProcessBuilder(toolchainDir.resolve("aapt2").absolutePath, *args).start()
```

## SHA-256 Verification

Все .dex файлы и native binaries проверяются по SHA-256 перед использованием.
SHA-256 хеши фиксируются в `ToolchainConfig.TOOLS[].sha256`.

Текущий статус: **все sha256 = "TODO"** — нужно вычислить при первом build.

## KeystoreGenerator

Генерирует debug keystore:
- Algorithm: RSA 2048
-Validity: 10000 дней
- Password: случайный (хранится в DataStore)
- Alias: "debug"

## Риски

- **Resume download**: 130MB без resume на мобильном → HTTP Range headers
- **W^X на API 34+**: DexClassLoader может не загрузить .dex из app-private
- **SHA-256 performance**: ~300ms для 60MB файла — приемлемо
- **CDN fallback**: если GitHub Releases недоступен → Maven Central fallback
- **aapt2 native**: может не работать на некоторых устройствах → ProcessBuilder + stderr
