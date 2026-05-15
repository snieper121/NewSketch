# 15 — UI/UX Design Specification

## Дата: 2026-05-15

## Дизайн-система

### Цветовая палитра (Material3)

**Seed color:** `#6750A4` (Purple) — стандартный Material3 purple.

**Light theme:**
| Role | Hex | Использование |
|------|-----|---------------|
| Primary | #6750A4 | Кнопки, FAB, активные иконки |
| OnPrimary | #FFFFFF | Текст на primary |
| PrimaryContainer | #EADDFF | Карточки акцентов, выделения |
| Secondary | #625B71 | Вторичные кнопки, chips |
| Background | #FFFBFE | Фон экранов |
| Surface | #FFFBFE | Карточки, sheets |
| SurfaceVariant | #E7E0EC | Панели, drawer |
| Error | #B3261E | Ошибки, destructive actions |

**Dark theme:**
| Role | Hex | Использование |
|------|-----|---------------|
| Primary | #D0BCFF | Кнопки, FAB, активные иконки |
| OnPrimary | #381E72 | Текст на primary |
| Background | #1C1B1F | Фон экранов |
| Surface | #1C1B1F | Карточки, sheets |
| Error | #F2B8B5 | Ошибки |

**Примечание:** На Android 12+ используется Dynamic Color (Material You). Палитра выше — fallback для старых устройств.

### Типографика

| Style | Size | Weight | Использование |
|-------|------|--------|---------------|
| headlineLarge | 28sp | Bold | Заголовки экранов |
| headlineMedium | 24sp | Bold | Подзаголовки |
| titleLarge | 20sp | Medium | Заголовки карточек |
| titleMedium | 16sp | Medium | Заголовки секций |
| bodyLarge | 16sp | Normal | Основной текст |
| bodyMedium | 14sp | Normal | Вторичный текст |
| labelLarge | 14sp | Medium | Кнопки, chips |
| labelMedium | 12sp | Medium | Caption, badges |

**Font:** Roboto (по умолчанию), моноширинный для кода (JetBrains Mono в M2+).

### Спейсинг (8dp grid)

| Значение | Применение |
|----------|-----------|
| 4dp | Минимальный отступ (иконки в ряду) |
| 8dp | Отступ между элементами в группе |
| 12dp | Отступ внутри карточки |
| 16dp | Стандартный padding контента |
| 24dp | Отступ между секциями |
| 32dp | Отступ сверху/снизу экрана |

### Формы (Shapes)

| Компонент | Corner radius |
|-----------|---------------|
| Карточки | 12dp |
| Кнопки | 20dp (pill) |
| Bottom Sheet | 28dp top corners |
| Dialog | 28dp |
| Chips | 8dp |
| FAB | 16dp |
| Иконки в палитре | 8dp |

---

## Экраны

### 1. ProjectsScreen (Главная — список проектов)

```
┌─────────────────────────────────────┐
│ ☰  Ai Sketch                   ⚙️  │  ← TopAppBar
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 📱 My App                   │   │  ← ProjectCard
│  │ com.example.myapp           │   │
│  │                    🔨  🗑️   │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 📱 Test Project             │   │
│  │ com.test.app                │   │
│  │                    🔨  🗑️   │   │
│  └─────────────────────────────┘   │
│                                     │
│                              [➕]   │  ← FAB: создать проект
└─────────────────────────────────────┘
```

**Элементы:**
- TopAppBar: название приложения + кнопка Settings
- LazyColumn с ProjectCard (имя, package, кнопки Build и Delete)
- FAB "+" для создания проекта
- EmptyState если нет проектов
- AlertDialog для создания (name + package name)

**Навигация:** → Settings, → Editor (по клику на карточку), → Build (по кнопке 🔨)

---

### 2. EditorScreen (Визуальный редактор)

```
┌─────────────────────────────────────┐
│ ☰  My App              💾  🤖  ✕  │  ← TopAppBar
├──────┬──────────────────────────────┤
│ 📁 🧩│                              │
│      │                              │
│ tree/│     Design Canvas            │
│ widg │     или                      │
│ ets  │     Code Editor              │
│      │     (OutlinedTextField)      │
│      │                              │
│      │                              │
│ ─────│──────────────────────────────│
│ / * "│ ' : { } ( ) [ ] = + - < >   │  ← Symbol Bar
└──────┴──────────────────────────────┘
```

**Элементы:**
- ModalNavigationDrawer (слева):
  - Toggle: Files / Widgets
  - Навигационные иконки: Chat, Screens, State, Resources, Build, Settings
  - Контент: FileTreePanel или WidgetPalette
- TopAppBar: имя проекта, Save, Close, AI Chat
- Body: DesignCanvas (визуальный) или CodeEditor (текстовый)
- BottomBar: SymbolBar для быстрого ввода символов

**Навигация:** → Chat, → ScreenManager, → StateLogic, → Resources, → Build, → ProjectSettings

---

### 3. BuildScreen (Сборка APK)

```
┌─────────────────────────────────────┐
│ ←  Build APK                        │  ← TopAppBar
├─────────────────────────────────────┤
│                                     │
│  Фаза: Kotlin Compile               │  ← Статус
│  ████████████░░░░░░  60%           │  ← Progress bar
│                                     │
│  ┌─────────────────────────────┐   │
│  │ [09:32:15] Сборка начата    │   │  ← Log panel
│  │ [09:32:16] CodeGen... OK    │   │
│  │ [09:32:18] Kotlin Compile.. │   │
│  │ [09:32:45] R.java compile.. │   │
│  └─────────────────────────────┘   │
│                                     │
│  [  Build APK  ]  [Install] [Stop] │  ← Кнопки
└─────────────────────────────────────┘
```

**Элементы:**
- TopAppBar с кнопкой Back
- Статус: имя фазы + LinearProgressIndicator
- Log panel: Card с LazyColumn монотонного текста (auto-scroll)
- Success: иконка ✅ + время сборки
- Error: иконка ❌ + сообщение об ошибке
- Кнопки: Build/Rebuild, Install, Cancel

---

### 4. ChatScreen (AI ассистент)

```
┌─────────────────────────────────────┐
│ ←  AI-assistant                     │  ← TopAppBar
├─────────────────────────────────────┤
│                                     │
│  ┌──────────────────────┐          │
│  │ Сделай экран логина  │          │  ← User message
│  └──────────────────────┘          │
│                                     │
│       ┌──────────────────────┐     │
│       │ Создаю экран логина  │     │  ← Assistant message
│       │ с полями email и     │     │
│       │ password...          │     │
│       └──────────────────────┘     │
│                                     │
│  ┌──────────────────────┐          │
│  │ Добавь кнопку "Войти"│          │  ← User message
│  └──────────────────────┘          │
│                                     │
├─────────────────────────────────────┤
│ [Введите сообщение...        ] [➤] │  ← Input row
└─────────────────────────────────────┘
```

**Элементы:**
- TopAppBar: "AI-assistant" + кнопка Back
- LazyColumn с MessageBubble (user — primaryContainer, assistant — surfaceVariant)
- Streaming: CircularProgressIndicator + "Thinking..."
- Input row: OutlinedTextField + кнопка Send
- Auto-scroll к последнему сообщению

---

### 5. SettingsScreen (Настройки)

```
┌─────────────────────────────────────┐
│ ←  Settings                         │  ← TopAppBar
├─────────────────────────────────────┤
│                                     │
│  AI Provider                        │
│  ┌─────────────────────────────┐   │
│  │ API Key: •••••••••••••••    │   │  ← PasswordVisualTransformation
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ Base URL: api.openai.com/v1 │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ Model: gpt-4o-mini          │   │
│  └─────────────────────────────┘   │
│                                     │
│  ℹ️ API key сохраняется локально    │  ← Help text
│  и не передаётся третьим лицам      │
└─────────────────────────────────────┘
```

**Элементы:**
- TopAppBar: "Settings" + кнопка Back
- 3 OutlinedTextField: API Key (password), Base URL, Model
- Help text внизу

---

### 6. ProjectSettingsScreen (Настройки проекта)

```
┌─────────────────────────────────────┐
│ ←  Project Settings                 │  ← TopAppBar
├─────────────────────────────────────┤
│                                     │
│  Package name                       │
│  ┌─────────────────────────────┐   │
│  │ com.example.myapp           │   │
│  └─────────────────────────────┘   │
│                                     │
│  Min SDK: [29]    Target SDK: [35]  │  ← Side by side
│                                     │
│  Permissions                        │
│  ┌──────────┐ ┌──────────┐        │
│  │ INTERNET │ │ CAMERA   │ [+]    │  ← Chips + Add
│  └──────────┘ └──────────┘        │
│                                     │
│  Dependencies                       │
│  ┌─────────────────────────────┐   │
│  │ coil-compose:2.6.0       [x]│   │
│  │ retrofit:2.11.0           [x]│   │
│  └─────────────────────────────┘   │
│  [+ Add dependency]                 │
│                                     │
│  Theme                              │
│  Primary: #6750A4 ● Dynamic: on    │  ← Read-only (M1)
└─────────────────────────────────────┘
```

---

### 7. ScreenManagerScreen (Управление экранами)

```
┌─────────────────────────────────────┐
│ ←  Screens                     [+] │  ← TopAppBar
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 🏠 MainScreen    launcher   │   │  ← Первый = launcher
│  │                    ✏️  🗑️   │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 📄 SettingsScreen           │   │
│  │                    ✏️  🗑️   │   │
│  └─────────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

---

### 8. ResourceManagerScreen (Ресурсы)

```
┌─────────────────────────────────────┐
│ ←  Resources                        │  ← TopAppBar
├─────────────────────────────────────┤
│  [Colors] [Strings] [Drawables] [Fonts] │  ← TabRow
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ ● #6750A4  primary       [x]│   │  ← Color card
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ ● #625B71  secondary      [x]│   │
│  └─────────────────────────────┘   │
│                                     │
│                              [➕]   │  ← FAB
└─────────────────────────────────────┘
```

---

### 9. StateLogicScreen (Переменные и события)

```
┌─────────────────────────────────────┐
│ ←  State & Logic                    │  ← TopAppBar
├─────────────────────────────────────┤
│  [Variables] [Events]               │  ← TabRow
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ counter     Int     = 0   [x]│   │  ← Variable card
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ userName    String  = ""  [x]│   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ isLoggedIn  Boolean = false[x]│   │
│  └─────────────────────────────┘   │
│                                     │
│                              [➕]   │  ← FAB
└─────────────────────────────────────┘
```

---

## Навигационные потоки

```
ProjectsScreen
    ├── [click project] → EditorScreen
    ├── [click Build icon] → BuildScreen
    └── [click Settings icon] → SettingsScreen

EditorScreen
    ├── [drawer: Chat] → ChatScreen
    ├── [drawer: Screens] → ScreenManagerScreen
    ├── [drawer: State/Logic] → StateLogicScreen
    ├── [drawer: Resources] → ResourceManagerScreen
    ├── [drawer: Build] → BuildScreen
    ├── [drawer: Settings] → ProjectSettingsScreen
    └── [hamburger → Files/Widgets toggle] → FileTreePanel / WidgetPalette

BuildScreen
    ├── [Install] → System installer
    └── [Back] → popBackStack()
```

---

## Оптимизация производительности

### Cold Start

1. **Baseline Profile** (M4) — компиляция критических путей при установке
2. **Lazy init** — `AppContainer` инициализирует только то что нужно для первого экрана
3. **Splash Screen API** (API 31+) — показываем splash пока грузится

### Память

1. **Toolchain в `noBackupFilesDir`** — не попадает в backup, не дублируется
2. **Lazy load toolchain** — загружать только нужные файлы перед сборкой
3. **Cleanup tmp** — удалять временные файлы после каждой сборки
4. **Bitmap pooling** — для preview изображений в редакторе

### UI Responsiveness

1. **Compose stability** — все data classes `@Immutable` или `@Stable`
2. **Lazy lists** — `key` параметр для стабильной идентификации элементов
3. **derivedStateOf** — для фильтрации/сортировки списков
4. **remember** — кеширование дорогих вычислений
5. **Dispatchers.Main** — все UI-операции на main thread, остальное на IO
6. **Debounced auto-save** — 500ms debounce перед записью JSON

### Build Process

1. **ForegroundService** — сборка не убивается системой
2. **Cancellation** — корутины с `ensureActive()` в каждой фазе
3. **OOM prevention** — `-Xmx1g` для kotlinc, chunked compilation
4. **Progress reporting** — `SharedFlow` для real-time обновления UI

### Списки

1. **LazyColumn** вместо Column + verticalScroll для > 10 элементов
2. **contentType** — разные типы элементов для эффективного переиспользования
3. **Paging** — для больших списков файлов (M2+)

---

## Accessibility

1. **Touch targets** — минимум 48x48dp для всех интерактивных элементов
2. **Content descriptions** — все иконки с `contentDescription`
3. **Color contrast** — WCAG AA (4.5:1 для текста)
4. **Screen reader** — `Modifier.semantics` для сложных composables
5. **Focus order** — логический порядок навигации с клавиатуры
