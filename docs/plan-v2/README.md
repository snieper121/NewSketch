# Ai IDE — Project Plan v2

Мобильная IDE для создания Android Compose-приложений на устройстве (API 29–36+).

## Документы (16)

| # | Файл | Содержание |
|---|------|-----------|
| 00 | [00-vision.md](00-vision.md) | Видение, audience, value proposition, non-goals, scope MVP/v0.2/v1.0 |
| 01 | [01-architecture.md](01-architecture.md) | Стек, single-module→multi, AppContainer→Koin, Result/UiEvent, crash reporting |
| 02 | [02-build-toolchain.md](02-build-toolchain.md) | Kotlinc+ecj+aapt2+d8, DexClassLoader, multidex, desugaring |
| 03 | [03-roadmap.md](03-roadmap.md) | Milestone'ы M0–M5, таймлайн ~20 недель |
| 04 | [04-data-model.md](04-data-model.md) | JSON-схемы, state binding, expression grammar, миграции |
| 05 | [05-code-generation.md](05-code-generation.md) | KotlinPoet, PRESERVE markers, Gradle export |
| 06 | [06-build-pipeline.md](06-build-pipeline.md) | 9 фаз сборки, cancellation, multidex, error handling |
| 07 | [07-ai-integration.md](07-ai-integration.md) | Providers, SecretFilter, TokenCounter, Prompts, DiffEngine |
| 08 | [08-testing-strategy.md](08-testing-strategy.md) | Unit+golden+snapshot+device matrix+a11y |
| 09 | [09-risks-and-compatibility.md](09-risks-and-compatibility.md) | 19 рисков, supply chain, legal, contingency plans |
| 10 | [10-current-state-analysis.md](10-current-state-analysis.md) | Что уже есть в проекте, что не хватает |
| 11 | [11-next-steps.md](11-next-steps.md) | Пошаговый план реализации (7 шагов) |
| 12 | [12-additional-concerns.md](12-additional-concerns.md) | Дополнительные вопросы |
| 13 | [13-toolchain-sources-and-order.md](13-toolchain-sources-and-order.md) | Источники toolchain, порядок загрузки |
| 14 | [14-implementation-order.md](14-implementation-order.md) | Подробный порядок реализации |
| 15 | [15-ui-design.md](15-ui-design.md) | UI/UX дизайн-спецификация, wireframes, оптимизация |

## Экраны приложения (9 текущих + 5 дополнительных)

### Текущие экраны (реализованы)

| # | Экран | Назначение | Статус |
|---|-------|-----------|--------|
| 1 | ProjectsScreen | Список проектов, создание, удаление | ✅ Рабочий |
| 2 | EditorScreen | Визуальный редактор + код | ✅ Рабочий |
| 3 | ChatScreen | AI ассистент | ✅ Рабочий |
| 4 | BuildScreen | Сборка APK, лог, установка | ✅ Рабочий |
| 5 | SettingsScreen | API key, model, base URL | ✅ Рабочий |
| 6 | ProjectSettingsScreen | Package, SDK, permissions | ⚠️ Мокап |
| 7 | ScreenManagerScreen | Управление экранами | ⚠️ Мокап |
| 8 | ResourceManagerScreen | Цвета, строки, drawable | ⚠️ Мокап |
| 9 | StateLogicScreen | Переменные, события | ⚠️ Мокап |

### Дополнительные экраны (необходимы)

| # | Экран | Назначение | Приоритет |
|---|-------|-----------|-----------|
| 10 | WidgetPropertySheet | Свойства выбранного виджета (bottom sheet) | Высокий |
| 11 | EventEditorScreen | Редактор событий (onClick, onValueChange) | Высокий |
| 12 | ComponentManagerScreen | Управление компонентами (API, DB) | Средний |
| 13 | PreviewScreen | Предпросмотр приложения | Средний |
| 14 | ExportScreen | Экспорт в Gradle проект | Низкий |

### Навигационный граф

```
ProjectsScreen
    ├── [click project] → EditorScreen
    ├── [click Build] → BuildScreen
    └── [click Settings] → SettingsScreen

EditorScreen (главный экран редактора)
    ├── [drawer: Chat] → ChatScreen
    ├── [drawer: Screens] → ScreenManagerScreen
    ├── [drawer: State/Logic] → StateLogicScreen
    ├── [drawer: Resources] → ResourceManagerScreen
    ├── [drawer: Build] → BuildScreen
    ├── [drawer: Settings] → ProjectSettingsScreen
    ├── [select widget] → WidgetPropertySheet (bottom sheet)
    └── [click event] → EventEditorScreen

BuildScreen
    ├── [Install] → System installer
    └── [Back] → popBackStack()
```

## Порядок чтения

**Для понимания проекта:**
1. `00-vision` — что строим и зачем
2. `03-roadmap` — в каком порядке
3. `11-next-steps` — конкретные шаги

**Для реализации:**
4. `10-current-state-analysis` — что уже есть
5. `15-ui-design` — как должны выглядеть экраны
6. `01-architecture` — как устроено
7. `04-data-model` — формат данных
8. `05-code-generation` — JSON → Kotlin
9. `02-build-toolchain` — инструменты
10. `06-build-pipeline` — как собираем APK
11. `07-ai-integration` — AI-ассистент
12. `08-testing` — как проверяем
13. `09-risks` — что может пойти не так

## Первый шаг

**M0 PoC** (см. 03-roadmap, Шаг 4 из 11-next-steps): собрать Hello Compose APK на устройстве через in-process kotlinc + ecj. Без UI. Это валидирует всю архитектуру.

## Ключевые решения v2 (обновления от v1)

- **KotlinPoet** вместо string templates для codegen
- **ecj** для компиляции R.java (не kotlinc)
- **Multidex + desugaring** в pipeline
- **SecretFilter** для AI context
- **Single-module** до M3, потом разбивка
- **AppContainer** в MVP, Koin в M3+
- **Custom action запрещён** — только whitelist action types с expression grammar
- **PRESERVE маркеры** для custom code в generated files
- **Non-goals** зафиксированы в 00-vision

## Платформа

- IDE: Android 10–16 (API 29–36), forward-compatible
- Generated apps: Android 5+ (API 21+, выбирает пользователь)
- Язык: Kotlin 100%, Jetpack Compose
- Сборка: on-device, без Gradle, без root
