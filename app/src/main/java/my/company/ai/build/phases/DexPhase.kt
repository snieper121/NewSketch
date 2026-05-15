package my.company.ai.build.phases

import my.company.ai.build.BuildPhase
import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.PhaseResult
import my.company.ai.build.toolchain.ToolchainManager

/**
 * Фаза 6: конвертация .class → .dex через d8.
 * M0: stub.
 */
class DexPhase(
    private val toolchain: ToolchainManager,
) : BuildPhase {
    override val name: String = "Dex"

    override suspend fun execute(context: BuildContext): PhaseResult {
        // TODO: d8 classes.jar → classes.dex
        return PhaseResult.Success
    }
}
