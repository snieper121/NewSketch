package my.company.ai.codegen

import com.squareup.kotlinpoet.*
import my.company.ai.domain.model.ProjectJson
import java.io.File

/**
 * Генерирует Theme.kt с Material3 темой.
 */
class ThemeGenerator : CodeGenerator {

    override fun generate(project: ProjectJson, outputDir: File): List<File> {
        val themeFile = FileSpec.builder("${project.packageName}.theme", "Theme")
            .addType(buildColorScheme())
            .addFunction(buildAppTheme())
            .build()

        val outFile = File(outputDir, "theme/Theme.kt")
        outFile.parentFile?.mkdirs()
        themeFile.writeTo(outFile.parentFile!!)
        return listOf(outFile)
    }

    private fun buildColorScheme(): TypeSpec {
        return TypeSpec.objectBuilder("AppColors")
            .addProperty(
                PropertySpec.builder("primary", ColorClass, KModifier.CONST)
                    .initializer("0xFF6750A4")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("onPrimary", ColorClass, KModifier.CONST)
                    .initializer("0xFFFFFFFF")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("background", ColorClass, KModifier.CONST)
                    .initializer("0xFFFFFBFE")
                    .build()
            )
            .build()
    }

    private fun buildAppTheme(): FunSpec {
        return FunSpec.builder("AppTheme")
            .addAnnotation(ComposableClass)
            .addParameter("content", LambdaTypeName.get(null as TypeName?, returnType = UnitClass), KModifier.NOINLINE)
            .addStatement(
                "val colorScheme = %T(\n" +
                "    primary = %T(AppColors.primary),\n" +
                "    onPrimary = %T(AppColors.onPrimary),\n" +
                "    background = %T(AppColors.background)\n" +
                ")",
                ClassName("androidx.compose.material3", "lightColorScheme"),
                ColorClass, ColorClass, ColorClass
            )
            .addStatement(
                "%T(\n" +
                "    colorScheme = colorScheme,\n" +
                "    typography = %T(),\n" +
                "    content = content\n" +
                ")",
                ClassName("androidx.compose.material3", "MaterialTheme"),
                ClassName("androidx.compose.material3", "Typography")
            )
            .build()
    }

    companion object {
        private val ColorClass = ClassName("androidx.compose.ui.graphics", "Color")
        private val ComposableClass = ClassName("androidx.compose.runtime", "Composable")
        private val UnitClass = ClassName("kotlin", "Unit")
    }
}
