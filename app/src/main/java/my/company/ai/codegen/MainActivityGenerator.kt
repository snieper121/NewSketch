package my.company.ai.codegen

import com.squareup.kotlinpoet.*
import my.company.ai.domain.model.ProjectJson
import my.company.ai.domain.model.ScreenJson
import java.io.File

/**
 * Генерирует MainActivity.kt с Compose setContent.
 */
class MainActivityGenerator : CodeGenerator {

    override fun generate(project: ProjectJson, outputDir: File): List<File> {
        val file = FileSpec.builder(project.packageName, "MainActivity")
            .addType(buildMainActivity(project))
            .addType(buildNavigationGraph(project))
            .build()

        val outFile = File(outputDir, "MainActivity.kt")
        outFile.parentFile?.mkdirs()
        file.writeTo(outFile.parentFile!!)
        return listOf(outFile)
    }

    private fun buildMainActivity(project: ProjectJson): TypeSpec {
        val onCreate = FunSpec.builder("onCreate")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("savedInstanceState", ClassName("android.os", "Bundle").copy(nullable = true))
            .addStatement("super.onCreate(savedInstanceState)")
            .addStatement("setContent { %T { AppNavigation() } }", ClassName("${project.packageName}.theme", "AppTheme"))
            .build()

        return TypeSpec.classBuilder("MainActivity")
            .superclass(ClassName("androidx.activity", "ComponentActivity"))
            .addFunction(onCreate)
            .build()
    }

    private fun buildNavigationGraph(project: ProjectJson): TypeSpec {
        val composable = FunSpec.builder("AppNavigation")
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addStatement("val navController = %T()", ClassName("androidx.navigation.compose", "rememberNavController"))
            .beginControlFlow("%T(navController = navController, startDestination = %S)",
                ClassName("androidx.navigation.compose", "NavHost"),
                project.startScreenId ?: project.screens.firstOrNull()?.id ?: "home")

        project.screens.forEach { screen ->
            composable.addStatement("composable(%S) { %N() }", screen.id, screen.id)
        }

        composable.endControlFlow()
        val func = composable.build()

        return TypeSpec.objectBuilder("AppNavigation")
            .addFunction(func)
            .build()
    }
}
