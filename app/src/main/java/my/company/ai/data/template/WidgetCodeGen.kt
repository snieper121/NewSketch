package my.company.ai.data.template

import my.company.ai.data.model.ModifierModel
import my.company.ai.data.model.ScreenModel
import my.company.ai.data.model.WidgetNode

/**
 * Генерирует Compose Kotlin-код из JSON-модели экрана.
 */
object WidgetCodeGen {

    fun generateScreen(screen: ScreenModel, packageName: String): String = buildString {
        appendLine("package $packageName.screens")
        appendLine()
        appendLine("// @Generated — do not edit manually")
        appendLine()
        appendLine("import androidx.compose.foundation.layout.*")
        appendLine("import androidx.compose.material3.*")
        appendLine("import androidx.compose.runtime.*")
        appendLine("import androidx.compose.ui.Modifier")
        appendLine("import androidx.compose.ui.unit.dp")
        appendLine("import androidx.navigation.NavController")
        appendLine()
        appendLine("@Composable")
        appendLine("fun ${screen.name}(navController: NavController) {")

        // State variables
        screen.state.forEach { v ->
            val mutableFn = when (v.type) {
                "Int" -> "mutableIntStateOf(${v.initial})"
                "Float" -> "mutableFloatStateOf(${v.initial})"
                "Boolean" -> "mutableStateOf(${v.initial})"
                else -> "mutableStateOf(${v.initial})"
            }
            appendLine("    var ${v.name} by remember { $mutableFn }")
        }
        if (screen.state.isNotEmpty()) appendLine()

        // Widget tree
        appendWidget(screen.root, indent = 4, screen)
        appendLine("}")
    }

    private fun StringBuilder.appendWidget(node: WidgetNode, indent: Int, screen: ScreenModel) {
        val pad = " ".repeat(indent)
        when (node.type) {
            "Scaffold" -> appendScaffold(node, indent, screen)
            "Column" -> appendContainer("Column", node, indent, screen)
            "Row" -> appendContainer("Row", node, indent, screen)
            "Box" -> appendContainer("Box", node, indent, screen)
            "Card" -> appendCard(node, indent, screen)
            "LazyColumn" -> appendLazy("LazyColumn", node, indent, screen)
            "LazyRow" -> appendLazy("LazyRow", node, indent, screen)
            "Text" -> appendText(node, indent)
            "Button" -> appendButton(node, indent, screen)
            "TextField" -> appendTextField(node, indent)
            "Icon" -> appendLine("${pad}Icon(Icons.Default.Star, contentDescription = null)")
            "Image" -> appendLine("${pad}// Image: ${node.properties["src"] ?: "placeholder"}")
            "Spacer" -> appendSpacer(node, indent)
            "Divider" -> appendLine("${pad}HorizontalDivider()")
            "Switch" -> appendSwitch(node, indent)
            "TopAppBar" -> {} // handled inside Scaffold
            else -> appendLine("${pad}// Unknown widget: ${node.type}")
        }
    }

    private fun StringBuilder.appendScaffold(node: WidgetNode, indent: Int, screen: ScreenModel) {
        val pad = " ".repeat(indent)
        appendLine("${pad}Scaffold(")
        // topBar
        val topBar = node.children.find { it.type == "TopAppBar" }
            ?: node.properties["topBar"]?.let { null }
        if (topBar != null) {
            val title = topBar.properties["title"] ?: "App"
            appendLine("$pad    topBar = { TopAppBar(title = { Text(\"$title\") }) }")
        }
        appendLine("$pad) { paddingValues ->")
        appendLine("$pad    Column(modifier = Modifier.padding(paddingValues)) {")
        node.children.filter { it.type != "TopAppBar" }.forEach {
            appendWidget(it, indent + 8, screen)
        }
        appendLine("$pad    }")
        appendLine("$pad}")
    }

    private fun StringBuilder.appendContainer(name: String, node: WidgetNode, indent: Int, screen: ScreenModel) {
        val pad = " ".repeat(indent)
        val mod = generateModifier(node.modifier)
        appendLine("$pad$name($mod) {")
        node.children.forEach { appendWidget(it, indent + 4, screen) }
        appendLine("$pad}")
    }

    private fun StringBuilder.appendCard(node: WidgetNode, indent: Int, screen: ScreenModel) {
        val pad = " ".repeat(indent)
        val mod = generateModifier(node.modifier)
        appendLine("${pad}Card($mod) {")
        node.children.forEach { appendWidget(it, indent + 4, screen) }
        appendLine("$pad}")
    }

    private fun StringBuilder.appendLazy(name: String, node: WidgetNode, indent: Int, screen: ScreenModel) {
        val pad = " ".repeat(indent)
        val mod = generateModifier(node.modifier)
        appendLine("$pad$name($mod) {")
        appendLine("$pad    items(${node.children.size}) { index ->")
        node.children.forEachIndexed { i, child ->
            appendLine("$pad        if (index == $i) {")
            appendWidget(child, indent + 12, screen)
            appendLine("$pad        }")
        }
        appendLine("$pad    }")
        appendLine("$pad}")
    }

    private fun StringBuilder.appendText(node: WidgetNode, indent: Int) {
        val pad = " ".repeat(indent)
        val text = node.properties["text"] ?: ""
        val style = node.properties["style"]
        val stylePart = if (style != null) ", style = MaterialTheme.typography.$style" else ""
        appendLine("${pad}Text(text = \"$text\"$stylePart)")
    }

    private fun StringBuilder.appendButton(node: WidgetNode, indent: Int, screen: ScreenModel) {
        val pad = " ".repeat(indent)
        val text = node.properties["text"] ?: "Button"
        val onClick = node.properties["onClick"]
        val action = screen.events.find { it.name == onClick }
        val body = if (action != null) {
            action.actions.joinToString("; ") { a ->
                when (a.type) {
                    "setState" -> "${a.target} = ${a.expression}"
                    "navigate" -> "navController.navigate(\"${a.destination}\")"
                    "navigateBack" -> "navController.popBackStack()"
                    else -> "/* ${a.type} */"
                }
            }
        } else "/* TODO */"
        appendLine("${pad}Button(onClick = { $body }) {")
        appendLine("$pad    Text(\"$text\")")
        appendLine("$pad}")
    }

    private fun StringBuilder.appendTextField(node: WidgetNode, indent: Int) {
        val pad = " ".repeat(indent)
        val label = node.properties["label"] ?: ""
        appendLine("${pad}OutlinedTextField(value = \"\", onValueChange = {}, label = { Text(\"$label\") })")
    }

    private fun StringBuilder.appendSpacer(node: WidgetNode, indent: Int) {
        val pad = " ".repeat(indent)
        val h = node.modifier.height
        if (h != null) {
            appendLine("${pad}Spacer(modifier = Modifier.height(${h}.dp))")
        } else {
            appendLine("${pad}Spacer(modifier = Modifier.height(16.dp))")
        }
    }

    private fun StringBuilder.appendSwitch(node: WidgetNode, indent: Int) {
        val pad = " ".repeat(indent)
        appendLine("${pad}Switch(checked = false, onCheckedChange = {})")
    }

    private fun generateModifier(mod: ModifierModel): String {
        val parts = mutableListOf<String>()
        if (mod.fillMaxSize) parts += ".fillMaxSize()"
        if (mod.fillMaxWidth) parts += ".fillMaxWidth()"
        mod.padding?.let { parts += ".padding(${it}.dp)" }
        mod.paddingHorizontal?.let { parts += ".padding(horizontal = ${it}.dp)" }
        mod.paddingVertical?.let { parts += ".padding(vertical = ${it}.dp)" }
        mod.width?.let { parts += ".width(${it}.dp)" }
        mod.height?.let { parts += ".height(${it}.dp)" }
        mod.weight?.let { parts += ".weight(${it}f)" }
        return if (parts.isEmpty()) "" else "modifier = Modifier${parts.joinToString("")}"
    }
}
