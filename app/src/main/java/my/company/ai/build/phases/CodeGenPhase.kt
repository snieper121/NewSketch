package my.company.ai.build.phases

import my.company.ai.build.BuildPhase
import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.PhaseResult
import my.company.ai.build.model.SampleProject

/**
 * Фаза 1: генерация hardcoded Hello World исходников.
 */
class CodeGenPhase : BuildPhase {
    override val name: String = "Code Generation"

    override suspend fun execute(context: BuildContext): PhaseResult {

        return try {
            SampleProject.writeTo(context.projectDir)
            PhaseResult.Success
        } catch (e: Exception) {
            PhaseResult.Failure("Ошибка генерации кода: ${e.message}", e)
        }
    }
}
