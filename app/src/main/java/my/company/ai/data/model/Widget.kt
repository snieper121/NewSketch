package my.company.ai.data.model

/**
 * Типы виджетов визуального редактора. Mapping «тип → Compose-код» делается
 * отдельным генератором (см. `design.WidgetCodeGen`, будет добавлен позже).
 */
enum class WidgetType(
    val displayName: String,
    val defaultText: String = "",
    val isContainer: Boolean = false,
) {
    Text(displayName = "Text", defaultText = "Text"),
    Button(displayName = "Button", defaultText = "Button"),
    TextField(displayName = "TextField", defaultText = "Input"),
    Image(displayName = "Image"),
    Icon(displayName = "Icon"),
    Checkbox(displayName = "Checkbox", defaultText = "Checkbox"),
    Switch(displayName = "Switch"),
    Divider(displayName = "Divider"),
    Spacer(displayName = "Spacer"),
    Card(displayName = "Card", isContainer = true),
    Column(displayName = "Column", isContainer = true),
    Row(displayName = "Row", isContainer = true),
    Box(displayName = "Box", isContainer = true),
}
