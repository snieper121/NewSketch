package my.company.ai.build.phases

import my.company.ai.build.BuildPhase
import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.PhaseResult
import my.company.ai.build.toolchain.ToolchainManager

/**
 * Фаза 3–4: компиляция и линковка ресурсов через aapt2.
 * M0: stub.
 */
class ResourceCompilePhase(
    private val toolchain: ToolchainManager,
) : BuildPhase {
    override val name: String = "Resource Compilation"

    override suspend fun execute(context: BuildContext): PhaseResult {
        // TODO: aapt2 compile + aapt2 link
        return PhaseResult.Success
    }
}
