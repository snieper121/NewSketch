# 11 — Next Steps (пошаговый план реализации)

## Принцип: Risk-first

Самое неизвестное — делаем первым. Если M0 PoC не работает — меняем архитектуру до любой остальной работы.

## Правильный порядок

```
ФАЗА A (3-5 нед)  — M0 PoC на HARDCODED Hello World
ФАЗА B (8-12 нед) — Шаг 1 (JSON) → Шаг 2 (Codegen) → Интеграция с M0
ФАЗА C (6-10 нед) — Шаг 3 (Editor на JSON)
ФАЗА D (4-6 нед)  — Шаг 5 (качество, параллельно C)
ФАЗА E (7-10 нед) — Шаг 6 (AI v2)
ФАЗА F (5-7 нед)  — Шаг 7 (polish, release)
```

**Итого реалистично: 33-50 недель** (solo dev с buffer × 1.5-2).

---

## Шаг 4 (M0 PoC) — ПЕРВЫМ

**Цель:** собрать Hello Compose APK на устройстве из HARDCODED source (без JSON, без editor).

**Exit criteria:**
- [ ] APK запускается на API 29, 34, 36
- [ ] Показывает Compose UI без crash
- [ ] < 5 мин сборки на Snapdragon 6xx
- [ ] Нет OOM на 4 GB RAM
- [ ] Cancellation работает (cleanup tmp files)
- [ ] DexClassLoader + SHA-256 check работает

**Задачи:**
- `build/BuildProgress`, `BuildError`, `BuildPhase` interfaces
- `build/ToolchainDownloader` с SHA-256
- `build/ToolchainManager`
- `build/KeystoreGenerator` — случайный пароль через Android Keystore
- `build/KotlinCompiler` interface + `InProcessKotlinCompiler`
- `build/JavaCompiler` interface + `EcjJavaCompiler` для R.java
- `build/ApkBuilder` interface + реализация (aapt2+d8+zipalign+apksigner)
- `build/phases/*` — KotlinCompilePhase, RJavaCompilePhase, DexPhase, ...
- `build/BuildPipeline` — chain of phases
- `build/BuildService` — real FGS с cancellation
- `ui/screens/build/BuildScreen.kt` — лог + progress
- HARDCODED `SampleProject.kt` — Hello World как строка

**НЕ делаем:** JSON model, Editor, AI, templates, project CRUD.

---

## Шаг 1 — JSON-интеграция

- `data/JsonModule.kt` (singleton Json)
- `ProjectStorage.readJson/writeJson`
- Разбить `ProjectRepository` на: `ProjectCrudRepository`, `ProjectFilesRepository`, `ProjectModelRepository`
- Создание проекта пишет `design/*.json`
- `domain/SchemaValidator`, `SchemaMigrator`

---

## Шаг 2 — Codegen через KotlinPoet

- `com.squareup:kotlinpoet:1.18.1` в libs.versions
- `codegen/ProjectGenerator` — orchestrator
- `codegen/ManifestGen`, `MainActivityGen`
- `codegen/ThemeCodeGen` — Material3 full color scheme (30+ slots), Typography (15+ roles)
- `codegen/NavigationCodeGen` — **Navigation 2.9 type-safe через @Serializable**
- `codegen/ScreenGenerator` — KotlinPoet
- `codegen/ExpressionParser` — grammar + **blacklist System/Runtime/ClassLoader**
- `codegen/ActionRegistry` — handlers per action type
- `codegen/StateBindingGen` — `rememberSaveable` + MutableState + two-way binding
- `codegen/PreserveBlock`
- `codegen/GenCache`
- Integration: заменить HARDCODED из Шага 4 на реальный generated проект

---

## Шаг 3 — Editor на JSON

- Удалить `Widget.kt` enum → `WidgetCatalog` object
- `EditorViewModel` хранит `ScreenModel`, `selectedWidgetId`
- Операции: add/remove/update/move/undo/redo (snapshot stack)
- `DesignCanvas` рекурсивно рендерит `WidgetNode`
- `PropertyEditor` bottom sheet
- Auto-save debounced 500 ms
- Кнопка Build → BuildScreen

---

## Шаг 5 — Качество (параллельно Шагу 3)

- `common/Result`, `AppError`, `UiEvent`
- API key → `EncryptedSharedPreferences`
- Unit-тесты: JSON, expression parser (включая blacklist), codegen golden files
- LeakCanary + StrictMode в debug
- Root `README.md`, `LICENSE`, `CONTRIBUTING.md`
- **Sample project bundled в `assets/`** для первого запуска без build

---

## Шаг 6 — AI v2

- `ai/ContextBuilder`, `PatchModels`, `ResponseParser`
- `ai/DiffEngine` + `PatchApplier` per type
- `ai/PromptTemplates`, `TokenCounter` (jtokkit), `SecretFilter`, `PromptCache`
- `ui/screens/chat/DiffViewer`
- `ui/screens/settings/UsageScreen`

---

## Шаг 7 — Polish + Release

- Onboarding flow с sample проектом
- Baseline Profile
- Roborazzi snapshot tests
- CI (GitHub Actions)
- Crashlytics opt-in
- Accessibility audit
- **Release plan:**
  - Alpha (internal): M3 end
  - Closed Beta (5-10 testers): M4 mid
  - Open Beta (Play Store beta track): M4 end
  - v1.0 stable: +2-4 недели после Open Beta

---

## Dependency matrix

```
Шаг 4 (M0)  → блокирующий, ПЕРВЫМ
    ↓
Шаг 1 (JSON) → блокирует 2, 3
    ↓
Шаг 2 (codegen) → блокирует 3; заменяет hardcoded в 4
    ↓              ↓
Шаг 3 (editor)  Шаг 5 (параллельно)
    ↓
Шаг 6 (AI)   → нужен 1-3
    ↓
Шаг 7 (release)
```

## Артефакты

| После | Видит пользователь |
|-------|--------------------|
| 4 (M0) | Hello World APK собирается, устанавливается, работает |
| 1 | Проект пишет валидные JSON |
| 2 | `generated/` содержит корректный Kotlin; pipeline собирает его |
| 3 | Editor с drag-drop, undo/redo |
| 5 | Sample project, тесты, стабильность |
| 6 | AI создаёт экраны с diff viewer |
| 7 | Beta в Play Store |
