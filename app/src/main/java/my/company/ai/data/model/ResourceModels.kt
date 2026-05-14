package my.company.ai.data.model

import kotlinx.serialization.Serializable

/**
 * Тема проекта. Сохраняется как design/theme.json.
 */
@Serializable
data class ThemeModel(
    val schemaVersion: Int = 1,
    val colorScheme: String = "dynamic",
    val seedColor: String = "#6750A4",
    val fontFamily: String = "Roboto",
    val shapeCornerRadius: Int = 16,
    val useDarkTheme: String = "system",
)

/**
 * Ресурсы проекта: цвета. Сохраняется как design/resources/colors.json.
 */
@Serializable
data class ColorsResource(
    val schemaVersion: Int = 1,
    val colors: Map<String, String> = mapOf(
        "primary" to "#6750A4",
        "secondary" to "#958DA5",
        "error" to "#B3261E",
        "background" to "#FFFBFE",
    ),
)

/**
 * Ресурсы проекта: строки. Сохраняется как design/resources/strings.json.
 */
@Serializable
data class StringsResource(
    val schemaVersion: Int = 1,
    val strings: Map<String, String> = mapOf("app_name" to "My App"),
)
