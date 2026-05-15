package my.company.ai.codegen

import my.company.ai.domain.model.ProjectJson
import java.io.File

/**
 * Оркестратор генерации всего проекта.
 * Вызывает все генераторы по очереди и собирает результат.
 */
class ProjectGenerator(
    private val generators: List<CodeGenerator> = listOf(
        ManifestGenerator(),
        MainActivityGenerator(),
        ThemeGenerator(),
    )
) {
    /**
     * Генерирует весь проект в указанную директорию.
     */
    fun generate(project: ProjectJson, outputDir: File): GenerationResult {
        val allFiles = mutableListOf<File>()
        val errors = mutableListOf<String>()

        generators.forEach { generator ->
            try {
                val files = generator.generate(project, outputDir)
                allFiles += files
            } catch (e: Exception) {
                errors += "${generator::class.simpleName}: ${e.message}"
            }
        }

        return GenerationResult(allFiles, errors)
    }
}
