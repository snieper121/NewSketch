# 14 — Подробный порядок реализации

> Скопировано из IMPLEMENTATION-PLAN.md Часть 3

---

## Принципы
1. **Risk-first:** M0 PoC первым — валидирует всю архитектуру
2. **No hard deadlines:** Качество решений важнее скорости
3. **CI-driven:** Никакой локальной сборки, только GitHub Actions
4. **Incremental:** Каждый шаг — рабочий код, не черновики

## Dependency Matrix

```
Шаг 0 (CI Fix) → блокирует всё
    ↓
Шаг 1 (Toolchain Config) → блокирует 2-5
    ↓
Шаг 2 (Downloader + SHA) → блокирует 3-5
    ↓
Шаг 3 (SecureDexLoader) → блокирует 4-5
    ↓
Шаг 4 (InProcessKotlinCompiler) → блокирует 6
    ↓
Шаг 5 (EcjJavaCompiler) → блокирует 6
    ↓
Шаг 6 (Build Pipeline) → блокирует 7
    ↓
Шаг 7 (UI + Service) → MVP M0 complete
```

---

### Шаг 0: Починка CI (CRITICAL — блокер для всего)

**Проблема:** Последний форс-пуш нового проекта сломал CI.
**Действия:**
1. Запустить CI и получить логи ошибок
2. Исправить все compilation errors
3. Убедиться что все 4 job'а (build, assemble, test, lint) проходят

**Exit criteria:**
- [ ] CI статус = green на main ветке
- [ ] APK artifact успешно собирается
- [ ] Все job'ы проходят без warning'ов

**Зависимости:** Нет

**Риски:**
- Kotlin 2.2.0 + Compose plugin mismatch → откатить на 2.1.0 если критично
- AGP 8.10.0 + Gradle 9.5.1 несовместимость → обновить AGP или понизить Gradle

---

### Шаг 1: ToolchainConfig.kt (Фаза 1.1 из 13-toolchain-sources)

**Файлы:**
- `app/src/main/java/my/company/ai/build/ToolchainConfig.kt`
- `app/src/main/java/my/company/ai/build/ToolchainError.kt` (sealed class)

**Что создать:**
```kotlin
object ToolchainConfig {
    const val KOTLIN_VERSION = "2.2.0"
    const val COMPOSE_COMPILER_VERSION = "2.2.0"
    const val ECJ_VERSION = "3.40.0"    // Единая версия
    const val BUILD_TOOLS_VERSION = "35.0.0"
    const val PLATFORM_API = 35
    const val R8_VERSION = "8.10.22"
    
    // CDN base — placeholder, обновим после создания toolchain repo
    const val CDN_BASE = "https://github.com/snieper121/ai-ide-toolchain/releases/download/v1/"
    
    data class Tool(...)
    val TOOLS: List<Tool>
}
```

**Exit criteria:**
- [ ] Файл компилируется (CI проходит)
- [ ] Все версии синхронизированы с libs.versions.toml
- [ ] Список tools полный (9 штук)

**Зависимости:** Шаг 0 (CI работает)

**Риски:**
- Версии Kotlin в toolchain и IDE разойдутся → CI сломается

---

### Шаг 2: ToolchainDownloader.kt (Фаза 1.2)

**Файлы:**
- `app/src/main/java/my/company/ai/build/ToolchainDownloader.kt`
- `app/src/main/java/my/company/ai/build/ToolchainManager.kt` (оркестратор)

**Что создать:**
- HTTP downloader с OkHttp (уже в проекте)
- SHA-256 verification
- `setReadOnly()` после записи
- Resume download через HTTP Range headers
- Progress callback (bytes downloaded / total)

**Exit criteria:**
- [ ] Скачивание работает (тест через CI или mock)
- [ ] SHA-256 проверяется
- [ ] Файлы становятся read-only
- [ ] Resume download работает (прерывание → продолжение)

**Зависимости:** Шаг 1

**Риски:**
- 130MB без resume → пользователь никогда не скачает → реализовать Range
- CDN недоступен → fallback на Maven Central

---

### Шаг 3: SecureDexLoader.kt (Фаза 1.4)

**Файлы:**
- `app/src/main/java/my/company/ai/build/SecureDexLoader.kt`

**Что создать:**
- SHA-256 check перед КАЖДОЙ загрузкой через DexClassLoader
- Кэширование DexClassLoader (не пересоздавать каждый раз)
- Graceful error при mismatch (удалить файл, запросить повторную загрузку)

**Exit criteria:**
- [ ] SHA-256 проверяется перед load
- [ ] При mismatch — файл удаляется, ошибка пользователю
- [ ] Кэш DexClassLoader работает

**Зависимости:** Шаг 2

**Риски:**
- SHA-256 для 60MB файла ~300ms — приемлемо, но тестировать на устройстве

---

### Шаг 4: InProcessKotlinCompiler.kt (Фаза 1.6)

**Файлы:**
- `app/src/main/java/my/company/ai/build/KotlinCompiler.kt` (interface)
- `app/src/main/java/my/company/ai/build/InProcessKotlinCompiler.kt`

**Что создать:**
```kotlin
interface KotlinCompiler {
    fun compile(args: List<String>): Int  // exit code
}

class InProcessKotlinCompiler(loader: SecureDexLoader) : KotlinCompiler {
    // DexClassLoader.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
    // invoke exec() with args
}
```

**Exit criteria:**
- [ ] K2JVMCompiler загружается через DexClassLoader
- [ ] Простой вызов с `-version` возвращает 0
- [ ] CI проходит (unit test с mock)

**Зависимости:** Шаг 3

**Риски:**
- K2JVMCompiler может не загрузиться на API 34+ (W^X) → тестировать
- Kotlinc 2.2.0 может требовать Java 21 → наш JDK 17 в CI

---

### Шаг 5: EcjJavaCompiler.kt (Фаза 1.8)

**Файлы:**
- `app/src/main/java/my/company/ai/build/JavaCompiler.kt` (interface)
- `app/src/main/java/my/company/ai/build/EcjJavaCompiler.kt`

**Что создать:**
- DexClassLoader для ecj.dex
- Вызывать `org.eclipse.jdt.internal.compiler.batch.Main.compile()`

**Exit criteria:**
- [ ] ecj загружается
- [ ] Компилирует тестовый .java файл
- [ ] CI проходит

**Зависимости:** Шаг 3

**Риски:**
- ecj 3.40.0 compatibility с Android runtime → тестировать

---

### Шаг 6: Build Pipeline — 9 фаз (Фазы 1.11-1.25)

**Файлы:**
- `app/src/main/java/my/company/ai/build/BuildPhase.kt` (interface)
- `app/src/main/java/my/company/ai/build/BuildPipeline.kt`
- `app/src/main/java/my/company/ai/build/BuildProgress.kt` (sealed class)
- `app/src/main/java/my/company/ai/build/BuildError.kt` (sealed class)
- `app/src/main/java/my/company/ai/build/phases/*.kt`

**Фазы:**
1. CodegenPhase — hardcoded Hello World .kt
2. KotlinCompilePhase — kotlinc
3. ResourceCompilePhase — aapt2 compile
4. ResourceLinkPhase — aapt2 link → R.java
5. RJavaCompilePhase — ecj
6. DexPhase — d8 + multidex
7. MergePhase — zip dex + resources
8. AlignPhase — zipalign
9. SignPhase — apksigner

**Exit criteria:**
- [ ] Каждая фаза возвращает success/error
- [ ] Pipeline собирает APK из HARDCODED source
- [ ] APK устанавливается и запускается
- [ ] Cancellation работает (cleanup tmp)
- [ ] CI проходит (unit tests)

**Зависимости:** Шаги 4, 5

**Риски:**
- aapt2 native binary не запускается на API 35 → нужен 16-KB aligned build
- d8 multidex не работает → проверить main-dex-rules
- OOM → chunked compilation, `-Xmx1g`

---

### Шаг 7: UI + BuildService (Фазы 1.26-1.29)

**Файлы:**
- `app/src/main/java/my/company/ai/build/BuildService.kt` (ForegroundService)
- `app/src/main/java/my/company/ai/build/ApkInstaller.kt`
- `app/src/main/java/my/company/ai/ui/screens/build/BuildViewModel.kt`
- `app/src/main/java/my/company/ai/ui/screens/build/BuildScreen.kt`
- Обновить `AppNavHost.kt` — добавить route BUILD

**Exit criteria:**
- [ ] Кнопка "Build Hello World" в UI
- [ ] Сборка запускается через ForegroundService
- [ ] Notification показывает progress
- [ ] APK устанавливается через FileProvider
- [ ] Собранное приложение показывает "Hello Compose"

**Зависимости:** Шаг 6

**Риски:**
- ForegroundService restrictions на API 34+ → нужен правильный type
- FileProvider не настроен → добавить в manifest

---

### Шаг 8: Интеграция в проект + HARDCODED Sample

**Файлы:**
- `app/src/main/java/my/company/ai/build/SampleProject.kt` — hardcoded Hello World
- Обновить `HomeScreen.kt` — кнопка "Build Sample"

**Exit criteria:**
- [ ] Пользовательский flow: открыть app → Build Sample → APK → Install → Hello World
- [ ] Все 8 пунктов из M0 Exit Criteria (см. 03-roadmap.md)

**Зависимости:** Шаг 7

---

### Post-M0: Фаза 2+ (JSON → Codegen → Editor → AI)

После успешного M0 — следовать 11-next-steps.md шагам 1-7.
Dependency matrix:
```
M0 (Шаги 0-8)
    ↓
Шаг 1 (JSON-интеграция) → блокирует 2-3
    ↓
Шаг 2 (Codegen KotlinPoet) → блокирует 3
    ↓
Шаг 3 (Editor на JSON) + Шаг 5 (Quality, параллельно)
    ↓
Шаг 6 (AI v2)
    ↓
Шаг 7 (Polish + Release)
```

---

## Чеклист перед началом работы

- [ ] Шаг 0: CI починен (все job'ы green)
- [ ] AGENTS.md обновлён с актуальным стеком
- [ ] Все версии синхронизированы (Kotlin 2.2.0, Gradle 9.5.1, AGP 8.10.0)
- [ ] Git push с CI trigger настроен
