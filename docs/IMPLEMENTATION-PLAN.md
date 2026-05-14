# Ai IDE — Master Implementation Plan

> Создано: 2026-05-14
> База: docs/plan-v2/ (14 документов)
> Принцип: Без жёстких дедлайнов, приоритет правильности решений над скоростью

---

## Часть 1 — Аудит плана

### 1.1 Противоречия между файлами

| # | Файлы | Противоречие | Severity |
|---|-------|--------------|----------|
| C1 | 10-current-state-analysis.md vs 00-vision.md, app/build.gradle.kts | Файл говорит minSdk=26, но во всех остальных местах уже minSdk=29 | Medium |
| C2 | 02-build-toolchain.md, 13-toolchain-sources-and-order.md vs app/ | Kotlin toolchain версия 2.1.0, но проект уже на Kotlin 2.2.0 | **Critical** |
| C3 | 13-toolchain-sources-and-order.md vs libs.versions.toml | ToolchainConfig.kt указывает Kotlin 2.1.0 / Compose Plugin 2.1.0, но фактически 2.2.0 | **Critical** |
| C4 | 01-architecture.md vs 03-roadmap.md | DI: AppContainer (MVP), Koin в M3+. В реальности код уже AppContainer — нужно уточнить | Low |
| C5 | 02-build-toolchain.md vs 13-toolchain-sources-and-order.md | ecj версия: 3.39.0 vs 3.40.0 | Medium |

### 1.2 Пробелы — что упомянуто, но не раскрыто

| # | Где упомянуто | Что не раскрыто | Severity |
|---|---------------|-----------------|----------|
| G1 | 06-build-pipeline.md | Нет механизма resume download для 130MB toolchain | **High** |
| G2 | 02-build-toolchain.md | Нет стратегии lazy-load для compose-classpath.jar (25MB) | Medium |
| G3 | 06-build-pipeline.md | Не описана обработка partial build failure (фаза N упала, N+1 не начинать) | Medium |
| G4 | 01-architecture.md | Нет описания процесса миграции с AppContainer на Koin (когда и как) | Low |
| G5 | 08-testing-strategy.md | Нет CI-стратегии для нашего кейса (только GitHub Actions, нет локальной сборки) | Medium |
| G6 | 00-vision.md | Нет деталей onboarding flow для первого запуска | Low |
| G7 | 06-build-pipeline.md | Не описана cleanup-стратегия при OOM во время сборки | Medium |

### 1.3 Устаревшие решения

| # | Файл | Устарело | Чем заменить |
|---|------|----------|-------------|
| O1 | 13-toolchain-sources-and-order.md | Kotlin 2.1.0 → Kotlin 2.2.0 | Обновить все URL и версии |
| O2 | 13-toolchain-sources-and-order.md | Compose Compiler Plugin 2.1.0 → 2.2.0 | Синхронизировать с libs.versions.toml |
| O3 | 10-current-state-analysis.md | Gradle 8.11.1 / 9.4.1 | Уже 9.5.1 — обновить |
| O4 | 02-build-toolchain.md | R8 8.10.22 — нужно уточнить версию d8 из AGP 8.10.0 | Зафиксировать exact version |

### 1.4 Риски, не отражённые в 09-risks-and-compatibility.md

| # | Риск | Митигация |
|---|------|-----------|
| R20 | GitHub token не персистится между сессиями | Документировать в AGENTS.md процесс авторизации |
| R21 | Kotlin 2.2.0 + Compose Compiler Plugin совместимость не протестирована | Первый CI-прогон после каждого обновления Kotlin |
| R22 | GitHub Actions runner на x86_64, но мы на aarch64 — нет локальной валидации | Всегда дожидаться CI, никогда не мержить без прохождения |
| R23 | Play Store может отклонить из-за dynamic code loading (DexClassLoader) | Подготовить appeal letter, альтернатива: bundled toolchain |
| R24 | 130MB toolchain download на мобильном соединении без resume | Реализовать HTTP Range requests, chunked download |
| R25 | AGP 8.10.0 + Gradle 9.5.1 — нестандартная комбинация | Мониторить release notes, держать fallback конфигурацию |
| R26 | EncryptedSharedPreferences в security-crypto 1.1.0-alpha06 — alpha quality | Обёртка с fallback на обычный DataStore при crash |

### 1.5 Проверка non-goals (scope violations)

Проверка: нет ли в файлах фич вне scope?

| Фича | Где упомянута | В scope? | Решение |
|------|---------------|----------|---------|
| Git integration (JGit) | 03-roadmap.md M5+ | ✅ В non-goals "до v1.0", но M5 — post-v1.0 | OK |
| Release signing | 03-roadmap.md v0.2+ | ✅ В non-goals "debug-only до v0.2" | OK |
| Cloud build fallback | 09-risks-and-compatibility.md | ✅ Только как contingency | OK |
| Real-time collaboration | Нигде в плане не упомянута | ✅ В non-goals | OK |
| Kotlin Multiplatform | Нигде в плане | ✅ В non-goals | OK |
| Game engines | Нигде | ✅ В non-goals | OK |

**Вывод:** Scope violations не обнаружены. Non-goals соблюдаются.

---

## Часть 2 — Доработка плана по файлам

### 00-vision.md
- **Добавить:** Описание onboarding flow для первого запуска
- **Исправить:** Ничего
- **Убрать:** Ничего

### 01-architecture.md
- **Добавить:** Подраздел "Миграция AppContainer → Koin" (триггеры, пошаговый план)
- **Исправить:** API-Level Compatibility Matrix — добавить API 36 details
- **Убрать:** Ничего

### 02-build-toolchain.md
- **Добавить:** Стратегия resume/chunked download для toolchain
- **Добавить:** Lazy-load фазы для compose-classpath.jar
- **Исправить:** Версия Kotlin → 2.2.0, ecj → зафиксировать единую версию
- **Убрать:** Ничего

### 03-roadmap.md
- **Добавить:** Примечание "Без жёстких дедлайнов, приоритет правильности решений"
- **Исправить:** Убрать " solo dev с буфером × 1.5"
- **Убрать:** Конкретные недельные оценки (оставить только фазы)

### 04-data-model.md
- **Добавить:** Примеры валидации ошибок (что показывать пользователю)
- **Исправить:** Ничего
- **Убрать:** Ничего

### 05-code-generation.md
- **Добавить:** Примеры PRESERVE block для каждого типа файла
- **Исправить:** Ничего
- **Убрать:** Ничего

### 06-build-pipeline.md
- **Добавить:** Partial build failure handling (cleanup, retry strategy)
- **Добавить:** OOM recovery strategy
- **Добавить:** Resume download для toolchain
- **Исправить:** Ничего
- **Убрать:** Ничего

### 07-ai-integration.md
- **Добавить:** Пометка "Post-M0, реализуется в M3+"
- **Исправить:** Ничего
- **Убрать:** Ничего (отличная документация для будущего)

### 08-testing-strategy.md
- **Добавить:** CI-only strategy (нет локальной сборки)
- **Добавить:** Golden file update procedure через CI
- **Исправить:** Ничего
- **Убрать:** Ничего

### 09-risks-and-compatibility.md
- **Добавить:** R20-R26 из аудита
- **Исправить:** Ничего
- **Убрать:** Ничего

### 10-current-state-analysis.md
- **Добавить:** Текущий стек (Kotlin 2.2.0, Gradle 9.5.1)
- **Исправить:** minSdk=26 → 29, Gradle версии
- **Убрать:** Ничего

### 11-next-steps.md
- **Добавить:** Dependency matrix визуализация
- **Исправить:** Убрать временные оценки, оставить порядок
- **Убрать:** Ничего

### 12-additional-concerns.md
- **Добавить:** Disk space handling для toolchain (130MB + build cache)
- **Исправить:** Ничего
- **Убрать:** Ничего

### 13-toolchain-sources-and-order.md
- **Добавить:** Реальные SHA-256 (вычислить при первой загрузке)
- **Исправить:** Kotlin 2.1.0 → 2.2.0, Compose Plugin 2.1.0 → 2.2.0
- **Исправить:** ECJ версия → единая (3.40.0)
- **Добавить:** Resume download / chunked download spec
- **Убрать:** Ничего

---

## Часть 3 — Подробный порядок реализации

### Принципы
1. **Risk-first:** M0 PoC первым — валидирует всю архитектуру
2. **No hard deadlines:** Качество решений важнее скорости
3. **CI-driven:** Никакой локальной сборки, только GitHub Actions
4. **Incremental:** Каждый шаг — рабочий код, не черновики

### Dependency Matrix

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
    const val KOTLIN_VERSION = "2.1.10"  // CI-проверено, стабильная версия
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
