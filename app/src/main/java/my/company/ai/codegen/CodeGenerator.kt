package my.company.ai.codegen

import my.company.ai.domain.model.ProjectJson
import java.io.File

/**
 * Интерфейс генераторов кода.
 */
interface CodeGenerator {
    /**
     * Генерирует файл(ы) в указанную директорию.
     */
    fun generate(project: ProjectJson, outputDir: File): List<File>
}

/**
 * Результат генерации проекта.
 */
data class GenerationResult(
    val generatedFiles: List<File>,
    val errors: List<String> = emptyList(),
)
