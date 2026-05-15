---
name: ai-ide-json-models
description: "Ai IDE JSON модели: ProjectModel, ScreenModel, WidgetNode, ResourceModels, domain/ProjectJson. Структура и schema. Триггеры: JSON model, screen model, widget node, project schema, ProjectJson, ScreenJson, WidgetJson, design/*.json."
---

# Ai IDE — JSON Models (Source of Truth)

## Принцип

JSON — source of truth для всех проектов. Визуальный редактор работает с JSON, из неё генерируется Kotlin-код.

## Два уровня моделей

### 1. Data layer (`data/model/`) — для Room/UI

| Модель | Файл | Назначение |
|--------|------|-----------|
| `ProjectModel` | `data/model/ProjectModel.kt` | Основная модель проекта (schemaVersion=2) |
| `ScreenModel` | `data/model/ScreenModel.kt` | Модель экрана с state, events, root widget |
| `WidgetNode` | `data/model/WidgetNode.kt` | Рекурсивное дерево виджетов |
| `ModifierModel` | `data/model/WidgetNode.kt` | Modifier-свойства (padding, size, background) |
| `StateVariable` | `data/model/ScreenModel.kt` | Переменная состояния (name, type, initial) |
| `EventModel` | `data/model/ScreenModel.kt` | Событие с actions |
| `ActionModel` | `data/model/ScreenModel.kt` | Действие (setState, navigate, showToast, custom) |

### 2. Domain layer (`domain/model/`) — для codegen

| Модель | Файл | Назначение |
|--------|------|-----------|
| `ProjectJson` | `domain/model/ProjectJson.kt` | Проект для генераторов |
| `ScreenJson` | (в ProjectJson) | Экран для генераторов |
| `WidgetJson` | (в ScreenJson) | Виджет для генераторов |

## ProjectModel (schema v2)

```kotlin
@Serializable
data class ProjectModel(
    val schemaVersion: Int = 2,
    val id: String,
    val name: String,
    val packageName: String,
    val minSdk: Int = 26,
    val targetSdk: Int = 35,
    val screens: List<String> = emptyList(),      // screen IDs
    val launcherScreen: String = "main",
    val permissions: List<String> = listOf("INTERNET"),
    val dependencies: List<String> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)
```

## ScreenModel (schema v2)

```kotlin
@Serializable
data class ScreenModel(
    val schemaVersion: Int = 2,
    val id: String,
    val name: String,
    val state: List<StateVariable> = emptyList(),
    val events: List<EventModel> = emptyList(),
    val root: WidgetNode,
)
```

## WidgetNode — рекурсивное дерево

```kotlin
@Serializable
data class WidgetNode(
    val type: String,           // "Column", "Text", "Button", ...
    val id: String,             // уникальный ID виджета
    val properties: Map<String, String> = emptyMap(),  // text, title, src, ...
    val modifier: ModifierModel = ModifierModel(),
    val children: List<WidgetNode> = emptyList(),      // рекурсия
)
```

## ModifierModel

```kotlin
@Serializable
data class ModifierModel(
    val fillMaxSize: Boolean = false,
    val fillMaxWidth: Boolean = false,
    val padding: Int? = null,           // dp
    val paddingHorizontal: Int? = null,
    val paddingVertical: Int? = null,
    val width: Int? = null,             // dp
    val height: Int? = null,            // dp
    val weight: Float? = null,          // для Row/Column children
    val background: String? = null,     // цвет (#RRGGBB или имя)
    val clip: String? = null,           // "circle", "rounded"
)
```

## ActionModel — типы действий

```kotlin
@Serializable
data class ActionModel(
    val type: String,           // "setState", "navigate", "showToast", "custom"
    val target: String = "",    // имя переменной или screen ID
    val expression: String = "", // выражение для setState
    val destination: String = "", // для navigate
    val message: String = "",   // для showToast
    val code: String = "",      // для custom (ограничено)
)
```

## Структура файлов проекта

```
<project-root>/
├── design/
│   ├── project.json          ← ProjectModel
│   └── screens/
│       ├── main.json         ← ScreenModel
│       ├── settings.json
│       └── ...
├── generated/                ← сгенерированный Kotlin (не редактировать!)
│   ├── MainActivity.kt
│   ├── AppNavigation.kt
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   └── screens/
│       ├── main.kt
│       └── ...
└── custom/                   ← пользовательский код (preserve при регенерации)
```

## Schema migration

`SchemaMigrator` конвертирует старые версии schema в актуальную.
`SchemaValidator` проверяет целостность JSON перед codegen.

## Текущее состояние

- Все модели сериализуются через `kotlinx.serialization`
- JsonModule — singleton Json с `ignoreUnknownKeys = true`
- ProjectModelRepository — CRUD для project.json
- Нет полной валидации всех полей (TODO)
