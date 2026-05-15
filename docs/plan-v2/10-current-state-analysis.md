# 10 — Анализ текущего состояния проекта

## Дата: 2026-05-15 (обновлено)

## Что есть

Рабочий Android-проект (Gradle, собирается через GitHub Actions CI) с развёрнутой структурой IDE.

### Конфигурация

- **Gradle:** 9.5.1, AGP 8.10.0, Kotlin 2.2.0 ✅
- **minSdk:** 29 ✅
- **targetSdk:** 36 ✅
- **Package:** `my.company.ai`
- **App Name:** "Ai Sketch"
- **DI:** ручной AppContainer (не Koin) — план: Koin в M3+
- **CI:** GitHub Actions (build/test/lint/assemble) ✅
- **APK artifact:** собирается ✅
- **Навигация:** 9 маршрутов в AppNavHost

### Структура кода (актуальная)

```
my.company.ai/
├── AiApp.kt                    — Application + AppContainer init
├── MainActivity.kt             — single Activity, Compose, edge-to-edge
├── di/AppContainer.kt          — ленивый DI (Room, Ktor, repos)
├── build/                      — Build pipeline (9 фаз)
│   ├── BuildPhase.kt           — interface для фаз
│   ├── BuildPipeline.kt        — оркестратор с progress/logs
│   ├── BuildService.kt         — ForegroundService
│   ├── KeystoreGenerator.kt    — генерация debug keystore
│   ├── ApkInstaller.kt         — установка через FileProvider
│   ├── ToolchainConfig.kt      — версии и URL toolchain
│   ├── model/BuildModels.kt    — BuildContext, PhaseResult, BuildProgress
│   ├── model/SampleProject.kt  — hardcoded Hello World
│   ├── toolchain/ToolchainDownloader.kt — загрузка с SHA-256
│   ├── toolchain/ToolchainManager.kt    — оркестратор загрузки
│   └── phases/                 — CodeGen, KotlinCompile, ResourceCompile, Dex, Package
├── codegen/                    — KotlinPoet генераторы
│   ├── CodeGenerator.kt        — interface
│   ├── ProjectGenerator.kt     — оркестратор
│   ├── ManifestGenerator.kt    — AndroidManifest.xml
│   ├── ThemeGenerator.kt       — Material3 theme
│   ├── MainActivityGenerator.kt
│   └── ScreenGenerator.kt      — рекурсивный генератор Compose виджетов
├── data/
│   ├── json/JsonModule.kt      — singleton Json
│   ├── model/ProjectModel.kt   — JSON-модель проекта (schema v2)
│   ├── model/ScreenModel.kt    — JSON-модель экрана (state, events, root widget)
│   ├── model/WidgetNode.kt     — рекурсивное дерево виджетов
│   ├── local/AppDatabase.kt    — Room DB
│   ├── local/ProjectDao.kt     — CRUD проектов
│   ├── local/ProjectEntity.kt  — Room entity
│   ├── local/ProjectStorage.kt — файловые операции
│   ├── remote/AiService.kt     — interface AI
│   ├── remote/OpenAiCompatibleService.kt — Ktor impl
│   ├── remote/HttpClientFactory.kt
│   ├── remote/dto/ChatCompletionDto.kt
│   ├── repository/ProjectRepository.kt  — создание/удаление/файлы
│   ├── repository/AiRepository.kt
│   └── repository/SettingsRepository.kt
├── di/AppContainer.kt          — ленивый DI
├── domain/
│   ├── model/ProjectJson.kt    — domain модель для codegen
│   ├── SchemaMigrator.kt       — миграция schema versions
│   └── SchemaValidator.kt      — валидация JSON
└── ui/
    ├── navigation/AppNavHost.kt — 9 маршрутов
    ├── ViewModelFactory.kt      — фабрика для 5 ViewModel'ей
    ├── theme/
    │   ├── Color.kt             — Purple/Pink палитра (дефолт)
    │   ├── Type.kt              — только bodyLarge
    │   └── Theme.kt             — AiAppTheme (dynamic color)
    └── screens/
        ├── projects/            — ProjectsScreen + ProjectsViewModel ✅
        ├── editor/              — EditorScreen + EditorViewModel ✅
        │   ├── DesignCanvas.kt  — preview виджетов
        │   ├── WidgetPalette.kt — список виджетов
        │   ├── FileTreePanel.kt — дерево файлов
        │   └── PropertyEditorSheet.kt — свойства (реализован, НЕ подключён)
        ├── chat/                — ChatScreen + ChatViewModel ✅
        ├── compiler/            — BuildScreen + BuildViewModel ✅
        ├── settings/            — SettingsScreen + SettingsViewModel ✅
        ├── projectsettings/     — ProjectSettingsScreen (без ViewModel) ⚠️
        ├── screens/             — ScreenManagerScreen (без ViewModel) ⚠️
        ├── resources/           — ResourceManagerScreen (без ViewModel) ⚠️
        └── statelogic/          — StateLogicScreen (без ViewModel) ⚠️
```

### Статус экранов

| # | Экран | ViewModel | Persist | Статус |
|---|-------|-----------|---------|--------|
| 1 | ProjectsScreen | ✅ | ✅ Room | Рабочий |
| 2 | EditorScreen | ✅ | ✅ FileSystem | Рабочий |
| 3 | ChatScreen | ✅ | ⚠️ ViewModel only | Рабочий (без сохранения) |
| 4 | BuildScreen | ✅ | N/A | Рабочий (downloadToolchain — stub) |
| 5 | SettingsScreen | ✅ | ✅ DataStore | Рабочий |
| 6 | ProjectSettingsScreen | ❌ | ❌ in-memory | Мокап |
| 7 | ScreenManagerScreen | ❌ | ❌ in-memory | Мокап (onEditScreen — TODO) |
| 8 | ResourceManagerScreen | ❌ | ❌ in-memory | Мокап |
| 9 | StateLogicScreen | ❌ | ❌ in-memory | Мокап |

### Навигация (9 маршрутов)

| Route | Pattern | Экран |
|-------|---------|-------|
| PROJECTS | `projects` (start) | ProjectsScreen |
| EDITOR | `editor/{projectId}` | EditorScreen |
| BUILD | `build/{projectId}` | BuildScreen |
| CHAT | `chat/{projectId}` | ChatScreen |
| SETTINGS | `settings` | SettingsScreen |
| PROJECT_SETTINGS | `project_settings/{projectId}` | ProjectSettingsScreen |
| SCREEN_MANAGER | `screen_manager/{projectId}` | ScreenManagerScreen |
| RESOURCES | `resources/{projectId}` | ResourceManagerScreen |
| STATE_LOGIC | `state_logic/{projectId}` | StateLogicScreen |

## Проблемы (что не соответствует плану)

| # | Проблема | Серьёзность | Где исправлять |
|---|----------|-------------|---------------|
| 1 | 4 из 9 экранов — мокапы без ViewModel | Высоко | Добавить ViewModel + persistence |
| 2 | PropertyEditorSheet реализован но не подключён | Высоко | Вызвать из EditorScreen |
| 3 | onEditScreen = TODO в ScreenManagerScreen | Высоко | Подключить навигацию |
| 4 | downloadToolchain() — stub в BuildViewModel | Критично | Реализовать загрузку |
| 5 | insertTextAtCursor — stub (append to end) | Средне | Реализовать cursor tracking |
| 6 | Widget add — UI-only, не в JSON | Высоко | Интеграция с JSON моделью |
| 7 | Theme — дефолтные Purple/Pink цвета | Средне | Определить бренд-палитру |
| 8 | Typography — только bodyLarge | Средне | Определить типографскую шкалу |
| 9 | Строки захардкожены на русском | Средне | Вынести в strings.xml |
| 10 | PropertyEditorSheet — вызывается но не из EditorScreen | Высоко | Wire up |

## Что можно оставить

- ✅ Gradle config — хороший
- ✅ Room + ProjectDao + ProjectEntity — годится
- ✅ ProjectStorage — годится
- ✅ Ktor + OpenAiCompatibleService — годится
- ✅ SettingsRepository — годится
- ✅ AppNavHost с 9 маршрутами — годится
- ✅ ViewModelFactory — годится
- ✅ Build pipeline (9 фаз) — реализован
- ✅ Codegen (KotlinPoet) — реализован
- ✅ JSON модели (ProjectModel, ScreenModel, WidgetNode) — реализованы
- ✅ Domain модели (ProjectJson) — реализованы
- ✅ 5 рабочих экранов с ViewModel

## Порядок исправлений

### Приоритет 1: Доработать мокапы

1. ProjectSettingsScreen → добавить ViewModel + persistence
2. ScreenManagerScreen → добавить ViewModel + подключить onEditScreen
3. ResourceManagerScreen → добавить ViewModel + persistence
4. StateLogicScreen → добавить ViewModel + persistence

### Приоритет 2: Подключить существующий код

5. PropertyEditorSheet → вызвать из EditorScreen
6. Widget add → интеграция с JSON моделью
7. downloadToolchain() → реализовать загрузку

### Приоритет 3: UI polish

8. Определить бренд-палитру (заменить Purple/Pink)
9. Определить типографскую шкалу
10. Вынести строки в strings.xml

### Приоритет 4: M0 completion

11. Реализовать toolchain download
12. Протестировать сборку на устройстве
13. SHA-256 checksums вычислить
