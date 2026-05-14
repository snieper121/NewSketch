# ADR — Ai IDE (v2)

Architecture Decision Records для проекта Ai IDE.

Полный план в `docs/plan-v2/`. Этот файл — краткая история ключевых решений с обоснованиями.

**Статусы:** ✅ Принято · 🔄 Обновлено · ❌ Отменено · ⏸ Отложено

---

## ADR-001: Язык целевых проектов — только Kotlin ✅

**Решение:** генерировать и редактировать только Kotlin-проекты на базе Jetpack Compose + Material 3. Java-шаблоны не поддерживаются.

**Следствие:** упрощается шаблонизация, одна модель кодогенерации.

**Источник:** `docs/plan-v2/00-vision.md`

---

## ADR-002: UI — Jetpack Compose + Material 3 ✅

**Решение:** весь UI приложения Compose, BOM управляет транзитивными версиями.

**minSdk изменён:** ~~26~~ → **29** (для поддержки scoped storage enforcement и W^X на API 34+).

**Источник:** `docs/plan-v2/01-architecture.md`

---

## ADR-003: Архитектура — MVVM + UDF + Result/UiEvent ✅

**Слои:** `ui` → `domain` (M2+) → `data`.

Экран = `@Composable` + `ViewModel`, состояние через `StateFlow<UiState>`, события через `Channel<UiEvent>`.

**Обновлено v2:** добавлен единый `Result<T>` sealed class + `AppError` для доменного слоя, `UiEvent` для one-shot событий (Snackbar/Dialog/Navigation).

**Источник:** `docs/plan-v2/01-architecture.md → Error Handling`

---

## ADR-004: DI — AppContainer в MVP → Koin в M3+ 🔄

**MVP (M0-M2):** ручной `AppContainer`, `ViewModelProvider.Factory`.

**M3+:** переход на Koin когда граф зависимостей усложнится (триггер: `:app` > 100 файлов ИЛИ команда > 2 человек).

**Почему Koin (не Hilt):** runtime DI без KSP, быстрее сборка, лучше с multi-module.

**Источник:** `docs/plan-v2/01-architecture.md → Dependency Injection`

---

## ADR-005: Хранилище проектов — file system + Room (индекс) ✅

**Решение:** исходники в `context.filesDir/projects/<uuid>/`. Метаданные в Room.

**Обновлено v2:** добавлено разделение:
- `design/` — JSON source of truth (новое)
- `generated/` — read-only, regenerated from JSON
- `custom/` — user Kotlin code (preserved)
- `build/` — compilation output

Toolchain в `context.noBackupFilesDir/toolchain/`.

**Источник:** `docs/plan-v2/01-architecture.md → Storage Model`

---

## ADR-006: Настройки — DataStore → EncryptedSharedPreferences для секретов 🔄

**MVP:** DataStore Preferences (без шифрования, TODO).

**v0.2+:** API-ключи мигрируют в `EncryptedSharedPreferences`. Non-secret preferences остаются в DataStore.

**Источник:** `docs/plan-v2/07-ai-integration.md → Security`

---

## ADR-007: Сеть — Ktor Client 3.x + kotlinx.serialization ✅

Без изменений от v1.

---

## ADR-008: AI-провайдер — абстракция + patch-based API 🔄

**MVP v1 (отменено):** plain chat, текстовые ответы.

**MVP v2 (актуально):** AI работает на уровне JSON-модели (structured patches), не на уровне raw Kotlin.

- Provider abstraction: OpenAI, Claude, OpenAI-compatible (Ollama/LM Studio)
- `SecretFilter` перед отправкой
- `TokenCounter` показывает cost preview
- `PromptCache` для повторных запросов
- `DiffEngine` применяет patches с rollback

**Запрещено:** action с raw Kotlin-кодом (`action.type = "custom"`).

**Источник:** `docs/plan-v2/07-ai-integration.md`, `docs/plan-v2/04-data-model.md`

---

## ADR-009: Генерация проектов — KotlinPoet (не string templates) 🔄

**v1 (отменено):** string templates «для простоты».

**v2:** **KotlinPoet** (`com.squareup:kotlinpoet:1.18.1`).

**Причина:** codegen будет расти (15 → 50+ виджетов). KotlinPoet даёт:
- Детерминированный output (golden tests)
- Автоматический imports management
- Защиту от форматных багов
- Масштабируется без регрессий

Цена: +2 MB в APK IDE. Приемлемо.

**Источник:** `docs/plan-v2/05-code-generation.md`

---

## ADR-010: Сборка пользовательских проектов — on-device через DexClassLoader 🔄

**v1 (отменено):** on-device build не реализуется, только ZIP export.

**v2:** **on-device build** — ключевая функция. Валидируется в M0 PoC.

**Инструменты:**
- `kotlinc` (in-process via DexClassLoader) — Kotlin → .class
- **`ecj`** (Eclipse JDT) — R.java → .class (НЕ kotlinc, он не компилирует .java)
- `aapt2` (native binary) — ресурсы
- `d8` с multidex + desugaring
- `zipalign` + `apksigner`

**Для Gradle export:** отдельная функция в v0.2+.

**Источник:** `docs/plan-v2/02-build-toolchain.md`, `docs/plan-v2/06-build-pipeline.md`

---

## ADR-011: Стек сборки — Gradle Kotlin DSL + Version Catalog ✅

Без изменений от v1.

**Обновлено:** Kotlin 2.1.0, AGP 8.10.0, JVM 17, compileSdk 36, minSdk 29 (IDE).

---

## ADR-012: JSON — source of truth (новое в v2) ✅

Визуальный редактор работает с JSON-моделью. Kotlin-код регенерируется из JSON.

**Схемы:** `ProjectModel`, `ScreenModel`, `WidgetNode`, `ModifierModel`, `StateVariable`, `EventModel`, `ActionModel`, `ThemeModel`, ресурсы.

**Версионирование:** каждый JSON имеет `schemaVersion`. `SchemaMigrator` переводит старые версии.

**Источник:** `docs/plan-v2/04-data-model.md`

---

## ADR-013: Expression Grammar для actions (новое в v2) ✅

Действия (`setState`, `navigate` и т.д.) используют ограниченный DSL — **не произвольный Kotlin**.

**Разрешено:** literals, state refs, arithmetic, comparison, boolean, ternary via if.
**Запрещено:** function calls, property access вне state, lambdas, side effects.

Expression парсится в AST перед codegen.

**Источник:** `docs/plan-v2/04-data-model.md → Expression grammar`

---

## ADR-014: Forward-Compatibility Policy (новое в v2) ✅

Цикл обновления под новые Android версии:
1. Феврал-март: Developer Preview → начать тестирование
2. Июнь-август: Beta → фикс breaking changes
3. Сентябрь-октябрь: Stable → обновить targetSdk
4. Ноябрь: публикация в Play Store

**Runtime feature detection** везде через `Build.VERSION.SDK_INT`.

**Источник:** `docs/plan-v2/09-risks-and-compatibility.md → Forward-Compatibility`

---

## ADR-015: Foreground Service для сборки (новое в v2) ✅

Сборка длится 5-15 мин. Без foreground service система убьёт процесс.

**Type:** `dataSync` (требование API 34+).

**Cancellation:** coroutine cancel + cleanup tmp файлов.

**Источник:** `docs/plan-v2/06-build-pipeline.md → Foreground Service`

---

## ADR-016: Single-Module → Multi-Module evolution (новое в v2) ✅

**M0-M2:** single module `:app` (быстрая итерация).

**M3+:** разбивка на `:core:*`, `:feature:*`, `:lib:*` (10+ модулей).

**Триггер:** `:app` > 100 файлов ИЛИ команда > 2 человек.

**Источник:** `docs/plan-v2/01-architecture.md → Модульная структура`

---

## Структура пакетов (v2)

```
my.company.ai/
├── AiApp.kt
├── MainActivity.kt
├── build/                     ← on-device сборка (M0 PoC)
│   ├── BuildService.kt
│   ├── ToolchainManager.kt
│   ├── InProcessCompiler.kt
│   ├── ApkBuilder.kt
│   ├── KeystoreGenerator.kt
│   └── BuildPipeline.kt
├── codegen/                   ← KotlinPoet-based (новое)
│   ├── ProjectGenerator.kt
│   ├── ScreenGenerator.kt
│   ├── NavigationCodeGen.kt
│   ├── ThemeCodeGen.kt
│   ├── ManifestGen.kt
│   └── ExpressionParser.kt
├── common/                    ← Result, UiEvent (новое)
│   ├── Result.kt
│   ├── AppError.kt
│   └── UiEvent.kt
├── di/
│   └── AppContainer.kt        ← → Koin в M3+
├── data/
│   ├── model/
│   │   ├── ProjectModel.kt    (JSON, новое)
│   │   ├── ScreenModel.kt     (JSON, новое)
│   │   ├── WidgetNode.kt      (JSON, новое)
│   │   ├── ResourceModels.kt  (JSON, новое)
│   │   ├── Project.kt         (domain, для Room)
│   │   └── ProjectFile.kt
│   ├── local/                 ← Room + FS
│   ├── remote/                ← Ktor + OpenAI
│   ├── repository/
│   └── template/              ← KotlinProjectTemplate
└── ui/
    ├── navigation/
    ├── screens/
    │   ├── projects/
    │   ├── editor/
    │   ├── chat/
    │   ├── build/             (новое)
    │   └── settings/
    └── theme/
```

---

## Открытые вопросы

- Code editor: plain TextField (MVP) → Sora Editor (M2+).
- Onboarding flow (первый запуск) — дизайн в `12-additional-concerns.md`.
- Plugin marketplace — v1.0+ (не в MVP).
- Local LLM (Ollama) — v1.0+.

---

## Legacy ADRs (архивные)

ADR-010 v1 («без on-device build») отменён в v2. Сохраняется в git history.
ADR-009 v1 («string templates») отменён в v2 в пользу KotlinPoet.
ADR-008 v1 (plain chat) заменён v2 (patch-based API).
