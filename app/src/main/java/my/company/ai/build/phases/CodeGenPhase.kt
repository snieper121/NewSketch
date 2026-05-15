package my.company.ai.build.phases

import my.company.ai.build.BuildPhase
import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.PhaseResult
import my.company.ai.build.model.SampleProject
import my.company.ai.codegen.ProjectGenerator
import my.company.ai.domain.model.ProjectJson
import java.io.File

/**
 * Фаза 1: генерация Kotlin исходников из JSON модели проекта.
 * Если JSON модель отсутствует — используется SampleProject (M0 fallback).
 */
class CodeGenPhase(
    private val projectGenerator: ProjectGenerator = ProjectGenerator()
) : BuildPhase {
    override val name: String = "Code Generation"

    override suspend fun execute(context: BuildContext): PhaseResult {
        val designDir = File(context.projectDir, "design")
        val projectJsonFile = File(designDir, "project.json")

        return try {
            if (projectJsonFile.exists()) {
                // Реальная генерация из JSON модели
                val project = context.json.decodeFromString(
                    ProjectJson.serializer(),
                    projectJsonFile.readText()
                )
                val outputDir = File(context.projectDir, "generated/src")
                outputDir.mkdirs()
                val result = projectGenerator.generate(project, outputDir)
                if (result.errors.isNotEmpty()) {
                    PhaseResult.Failure("Ошибки генерации: ${result.errors.joinToString()}")
                } else {
                    PhaseResult.Success
                }
            } else {
                // M0 fallback: hardcoded Hello World
                SampleProject.writeTo(context.projectDir)
                PhaseResult.Success
            }
        } catch (e: Exception) {
            PhaseResult.Failure("Ошибка генерации кода: ${e.message}", e)
        }
    }
}
