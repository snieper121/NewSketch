package my.company.ai.data.model

import kotlinx.serialization.Serializable

/**
 * Узел дерева виджетов. Рекурсивная структура — каждый виджет может содержать children.
 */
@Serializable
data class WidgetNode(
    val type: String,
    val id: String,
    val properties: Map<String, String> = emptyMap(),
    val modifier: ModifierModel = ModifierModel(),
    val children: List<WidgetNode> = emptyList(),
)

/**
 * Modifier-свойства виджета.
 */
@Serializable
data class ModifierModel(
    val fillMaxSize: Boolean = false,
    val fillMaxWidth: Boolean = false,
    val padding: Int? = null,
    val paddingHorizontal: Int? = null,
    val paddingVertical: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val weight: Float? = null,
    val background: String? = null,
    val clip: String? = null,
)
