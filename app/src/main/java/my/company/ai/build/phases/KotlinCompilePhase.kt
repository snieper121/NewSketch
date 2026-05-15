package my.company.ai.build.phases

import my.company.ai.build.BuildPhase
import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.PhaseResult
import my.company.ai.build.toolchain.ToolchainManager
import java.io.File

/**
 * Фаза 2: компиляция Kotlin → .class через kotlinc.
 * M0: stub — реальная компиляция будет реализована после интеграции toolchain.
 */
class KotlinCompilePhase(
    private val toolchain: ToolchainManager,
) : BuildPhase {
    override val name: String = "Kotlin Compilation"

    override suspend fun execute(context: BuildContext): PhaseResult {

        if (!toolchain.isToolchainReady()) {
            // M0: нет реального kotlinc — имитируем успех для проверки pipeline
            return PhaseResult.Success
        }

        val sources = context.projectDir.walkTopDown()
            .filter { it.extension == "kt" }
            .map { it.absolutePath }
            .toList()

        if (sources.isEmpty()) {
            return PhaseResult.Failure("Не найдены .kt файлы")
        }

        // TODO: запуск kotlinc через ProcessBuilder или Compiler API
        return PhaseResult.Success
    }
}
