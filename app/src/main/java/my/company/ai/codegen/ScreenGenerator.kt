package my.company.ai.codegen

import com.squareup.kotlinpoet.*
import my.company.ai.domain.model.ScreenJson
import my.company.ai.domain.model.WidgetJson
import java.io.File

/**
 * Генерирует Compose-функции экранов из ScreenJson + WidgetJson дерева.
 */
class ScreenGenerator : CodeGenerator {

    override fun generate(project: my.company.ai.domain.model.ProjectJson, outputDir: File): List<File> {
        val files = mutableListOf<File>()

        project.screens.forEach { screen ->
            val fileSpec = generateScreenFile(screen, project.packageName)
            val outFile = File(outputDir, "screens/${screen.id}.kt")
            outFile.parentFile?.mkdirs()
            fileSpec.writeTo(outFile.parentFile!!)
            files += outFile
        }

        return files
    }

    private fun generateScreenFile(screen: ScreenJson, packageName: String): FileSpec {
        val funSpec = FunSpec.builder(screen.id)
            .addAnnotation(ComposableClass)
            .addStatement("val navController = %T.current", ClassName("androidx.navigation", "NavHostController").nestedClass("Companion"))
            // TODO: state bindings via rememberSaveable
            .addCode(generateWidgetTree(screen.widgets))
            .build()

        return FileSpec.builder(packageName, screen.id)
            .addFunction(funSpec)
            .build()
    }

    /**
     * Рекурсивно генерирует Compose-код для дерева виджетов.
     */
    private fun generateWidgetTree(widgets: List<WidgetJson>): CodeBlock {
        val builder = CodeBlock.builder()

        widgets.forEach { widget ->
            builder.add(generateWidget(widget))
        }

        return builder.build()
    }

    private fun generateWidget(widget: WidgetJson): CodeBlock {
        return when (widget.type.lowercase()) {
            "column" -> generateColumn(widget)
            "row" -> generateRow(widget)
            "box" -> generateBox(widget)
            "text" -> generateText(widget)
            "button" -> generateButton(widget)
            "textfield" -> generateTextField(widget)
            "image" -> generateImage(widget)
            "spacer" -> generateSpacer(widget)
            "divider" -> generateDivider(widget)
            "card" -> generateCard(widget)
            "scaffold" -> generateScaffold(widget)
            "topappbar" -> generateTopAppBar(widget)
            "bottomnavigation" -> generateBottomNavigation(widget)
            "lazyColumn".lowercase() -> generateLazyColumn(widget)
            "lazyRow".lowercase() -> generateLazyRow(widget)
            else -> {
                // Неизвестный виджет — рендерим как Box с placeholder
                CodeBlock.builder()
                    .addStatement("/* Неизвестный виджет: ${widget.type} */")
                    .addStatement("%T(modifier = %T.fillMaxWidth().height(48.dp)) {}",
                        ClassName("androidx.compose.foundation.layout", "Box"),
                        ClassName("androidx.compose.ui", "Modifier"))
                    .build()
            }
        }
    }

    private fun generateColumn(widget: WidgetJson): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("%T(", ClassName("androidx.compose.foundation.layout", "Column"))
        // TODO: parse properties (modifier, verticalArrangement, horizontalAlignment)
        widget.children.forEach { child ->
            builder.add(generateWidget(child))
        }
        builder.endControlFlow()
        return builder.build()
    }

    private fun generateRow(widget: WidgetJson): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("%T(", ClassName("androidx.compose.foundation.layout", "Row"))
        widget.children.forEach { child ->
            builder.add(generateWidget(child))
        }
        builder.endControlFlow()
        return builder.build()
    }

    private fun generateBox(widget: WidgetJson): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("%T(", ClassName("androidx.compose.foundation.layout", "Box"))
        widget.children.forEach { child ->
            builder.add(generateWidget(child))
        }
        builder.endControlFlow()
        return builder.build()
    }

    private fun generateText(widget: WidgetJson): CodeBlock {
        val text = widget.properties["text"]?.toString()?.trim('"') ?: "Text"
        return CodeBlock.builder()
            .addStatement("%T(%S)", ClassName("androidx.compose.material3", "Text"), text)
            .build()
    }

    private fun generateButton(widget: WidgetJson): CodeBlock {
        val text = widget.properties["text"]?.toString()?.trim('"') ?: "Button"
        val onClickAction = widget.actions.firstOrNull()
        val builder = CodeBlock.builder()
        if (onClickAction != null) {
            builder.beginControlFlow("%T(onClick = { /* TODO: ${onClickAction.type} */ })",
                ClassName("androidx.compose.material3", "Button"))
        } else {
            builder.beginControlFlow("%T(onClick = {})",
                ClassName("androidx.compose.material3", "Button"))
        }
        builder.addStatement("%T(%S)", ClassName("androidx.compose.material3", "Text"), text)
        builder.endControlFlow()
        return builder.build()
    }

    private fun generateTextField(widget: WidgetJson): CodeBlock {
        val label = widget.properties["label"]?.toString()?.trim('"') ?: ""
        return CodeBlock.builder()
            .addStatement("var text by %T { %T(%S) }",
                ClassName("androidx.compose.runtime", "remember"),
                ClassName("androidx.compose.runtime.mutableStateOf"),
                "")
            .addStatement("%T(\n  value = text,\n  onValueChange = { text = it },\n  label = { %T(%S) }\n)",
                ClassName("androidx.compose.material3", "TextField"),
                ClassName("androidx.compose.material3", "Text"),
                label)
            .build()
    }

    private fun generateImage(widget: WidgetJson): CodeBlock {
        val src = widget.properties["src"]?.toString()?.trim('"') ?: ""
        return CodeBlock.builder()
            .addStatement("/* Image: $src */")
            .addStatement("%T(\n  painter = %T(id = R.drawable.placeholder),\n  contentDescription = null,\n  modifier = %T.size(64.dp)\n)",
                ClassName("androidx.compose.foundation", "Image"),
                ClassName("androidx.compose.ui.res", "painterResource"),
                ClassName("androidx.compose.ui", "Modifier"))
            .build()
    }

    private fun generateSpacer(widget: WidgetJson): CodeBlock {
        return CodeBlock.builder()
            .addStatement("%T(modifier = %T.height(16.dp))",
                ClassName("androidx.compose.foundation.layout", "Spacer"),
                ClassName("androidx.compose.ui", "Modifier"))
            .build()
    }

    private fun generateDivider(widget: WidgetJson): CodeBlock {
        return CodeBlock.builder()
            .addStatement("%T()", ClassName("androidx.compose.material3", "HorizontalDivider"))
            .build()
    }

    private fun generateCard(widget: WidgetJson): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("%T(", ClassName("androidx.compose.material3", "Card"))
        widget.children.forEach { child ->
            builder.add(generateWidget(child))
        }
        builder.endControlFlow()
        return builder.build()
    }

    private fun generateScaffold(widget: WidgetJson): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("%T(", ClassName("androidx.compose.material3", "Scaffold"))
        // TODO: topBar, bottomBar, floatingActionButton from children
        widget.children.forEach { child ->
            builder.add(generateWidget(child))
        }
        builder.endControlFlow()
        return builder.build()
    }

    private fun generateTopAppBar(widget: WidgetJson): CodeBlock {
        val title = widget.properties["title"]?.toString()?.trim('"') ?: ""
        return CodeBlock.builder()
            .addStatement("%T(title = { %T(%S) })",
                ClassName("androidx.compose.material3", "TopAppBar"),
                ClassName("androidx.compose.material3", "Text"),
                title)
            .build()
    }

    private fun generateBottomNavigation(widget: WidgetJson): CodeBlock {
        // M0: placeholder
        return CodeBlock.builder()
            .addStatement("/* BottomNavigation: ${widget.children.size} items */")
            .build()
    }

    private fun generateLazyColumn(widget: WidgetJson): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("%T(", ClassName("androidx.compose.foundation.lazy", "LazyColumn"))
        // M0: placeholder — items from state or hardcoded
        widget.children.forEach { child ->
            builder.beginControlFlow("item")
            builder.add(generateWidget(child))
            builder.endControlFlow()
        }
        builder.endControlFlow()
        return builder.build()
    }

    private fun generateLazyRow(widget: WidgetJson): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("%T(", ClassName("androidx.compose.foundation.lazy", "LazyRow"))
        widget.children.forEach { child ->
            builder.beginControlFlow("item")
            builder.add(generateWidget(child))
            builder.endControlFlow()
        }
        builder.endControlFlow()
        return builder.build()
    }

    companion object {
        private val ComposableClass = ClassName("androidx.compose.runtime", "Composable")
    }
}
