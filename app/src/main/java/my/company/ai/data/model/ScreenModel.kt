package my.company.ai.data.model

import kotlinx.serialization.Serializable

/**
 * JSON-модель экрана (source of truth).
 * Сохраняется как design/screens/<id>.json.
 */
@Serializable
data class ScreenModel(
    val schemaVersion: Int = 2,
    val id: String,
    val name: String,
    val state: List<StateVariable> = emptyList(),
    val events: List<EventModel> = emptyList(),
    val root: WidgetNode,
)

@Serializable
data class StateVariable(
    val name: String,
    val type: String,
    val initial: String = "",
)

@Serializable
data class EventModel(
    val name: String,
    val actions: List<ActionModel> = emptyList(),
)

@Serializable
data class ActionModel(
    val type: String,
    val target: String = "",
    val expression: String = "",
    val destination: String = "",
    val message: String = "",
    val code: String = "",
)
