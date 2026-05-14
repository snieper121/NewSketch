# 10 — Анализ текущего состояния проекта

## Дата: 2026-05-14

## Что есть

Рабочий Android-проект (Gradle, собирается через GitHub Actions CI) с базовой структурой IDE.

### Конфигурация

- **Gradle:** 9.5.1, AGP 8.10.0, Kotlin 2.1.10 ✅
- **minSdk:** 29 ✅
- **targetSdk:** 36 ✅
- **Package:** `my.company.ai`
- **DI:** ручной AppContainer (не Koin) — план: Koin в M3+
- **aapt2:** из Maven репозитория (не локальный override) ✅
- **CI:** GitHub Actions (build/test/lint/assemble) ✅
- **APK artifact:** 16.9 MB debug APK ✅

### Структура кода

```
my.company.ai/
├── AiApp.kt                    — Application + AppContainer init
├── MainActivity.kt             — single Activity, Compose, edge-to-edge
├── di/AppContainer.kt          — ленивый DI (Room, Ktor, repos)
├── data/
│   ├── model/Project.kt        — id, name, packageName, rootPath, timestamps
│   ├── model/ProjectFile.kt    — дерево файлов (relativePath, isDirectory)
│   ├── model/Widget.kt         — enum WidgetType (13 типов)
│   ├── model/ChatMessage.kt    — сообщение чата
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
│   ├── repository/SettingsRepository.kt
│   └── template/
│       ├── KotlinProjectTemplate.kt  — генерация XML+AppCompat проекта
│       └── WidgetCodeGen.kt          — генерация кода виджетов
├── ui/
│   ├── navigation/AppNavHost.kt      — 4 маршрута (projects/editor/chat/settings)
│   ├── ViewModelFactory.kt
│   ├── screens/projects/             — список проектов + создание
│   ├── screens/editor/               — EditorScreen, DesignCanvas, WidgetPalette, FileTree
│   ├── screens/chat/                 — AI чат
│   ├── screens/settings/             — настройки (API key, model, base URL)
│   └── theme/                        — Material3 (Color, Type, Theme)
```

### Зависимости (libs.versions.toml)

✅ Compose BOM 2024.12.01, Navigation, Room, Ktor, DataStore, Timber, Serialization, Coroutines

## Проблемы (что не соответствует плану)

| # | Проблема | Серьёзность | Где исправлять |
|---|----------|-------------|---------------|
| 1 | Шаблон генерирует XML+AppCompat, не Compose | Критично | `data/template/KotlinProjectTemplate.kt` |
| 2 | `minSdk = 26`, нужно `29` | Критично | `app/build.gradle.kts` |
| 3 | Нет JSON-модели (project.json/screen.json) | Критично | `data/model/` — новые классы |
| 4 | Нет build pipeline (on-device компиляция) | Критично | Новый пакет `build/` |
| 5 | `Widget` — enum, а не дерево с properties | Высоко | `data/model/Widget.kt` → переписать |
| 6 | Manifest неполный (нет service, permissions) | Средне | `AndroidManifest.xml` |
| 7 | DI ручной, не Koin | Низко | Можно мигрировать позже |
| 8 | WidgetCodeGen генерирует XML | Высоко | `data/template/WidgetCodeGen.kt` |

## Что можно оставить

- ✅ Gradle config (AGP, Kotlin, Compose BOM, libs.versions.toml) — хороший
- ✅ Room + ProjectDao + ProjectEntity — годится для метаданных
- ✅ ProjectStorage (файловые операции) — годится
- ✅ Ktor + OpenAiCompatibleService — годится для AI
- ✅ SettingsRepository (DataStore) — годится
- ✅ Navigation (AppNavHost, Routes) — годится
- ✅ UI экраны (ProjectsScreen, EditorScreen, ChatScreen, SettingsScreen) — каркас годится
- ✅ Theme (Material3) — годится
- ✅ MainActivity (edge-to-edge, single Activity) — годится

## Порядок исправлений

### Фаза 0: Исправить фундамент

1. `app/build.gradle.kts` → `minSdk = 29`
2. `AndroidManifest.xml` → добавить permissions, service, queries
3. `data/model/` → новые data classes: `ScreenModel`, `WidgetNode`, `ProjectModel` (JSON-serializable)
4. `data/template/KotlinProjectTemplate.kt` → переписать под Compose-шаблон
5. `data/template/WidgetCodeGen.kt` → переписать под Compose codegen

### Фаза 1: M0 PoC (on-device build)

6. Новый пакет `build/` → `BuildService`, `ToolchainManager`, `InProcessCompiler`, `ApkBuilder`
7. Тестовая кнопка → собрать Hello Compose APK на устройстве

### Фаза 2: M1 (связать editor с JSON-моделью)

8. `EditorViewModel` → работает с `ScreenModel` (JSON)
9. `DesignCanvas` → рендерит дерево из JSON
10. Compose codegen из JSON → .kt файлы
11. Full flow: edit → codegen → build → install

### Фаза 3: M2+ (по roadmap)

12. Multi-screen, navigation codegen
13. Resources, theme customization
14. AI integration (structured patches)
