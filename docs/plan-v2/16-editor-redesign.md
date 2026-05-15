# 16 — Redesign EditorScreen (Sketchware Pro аналог без XML)

## Дата: 2026-05-15

## Проблема

Текущий EditorScreen не похож на Sketchware Pro. Основные отличия:

| Аспект | Sketchware Pro | Текущий Ai IDE | Нужно |
|--------|---------------|----------------|-------|
| Палитра виджетов | Внизу экрана, видна | В drawer (скрыта) | Внизу |
| Canvas | Drag-and-drop область | Список карточек | D&D область |
| Свойства виджета | Bottom sheet при выборе | Не подключён | Подключить |
| Навигация | Bottom tabs (View/Logic/Component) | В drawer | Bottom tabs |
| Дерево виджетов | Рекурсивное | Плоский список | Рекурсивное |
| Event editor | Встроенный | Отдельный экран | Встроенный |
| Код | Не виден | OutlinedTextField | Скрыть (M0) |

## Новая архитектура EditorScreen

### Структура экрана

```
┌─────────────────────────────────────────┐
│ ☰  My App              💾  🤖  👁️  ✕  │  ← TopAppBar
├─────────────────────────────────────────┤
│                                         │
│                                         │
│           Canvas Area                   │
│      (drag-and-drop виджетов)           │
│                                         │
│    ┌─────────────────────────────┐     │
│    │  Column (selected)          │     │  ← Выделенный виджет
│    │  ┌─────────────────────┐   │     │
│    │  │ Text: "Hello"       │   │     │
│    │  └─────────────────────┘   │     │
│    │  ┌─────────────────────┐   │     │
│    │  │ Button: "Click me"  │   │     │
│    │  └─────────────────────┘   │     │
│    └─────────────────────────────┘     │
│                                         │
├─────────────────────────────────────────┤
│ [Widget Palette - scrollable bottom]    │  ← Палитра виджетов
│ 📝 Text  │ 🔘 Button │ 📋 TextField   │
│ 🖼️ Image │ ☑️ Check  │ 🔄 Switch     │
│ 📦 Column │ 📦 Row   │ 📦 Box        │
│ 📄 Card  │ 📏 Div   │ ⬜ Spacer     │
├─────────────────────────────────────────┤
│ [View]  [Logic]  [Component]            │  ← Bottom tabs
└─────────────────────────────────────────┘
```

### Bottom Tabs

| Tab | Содержание |
|-----|-----------|
| **View** | Canvas + Widget Palette (основной режим) |
| **Logic** | Event Editor (список событий + actions) |
| **Component** | Component Manager (API, DB, переменные) |

### View Tab — основной режим

**Canvas Area:**
- Рекурсивно рендерит дерево виджетов из ScreenModel
- Tap на виджет → выделение + открытие PropertyEditorSheet
- Long press → контекстное меню (удалить, дублировать, переместить)
- Drag handle → перемещение виджета в дереве

**Widget Palette (внизу):**
- Горизонтальный скролл с категориями
- Категории: Layout, Input, Display, Container
- Tap → добавляет виджет в конец выбранного контейнера
- Drag → добавляет в конкретное место

**PropertyEditorSheet (bottom sheet):**
- Появляется при выборе виджета
- Показывает свойства в зависимости от типа
- Кнопка "Готово" закрывает лист

### Logic Tab — редактор событий

```
┌─────────────────────────────────────────┐
│ ←  Events & Logic                       │
├─────────────────────────────────────────┤
│                                         │
│  Screen: MainScreen                     │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ 📌 State Variables              │   │
│  │ counter: Int = 0                │   │
│  │ userName: String = ""           │   │
│  │ [+ Add Variable]                │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ ⚡ Events                       │   │
│  │                                 │   │
│  │ onClick_btn_1                   │   │
│  │   → setState: counter + 1       │   │
│  │   → navigate: SettingsScreen    │   │
│  │                                 │   │
│  │ onValueChange_textField_1       │   │
│  │   → setState: userName = it     │   │
│  │                                 │   │
│  │ [+ Add Event]                   │   │
│  └─────────────────────────────────┘   │
│                                         │
└─────────────────────────────────────────┘
```

### Component Tab — управление компонентами

```
┌─────────────────────────────────────────┐
│ ←  Components                           │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ 🌐 API Endpoints               │   │
│  │ GET /users → UserList           │   │
│  │ POST /login → AuthResponse      │   │
│  │ [+ Add Endpoint]                │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ 🗄️ Database                     │   │
│  │ users: Room Entity              │   │
│  │ [+ Add Table]                   │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ 🔧 Shared Preferences          │   │
│  │ authToken: String               │   │
│  │ [+ Add Preference]              │   │
│  └─────────────────────────────────┘   │
│                                         │
└─────────────────────────────────────────┘
```

## Что нужно реализовать

### Приоритет 1: View Tab (основа)

1. **Перенести WidgetPalette из drawer в bottom** — горизонтальный скролл
2. **Подключить PropertyEditorSheet** — вызов при tap на виджет
3. **Рекурсивный рендер дерева** — DesignCanvas рендерит WidgetNode дерево
4. **Tap-to-select** — выделение виджета на canvas
5. **Удалить drawer** — заменить на bottom tabs

### Приоритет 2: Logic Tab

6. **State Variables section** — список переменных с add/edit/delete
7. **Events section** — список событий с actions
8. **Action editor** — редактирование action для события

### Приоритет 3: Component Tab

9. **API Endpoints** — CRUD для HTTP endpoints
10. **Database tables** — CRUD для Room entities
11. **Shared Preferences** — CRUD для preferences

### Приоритет 4: Drag-and-Drop

12. **Drag handle** — иконка для перетаскивания виджета
13. **Drop zones** — подсветка мест куда можно бросить
14. **Reorder** — изменение порядка виджетов в контейнере

## Файлы для изменения

| Файл | Что менять |
|------|-----------|
| `EditorScreen.kt` | Убрать drawer, добавить bottom tabs, перенести palette вниз |
| `DesignCanvas.kt` | Рекурсивный рендер WidgetNode, tap-to-select |
| `WidgetPalette.kt` | Горизонтальный скролл, категории |
| `PropertyEditorSheet.kt` | Подключить к EditorScreen |
| `EditorViewModel.kt` | Добавить selectedWidgetId, логику выбора |
| Новый: `EventEditorPanel.kt` | Панель редактирования событий |
| Новый: `ComponentManagerPanel.kt` | Панель управления компонентами |

## Навигация (обновлённая)

```
ProjectsScreen
    └── [click project] → EditorScreen (View tab по умолчанию)

EditorScreen
    ├── [View tab] → Canvas + Widget Palette
    ├── [Logic tab] → Event Editor
    ├── [Component tab] → Component Manager
    ├── [tap widget] → PropertyEditorSheet (bottom sheet)
    ├── [🤖 button] → ChatScreen
    ├── [👁️ button] → PreviewScreen
    └── [✕ button] → ProjectsScreen
```

## Дополнительные экраны (необходимы)

| # | Экран | Назначение | Приоритет |
|---|-------|-----------|-----------|
| 10 | PropertyEditorSheet | Свойства виджета (bottom sheet) | Высокий |
| 11 | EventEditorPanel | Редактор событий (в Logic tab) | Высокий |
| 12 | ComponentManagerPanel | Управление компонентами (в Component tab) | Средний |
| 13 | PreviewScreen | Предпросмотр приложения | Средний |
| 14 | ExportScreen | Экспорт в Gradle проект | Низкий |

## Сравнение с Sketchware Pro

| Sketchware Pro | Ai IDE (новый) |
|---------------|----------------|
| View tab | View tab (canvas + palette) |
| Logic tab | Logic tab (events + actions) |
| Component tab | Component tab (API, DB, prefs) |
| Drag-and-drop | Drag-and-drop (M1) |
| Block editor | Expression editor (text-based) |
| XML layout | JSON model (Compose) |
| Java codegen | Kotlin codegen (KotlinPoet) |
