# 05 — Code Generation

## Принцип

JSON model → Kotlin source code. Генерация — однонаправленная: JSON всегда первичен. Сгенерированный код не редактируется вручную (для этого есть `custom/`).

## Подход: KotlinPoet (переосмыслено)

В v1 плана был выбор string templates. **Пересмотрено в пользу KotlinPoet** по следующим причинам:

| | String Templates | KotlinPoet |
|---|---|---|
| Ошибки форматирования | Частые | Исключены |
| Escape символов | Вручную | Автоматически |
| Управление imports | Вручную | Автоматически |
| Unit tests | Text comparison | AST comparison |
| Рост сложности (50+ виджетов) | Быстрая деградация | Масштабируется |
| Размер зависимости | 0 | ~2 MB |

2 MB в IDE (не в generated apps!) — допустимая цена за корректность.

**Maven:** `com.squareup:kotlinpoet:1.18.1`

### Пример использования KotlinPoet

```kotlin
val fileSpec = FileSpec.builder("com.example.myapp.screens", "MainScreen")
    .addFileComment("@Generated — do not edit manually")
    .addFileComment("Source: design/screens/main.json")
    .addImport("androidx.compose.foundation.layout", "*")
    .addImport("androidx.compose.material3", "*")
    .addImport("androidx.compose.runtime", "*")
    .addFunction(
        FunSpec.builder("MainScreen")
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addParameter("navController", ClassName("androidx.navigation", "NavController"))
            .addCode(generateScreenBody(screen))
            .build()
    )
    .build()

fileSpec.writeTo(outputDir)
```

Для тел composable'ов используется `CodeBlock.builder()` с `.add()` и placeholders (`%T` для типов, `%L` для literals, `%S` для strings).

## Генерируемые файлы

| Файл | Источник | Содержимое |
|------|----------|-----------|
| `MainActivity.kt` | project.json | `setContent { AppTheme { AppNavigation() } }` |
| `AppNavigation.kt` | project.json (screens list) | `NavHost` + composable routes |
| `theme/Theme.kt` | theme.json | `MaterialTheme` wrapper, dynamic colors |
| `theme/Color.kt` | resources/colors.json | `Color` constants |
| `theme/Type.kt` | theme.json (fontFamily) | `Typography` |
| `screens/<Name>Screen.kt` | screens/<id>.json | `@Composable` function |
| `AndroidManifest.xml` | project.json | permissions, activity |
| `res/values/strings.xml` | resources/strings.json | XML resources |
| `res/values/themes.xml` | theme.json | launcher theme |

## Пример: Screen Generation

**Вход** (main.json):
```json
{
  "id": "main",
  "name": "MainScreen",
  "state": [{ "name": "counter", "type": "Int", "initial": "0" }],
  "events": [{ "name": "onIncrement", "actions": [
    { "type": "setState", "target": "counter", "expression": "counter + 1" }
  ]}],
  "root": {
    "type": "Column",
    "modifier": { "fillMaxSize": true, "padding": 16 },
    "children": [
      { "type": "Text", "properties": { "text": "${counter}", "style": "headlineMedium" } },
      { "type": "Button", "properties": { "text": "Add", "onClick": "onIncrement" } }
    ]
  }
}
```

**Выход** (MainScreen.kt через KotlinPoet):
```kotlin
// @Generated — do not edit manually
// Source: design/screens/main.json
// Generated at: 2026-05-11T20:00:00Z

package com.example.myapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
public fun MainScreen(navController: NavController) {
    var counter by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "$counter", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { counter = counter + 1 }) {
            Text("Add")
        }
    }
}
```

## State Binding Pattern

Паттерн reactivity (см. `04-data-model.md` → State Variable):

**Генерация state** — через `rememberSaveable` для переживания configuration changes и process death:

```kotlin
// JSON: { "name": "counter", "type": "Int", "initial": "0" }
var counter by rememberSaveable { mutableIntStateOf(0) }

// JSON: { "name": "userName", "type": "String", "initial": "\"\"" }
var userName by rememberSaveable { mutableStateOf("") }

// JSON: { "name": "isEnabled", "type": "Boolean", "initial": "false" }
var isEnabled by rememberSaveable { mutableStateOf(false) }
```

**Почему `rememberSaveable`, не `remember`:**
- `remember` теряется при rotation, split screen, dark mode switch
- `rememberSaveable` сохраняется через Bundle (выживает configuration changes)
- Для простых типов (Int, String, Boolean, Float) — работает автоматически
- Для сложных объектов (List, custom data classes) — нужен custom `Saver` (генерируется автоматически)

**Исключения — используется `remember` без Saveable:**
- `rememberCoroutineScope()` — не сохраняется (coroutine scope recreation OK)
- `NavController` — передаётся, не хранится
- `remember { Animatable(...) }` — анимации перезапускаются (OK для UX)

**Read binding (`${state}` в property):**
```json
{ "type": "Text", "properties": { "text": "Hi ${userName}" } }
```
→
```kotlin
Text(text = "Hi $userName")
```

**Two-way binding (TextField):**
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

**Action execution (setState):**
```json
{ "type": "setState", "target": "counter", "expression": "counter + 1" }
```

`expression` парсится grammar (`04-data-model.md`) и транслируется в Kotlin:
```kotlin
counter = counter + 1
```

Генератор expression делает AST-обход, не `toString()`, чтобы избежать инъекций.

## Navigation Generation (Navigation 2.9 type-safe)

Используем `@Serializable` data classes вместо строковых routes.

Из `project.json` → `AppNavigation.kt`:

```kotlin
import kotlinx.serialization.Serializable

@Serializable data object MainRoute
@Serializable data object SettingsRoute

@Composable
public fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MainRoute) {
        composable<MainRoute> { MainScreen(navController) }
        composable<SettingsRoute> { SettingsScreen(navController) }
    }
}
```

**Преимущества type-safe (Nav 2.9+):**
- Compile-time проверка destination
- Type-safe arguments через data class fields
- `navController.navigate(SettingsRoute)` вместо `navController.navigate("settings")`

**Генерация:**
- Каждый screen → `@Serializable data object <Name>Route`
- Если screen принимает аргументы → `@Serializable data class <Name>Route(val arg: Type)`
- Action `navigate` → `navController.navigate(<Name>Route)`

## Theme Generation

Из `theme.json` + `colors.json`:

```kotlin
@Composable
public fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(primary = Primary, secondary = Secondary)
        else -> lightColorScheme(primary = Primary, secondary = Secondary)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

## Custom Code (user-written)

Файлы в `custom/` **никогда не перезаписываются** при регенерации.

### @Generated + PRESERVE маркеры

Все сгенерированные файлы начинаются с `@Generated` header:

```kotlin
// @Generated — do not edit manually
// Source: design/screens/main.json
// Generated at: 2026-05-11T20:00:00Z
// PRESERVE-BELOW: нет
```

`PRESERVE-BELOW` блок позволяет сохранить пользовательский код в конце generated-файла:

```kotlin
// PRESERVE-BELOW: START
// Этот блок сохраняется при regeneration
private fun customHelper() {
    // ...
}
// PRESERVE-BELOW: END
```

При regenerate:
1. Читается старый .kt файл
2. Извлекается блок между `PRESERVE-BELOW: START` и `PRESERVE-BELOW: END`
3. Генерируется новый файл из JSON
4. Preserve-блок вставляется обратно

### Модель custom/ папки

Файлы в `custom/` — полностью user's. IDE не трогает:
```
custom/
├── Utils.kt              — user helper functions
├── network/ApiClient.kt  — user code
└── data/Repository.kt    — user code
```

В event actions на них можно ссылаться через import:
```kotlin
// Generated code:
import com.example.myapp.custom.Utils
// ...
Button(onClick = { Utils.doSomething() })
```

Imports для custom/ добавляются codegen'ом при обнаружении ссылок.

## Modifier Generation

KotlinPoet для модификаторов:
```kotlin
fun ModifierModel.toCodeBlock(): CodeBlock {
    val builder = CodeBlock.builder().add("%T", MODIFIER)
    if (fillMaxSize) builder.add("\n    .fillMaxSize()")
    if (fillMaxWidth) builder.add("\n    .fillMaxWidth()")
    padding?.let { builder.add("\n    .padding(%L.dp)", it) }
    width?.let { builder.add("\n    .width(%L.dp)", it) }
    height?.let { builder.add("\n    .height(%L.dp)", it) }
    weight?.let { builder.add("\n    .weight(%Lf)", it) }
    background?.let {
        val hex = it.removePrefix("#")
        builder.add("\n    .background(%T(0xFF%L))", COLOR, hex)
    }
    clip?.let {
        val radius = it.removePrefix("rounded_")
        builder.add("\n    .clip(%T(%L.dp))", ROUNDED_CORNER_SHAPE, radius)
    }
    border?.let { b ->
        builder.add(
            "\n    .border(width = %L.dp, color = %T(0xFF%L))",
            b.width, COLOR, b.color.removePrefix("#")
        )
    }
    shadow?.let { s ->
        builder.add("\n    .shadow(elevation = %L.dp)", s.elevation)
    }
    alpha?.let { builder.add("\n    .alpha(%Lf)", it) }
    return builder.build()
}

private val MODIFIER = ClassName("androidx.compose.ui", "Modifier")
private val COLOR = ClassName("androidx.compose.ui.graphics", "Color")
private val ROUNDED_CORNER_SHAPE = ClassName("androidx.compose.foundation.shape", "RoundedCornerShape")
```

## Regeneration Strategy

При изменении JSON:
1. Определить какие файлы затронуты (diff JSON → список файлов)
2. Прочитать существующие generated-файлы → извлечь PRESERVE-блоки
3. Перегенерировать затронутые .kt
4. Вставить preserve-блоки обратно
5. Обновить `build/gen-cache.json`

```kotlin
data class GenCache(
    val files: Map<String, FileEntry>
)
data class FileEntry(
    val sourceHash: String,        // SHA-256 of source JSON
    val generatedAt: Long,
    val targetPath: String
)
```

Build pipeline перекомпилирует только изменённые .kt (по timestamp).

## Gradle Export

При экспорте (v0.2+) дополнительно генерируются:

### settings.gradle.kts
```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "MyApp"
include(":app")
```

### build.gradle.kts (root)
```kotlin
plugins {
    id("com.android.application") version "8.10.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}
```

### app/build.gradle.kts
Содержит `compose = true`, все зависимости от Compose BOM, applicationId из project.json.

### gradle/wrapper/gradle-wrapper.properties
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### gradle/wrapper/gradle-wrapper.jar
**Источник:** bundled в IDE APK как asset. Версия — фиксированная с gradle-wrapper.properties.

Распаковывается при export. SHA-256 проверяется.

### gradlew + gradlew.bat
Тоже bundled в assets, копируются as-is.

### .gitignore
```
/.gradle
/build/
/local.properties
*.iml
.DS_Store
```

### version catalog (опционально)

Если `exportWithVersionCatalog = true` в настройках:
```
gradle/libs.versions.toml
```
Генерируется из project.json dependencies + toolchain manifest.

## Testing Strategy (codegen)

**Golden file tests** (см. `08-testing-strategy.md`):
```kotlin
@Test
fun `generates expected code for counter screen`() {
    val screen = loadScreen("counter.json")
    val generated = ScreenGenerator.generate(screen, "com.test")
    val expected = readResource("counter_expected.kt")
    assertThat(generated).isEqualTo(expected)
}
```

Эта стратегия — почему KotlinPoet важен: он даёт **детерминированный** output, не зависящий от whitespace/format, что делает golden tests надёжными.

## Performance

Для 5-экранного проекта:
- Full regeneration: < 500 ms
- Incremental (1 экран): < 100 ms
- KotlinPoet overhead: ~50 ms на файл

Для 20-экранного проекта:
- Full: < 2 сек
- Incremental: < 150 ms
