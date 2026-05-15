package my.company.ai.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Корневая JSON-модель проекта.
 *
 * Сериализуется в `design/project.json`.
 */
@Serializable
data class ProjectJson(
    val schemaVersion: Int = 1,
    val name: String,
    val packageName: String,
    val minSdk: Int = 29,
    val targetSdk: Int = 36,
    val screens: List<ScreenJson> = emptyList(),
    val state: List<StateJson> = emptyList(),
    val resources: ResourceJson = ResourceJson(),
    val dependencies: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
)

@Serializable
data class ScreenJson(
    val id: String,
    val name: String,
    val route: String,
    val isStart: Boolean = false,
    val widgets: List<WidgetJson> = emptyList(),
)

@Serializable
data class WidgetJson(
    val id: String,
    val type: String, // "Column", "Text", "Button", ...
    val properties: JsonObject = JsonObject(emptyMap()),
    val children: List<WidgetJson> = emptyList(),
    val actions: List<ActionJson> = emptyList(),
)

@Serializable
data class ActionJson(
    val type: String, // "navigate", "setState", "showDialog"
    val target: String? = null,
    val payload: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class StateJson(
    val id: String,
    val name: String,
    val type: String, // "String", "Int", "Boolean", "List"
    val defaultValue: String? = null,
    val isPersistent: Boolean = false,
)

@Serializable
data class ResourceJson(
    val colors: List<ColorResource> = emptyList(),
    val strings: List<StringResource> = emptyList(),
    val drawables: List<DrawableResource> = emptyList(),
)

@Serializable
data class ColorResource(
    val name: String,
    val value: String, // hex #RRGGBB или #AARRGGBB
)

@Serializable
data class StringResource(
    val name: String,
    val value: String,
)

@Serializable
data class DrawableResource(
    val name: String,
    val type: String, // "vector", "png"
    val content: String? = null, // base64 для png или xml для vector
)
