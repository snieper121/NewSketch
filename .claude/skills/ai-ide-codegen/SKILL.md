---
name: ai-ide-codegen
description: "Ai IDE codegen: KotlinPoet генерация Compose кода из JSON моделей. ProjectGenerator, ScreenGenerator, ManifestGen, ThemeCodeGen. Триггеры: codegen, KotlinPoet, generate screen, generate code, ScreenGenerator, ProjectGenerator, ManifestGenerator, ThemeGenerator."
---

# Ai IDE — Code Generation (KotlinPoet)

## Принцип

JSON model → Kotlin source code. Генерация однонаправленная: JSON всегда первичен.
Сгенерированный код не редактируется вручную.

## Архитектура

```
ProjectJson (domain model)
    → ProjectGenerator (оркестратор)
        → ManifestGenerator    → AndroidManifest.xml
        → ThemeGenerator       → Theme.kt, Color.kt, Type.kt
        → MainActivityGenerator → MainActivity.kt
        → ScreenGenerator      → screens/<id>.kt (Compose функции)
```

### Ключевые файлы

| Файл | Роль |
|------|------|
| `codegen/CodeGenerator.kt` | Interface: `fun generate(project, outputDir): List<File>` |
| `codegen/ProjectGenerator.kt` | Оркестратор: вызывает все генераторы, собирает результат |
| `codegen/ManifestGenerator.kt` | Генерирует AndroidManifest.xml |
| `codegen/ThemeGenerator.kt` | Material3 theme: Color.kt, Theme.kt, Type.kt |
| `codegen/MainActivityGenerator.kt` | MainActivity с setContent { AppTheme { AppNavigation() } } |
| `codegen/ScreenGenerator.kt` | Рекурсивно генерирует Compose-функции из WidgetJson дерева |

### ScreenGenerator — поддерживаемые виджеты

| Widget type | Compose equivalent |
|-------------|-------------------|
| `column` | `Column { }` |
| `row` | `Row { }` |
| `box` | `Box { }` |
| `text` | `Text("...")` |
| `button` | `Button(onClick = { }) { Text("...") }` |
| `textfield` | `TextField(value, onValueChange, label)` |
| `image` | `Image(painterResource(...))` |
| `spacer` | `Spacer(Modifier.height(16.dp))` |
| `divider` | `HorizontalDivider()` |
| `card` | `Card { }` |
| `scaffold` | `Scaffold { }` |
| `topappbar` | `TopAppBar(title = { Text("...") })` |
| `lazyColumn` | `LazyColumn { item { } }` |
| `lazyRow` | `LazyRow { item { } }` |
| `bottomnavigation` | Placeholder (TODO) |

### Используемый API KotlinPoet

```kotlin
FileSpec.builder(packageName, fileName)
    .addFunction(FunSpec.builder("ScreenName")
        .addAnnotation(ComposableClass)
        .addCode(CodeBlock.builder()
            .addStatement("%T(...)", ClassName(...))
            .build())
        .build())
    .build()
```

- `%T` — тип (ClassName)
- `%S` — строка
- `%L` — литерал
- `beginControlFlow` / `endControlFlow` — для `Column { }`, `Row { }` и т.д.

### Текущее состояние

- Все генераторы работают с `ProjectJson` / `ScreenJson` / `WidgetJson`
- ScreenGenerator рекурсивно обходит дерево виджетов
- ThemeGenerator генерирует Material3 color scheme
- Нет PreserveBlock (TODO)
- Нет GenCache (TODO)
- Нет ExpressionParser для actions (TODO — сейчас `/* TODO: action */`)

### Что нужно доработать

1. State bindings через `rememberSaveable` + `MutableState`
2. ExpressionParser с blacklist (System/Runtime/ClassLoader)
3. Action handlers (navigate, setState, showToast)
4. PreserveBlock для custom кода
5. Incremental codegen (только изменённые экраны)
