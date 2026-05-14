# 04 — Data Model

## Принцип

JSON — единственный source of truth. Визуальный редактор читает/пишет JSON. Kotlin-код генерируется из JSON и никогда не редактируется напрямую (кроме `custom/`).

## Структура файлов проекта

```
projects/<uuid>/
├── design/
│   ├── project.json
│   ├── screens/
│   │   ├── main.json
│   │   └── settings.json
│   ├── resources/
│   │   ├── colors.json
│   │   ├── strings.json
│   │   ├── dimens.json
│   │   └── drawables.json
│   └── theme.json
├── generated/        ← read-only, regenerated
├── custom/           ← user Kotlin code, preserved
└── build/            ← compilation output
```

## project.json

```json
{
  "schemaVersion": 2,
  "id": "uuid-here",
  "name": "My App",
  "packageName": "com.example.myapp",
  "minSdk": 26,
  "targetSdk": 35,
  "screens": ["main", "settings"],
  "launcherScreen": "main",
  "permissions": ["INTERNET"],
  "dependencies": [],
  "createdAt": "2026-05-11T20:00:00Z",
  "updatedAt": "2026-05-11T20:30:00Z"
}
```

## screen.json

```json
{
  "schemaVersion": 2,
  "id": "main",
  "name": "MainScreen",
  "state": [
    { "name": "counter", "type": "Int", "initial": "0" },
    { "name": "userName", "type": "String", "initial": "\"\"" }
  ],
  "events": [
    { "name": "onIncrement", "actions": [
      { "type": "setState", "target": "counter", "expression": "counter + 1" }
    ]},
    { "name": "onNavigate", "actions": [
      { "type": "navigate", "destination": "settings" }
    ]}
  ],
  "root": {
    "type": "Scaffold",
    "id": "scaffold_1",
    "properties": {
      "topBar": {
        "type": "TopAppBar",
        "id": "topbar_1",
        "properties": { "title": "My App" }
      }
    },
    "children": [
      {
        "type": "Column",
        "id": "col_1",
        "modifier": { "fillMaxSize": true, "padding": 16 },
        "children": [
          {
            "type": "Text",
            "id": "text_1",
            "properties": {
              "text": "${counter}",
              "style": "headlineMedium"
            }
          },
          {
            "type": "Button",
            "id": "btn_1",
            "properties": {
              "text": "Increment",
              "onClick": "onIncrement"
            }
          }
        ]
      }
    ]
  }
}
```

## Widget Node Schema

Каждый виджет — узел дерева:

```json
{
  "type": "string (required)",
  "id": "string (auto-generated, unique per screen)",
  "properties": { "...зависит от type" },
  "modifier": { "...см. Modifier Schema" },
  "children": [ "...widget nodes (optional)" ]
}
```

### Типы виджетов (MVP)

| Type | Properties | Children |
|------|-----------|----------|
| Text | text, style, color, maxLines, textAlign | — |
| Button | text, onClick, enabled | — |
| TextField | value, onValueChange, label, placeholder, singleLine | — |
| Column | verticalArrangement, horizontalAlignment | да |
| Row | horizontalArrangement, verticalAlignment | да |
| Card | elevation, shape | да |
| Scaffold | topBar, bottomBar, floatingActionButton | да (content) |
| TopAppBar | title, navigationIcon, actions | — |
| Icon | icon, tint, size, contentDescription | — |
| Image | src, contentDescription, contentScale | — |
| Spacer | — | — |
| Divider | color, thickness | — |
| Switch | checked, onCheckedChange, enabled | — |
| LazyColumn | verticalArrangement | да (items) |
| LazyRow | horizontalArrangement | да (items) |

## Modifier Schema (расширенный)

```json
{
  "fillMaxSize": true,
  "fillMaxWidth": true,
  "fillMaxHeight": false,
  "padding": 16,
  "paddingHorizontal": 8,
  "paddingVertical": 12,
  "paddingStart": 0,
  "paddingEnd": 0,
  "paddingTop": 0,
  "paddingBottom": 0,
  "width": 200,
  "height": 100,
  "size": 48,
  "minWidth": 100,
  "minHeight": 50,
  "weight": 1.0,
  "offset": { "x": 10, "y": 20 },
  "clickable": "eventName",
  "background": "#FF6750A4",
  "border": { "width": 1, "color": "#000000" },
  "clip": "rounded_16",
  "shadow": { "elevation": 4, "shape": "rounded_8" },
  "alpha": 0.8,
  "rotate": 45,
  "scale": 1.2,
  "verticalScroll": true,
  "horizontalScroll": false,
  "aspectRatio": 1.5
}
```

## State Variable

```json
{ "name": "counter", "type": "Int", "initial": "0" }
```

Поддерживаемые типы: `Int`, `String`, `Boolean`, `Float`, `List<String>`.

### Binding (reactivity)

Генератор преобразует state variable в:
```kotlin
var counter by remember { mutableIntStateOf(0) }
```

**Read binding** — в properties используется `${stateName}`:
```json
{ "text": "Count: ${counter}" }
```
→
```kotlin
Text(text = "Count: $counter")
```

Compose автоматически rerender'ит при изменении `counter` благодаря delegate `by`.

**Two-way binding** — для TextField:
```json
{
  "type": "TextField",
  "properties": {
    "value": "${userName}",
    "onValueChange": { "setState": "userName" }
  }
}
```
→
```kotlin
OutlinedTextField(
    value = userName,
    onValueChange = { userName = it }
)
```

Это стандартный Compose pattern — никаких доп. библиотек не нужно.

## Event / Action

```json
{
  "name": "onSubmit",
  "actions": [
    { "type": "setState", "target": "counter", "expression": "counter + 1" },
    { "type": "navigate", "destination": "settings" },
    { "type": "showSnackbar", "message": "Done!" }
  ]
}
```

### Action Types (MVP — безопасный набор)

| Type | Поля | Семантика |
|------|------|-----------|
| `setState` | target, expression | `target = expression` |
| `navigate` | destination | `navController.navigate(destination)` |
| `navigateBack` | — | `navController.popBackStack()` |
| `showSnackbar` | message | `snackbarHostState.showSnackbar(message)` |
| `showDialog` | dialogId | Открыть диалог по id |
| `toggleState` | target | `target = !target` (для Boolean) |
| `incrementState` | target, by | `target = target + by` (shortcut) |

### Expression grammar (ограниченный DSL)

`expression` — **не произвольный Kotlin**. Это ограниченная grammar:
- Literals: `0`, `"text"`, `true`, `false`, `1.5`
- State references: `counter`, `userName`
- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Comparison: `==`, `!=`, `<`, `>`, `<=`, `>=`
- Boolean: `&&`, `||`, `!`
- String concat: `"prefix " + name`
- Ternary (via if): `if (x > 0) "+" else "-"`

Валидация: перед codegen expression парсится и проверяется grammar. Ошибка → не генерировать, показать пользователю.

**Что НЕЛЬЗЯ:**
- Произвольные function calls (`println(...)`, `withContext(...)`)
- Property access вне state (`System.currentTimeMillis()`)
- Lambda / higher-order functions
- Any side effects вне setState

### Blacklist опасных identifiers (обязательно)

Помимо whitelist grammar, parser **явно отклоняет** identifiers которые потенциально опасны, даже если бы grammar их допустила:

```kotlin
private val FORBIDDEN_IDENTIFIERS = setOf(
    // Reflection / классы
    "System", "Runtime", "Class", "ClassLoader", "Thread",
    "ProcessBuilder", "Process",
    // IO
    "File", "FileInputStream", "FileOutputStream", "URL", "URI",
    "Socket", "ServerSocket", "HttpURLConnection",
    // Reflection (Kotlin)
    "::class", "javaClass", "reflect",
    // Coroutines
    "runBlocking", "GlobalScope", "withContext", "launch", "async",
    // Ktor / networking
    "HttpClient",
    // Dangerous JVM
    "Unsafe", "MethodHandle", "VarHandle",
    // Android
    "Context", "PackageManager", "Intent", "Activity",
    // IO streams
    "exec", "eval",
)

fun validateIdentifier(name: String) {
    if (name in FORBIDDEN_IDENTIFIERS) {
        throw ExpressionValidationError("Запрещённый identifier: $name")
    }
    // Дополнительно: regex на `.` (property access через dot) → запрет
    if ("." in name) throw ExpressionValidationError("Property access не поддерживается: $name")
}
```

Этот blacklist **applies AFTER grammar parsing**. Grammar уже отсекает большинство — blacklist добавляет безопасность «в глубину».

**Почему важно:** даже если AI сгенерирует `counter + System.currentTimeMillis()` и обойдёт grammar — blacklist блокирует выполнение. Defence in depth.

### Custom Kotlin action — ЗАПРЕЩЁН в MVP

В `04-data-model.md` v1 был тип `custom` с raw Kotlin. **В MVP v2 он удалён** по следующим причинам:

1. **Security** — AI может сгенерировать небезопасный код (API calls, file access)
2. **Validation** — невозможно валидировать произвольный код
3. **Codegen** — malformed code ломает сборку
4. **Testing** — невозможно тестировать

**Альтернатива в v1.0+:** whitelist safe API calls через extension mechanism. Пока — только перечисленные action types.

## Resources

### colors.json
```json
{
  "schemaVersion": 1,
  "colors": {
    "primary": "#6750A4",
    "secondary": "#958DA5",
    "error": "#B3261E",
    "background": "#FFFBFE"
  }
}
```

### strings.json
```json
{
  "schemaVersion": 1,
  "strings": {
    "app_name": "My App",
    "greeting": "Hello, %s!"
  }
}
```

### theme.json
```json
{
  "schemaVersion": 1,
  "colorScheme": "dynamic",
  "seedColor": "#6750A4",
  "fontFamily": "Roboto",
  "shapeCornerRadius": 16,
  "useDarkTheme": "system"
}
```

## Schema Versions

| schemaVersion | Введено в | Что изменилось |
|---|---|---|
| 1 | v0.1-alpha | Базовая структура |
| 2 | v0.1-MVP | Modifier вынесен в отдельное поле, action grammar ограничена |

## Версионирование схем

Каждый JSON имеет `schemaVersion`. При обновлении IDE:

```kotlin
fun migrateProject(json: JsonObject): JsonObject {
    val version = json["schemaVersion"]?.jsonPrimitive?.int ?: 1
    return when {
        version < 2 -> migrateV1toV2(json)
        else -> json
    }
}
```

Миграции — чистые функции, тестируемые. Старые проекты открываются в новой IDE без потерь.

## Валидация

При сохранении/загрузке:
- `id` уникален в пределах экрана
- `onClick` ссылается на существующий event
- `navigate destination` ссылается на существующий screen
- `type` — из списка поддерживаемых виджетов
- `action.type` — из whitelist (setState, navigate, ...)
- `expression` — парсится grammar без ошибок
- `schemaVersion` — поддерживаемая версия

Ошибки валидации показываются в UI как warnings (не блокируют работу, кроме невалидных action.type).

## Future Extensibility (post-MVP)

v1.0+ добавит:
- `action.type = "asyncCall"` — вызов coroutine функции из `custom/`
- `action.type = "dbQuery"` — Room-запрос (когда появится DB designer)
- `action.type = "httpGet"` — HTTP-запрос (через whitelist endpoints)
- Plugin widgets — `type` с префиксом `plugin:charts.LineChart`
- Custom state types — `List<Product>`, nested records

Все future extensions будут добавляться через bump `schemaVersion` + миграции.
