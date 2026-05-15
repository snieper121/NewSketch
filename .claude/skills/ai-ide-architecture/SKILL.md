---
name: ai-ide-architecture
description: "Ai IDE архитектура: слои, DI, навигация, модульная структура, roadmap M0-M5. Триггеры: Ai IDE architecture, AppContainer, DI, modules, roadmap, M0, M1, M2, M3, vision, non-goals."
---

# Ai IDE — Архитектура

## Что это

Ai IDE — мобильная среда разработки Android-приложений, работающая на Android-устройствах (API 29–36+).
Пользователь создаёт Compose-приложения визуально и с помощью AI, собирает APK на устройстве без ПК.

## Стек

| Слой | Технология |
|------|-----------|
| UI | Jetpack Compose + Material3 (BOM 2024.12.01) |
| Presentation | ViewModel + StateFlow (MVI) |
| DI | AppContainer (ручной, MVP) |
| Navigation | Navigation Compose 2.9.0 |
| Storage | Room + FileSystem |
| Network | Ktor Client 3.0.2 |
| Serialization | Kotlinx Serialization 1.7.3 |
| Codegen | KotlinPoet 1.18.1 |
| Logging | Timber 5.0.1 |

## Архитектурные слои

```
┌───────────────────────────────────────┐
│           UI Layer (Compose)          │
│  Screens, Components, Theme          │
├───────────────────────────────────────┤
│         Presentation Layer            │
│  ViewModels, UiState, UiEvents (MVI) │
├───────────────────────────────────────┤
│           Domain Layer                │
│  SchemaValidator, SchemaMigrator      │
├───────────────────────────────────────┤
│            Data Layer                 │
│  Repositories, Room, FileSystem, JSON │
├───────────────────────────────────────┤
│          Build Layer                  │
│  Toolchain, BuildPipeline, Codegen    │
├───────────────────────────────────────┤
│           AI Layer                    │
│  AiService, OpenAiCompatible         │
└───────────────────────────────────────┘
```

## DI: AppContainer

```kotlin
class AppContainer(context: Context) {
    val database = Room.databaseBuilder(...).build()
    val projectDao = database.projectDao()
    val projectStorage = ProjectStorage(context)
    val settingsRepository = SettingsRepository(context)
    val aiService = OpenAiCompatibleService(...)
    val projectRepository = ProjectRepository(projectDao, projectStorage)
    val aiRepository = AiRepository(aiService)
}
```

Инициализируется в `AiApp.onCreate()`, передаётся через `ViewModelFactory`.

## Навигация

4 основных маршрута в `AppNavHost.kt`:
- PROJECTS — список проектов
- EDITOR — визуальный редактор
- CHAT — AI чат
- SETTINGS — настройки

## Текущая структура кода

```
my.company.ai/
├── AiApp.kt                    — Application + AppContainer
├── MainActivity.kt             — single Activity, Compose
├── build/                      — build pipeline (9 фаз)
├── codegen/                    — KotlinPoet генераторы
├── data/
│   ├── json/                   — JsonModule
│   ├── local/                  — Room (AppDatabase, ProjectDao, ProjectStorage)
│   ├── model/                  — ProjectModel, ScreenModel, WidgetNode
│   ├── remote/                 — AI service (Ktor)
│   ├── repository/             — ProjectRepository, AiRepository, SettingsRepository
│   └── template/               — Legacy XML templates (не используется)
├── di/                         — AppContainer
├── domain/
│   ├── model/                  — ProjectJson (для codegen)
│   ├── SchemaMigrator.kt
│   └── SchemaValidator.kt
└── ui/
    ├── navigation/             — AppNavHost
    ├── screens/                — projects, editor, chat, settings, build
    ├── theme/                  — Material3
    └── ViewModelFactory.kt
```

## Roadmap

| Milestone | Цель | Статус |
|-----------|------|--------|
| M0 | PoC: Hello Compose APK собирается на устройстве | В работе |
| M1 | MVP Editor: визуальный редактор + JSON + codegen | Планируется |
| M2 | Multi-screen + Resources | Планируется |
| M3 | AI Integration | Планируется |
| M4 | Polish + Release | Планируется |
| M5+ | Git, plugins, marketplace | Post-MVP |

## Non-goals

- iOS / macOS / Windows / Web
- Kotlin Multiplatform
- Native C/C++ / Rust
- Game engines
- Backend generation
- Real-time collaboration
- Cloud project sync
- WYSIWYG pixel-perfect preview
