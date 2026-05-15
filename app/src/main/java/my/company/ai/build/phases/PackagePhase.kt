package my.company.ai.build.phases

import my.company.ai.build.BuildPhase
import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.PhaseResult
import my.company.ai.build.toolchain.ToolchainManager

/**
 * Фаза 7–9: merge APK → zipalign → sign.
 * M0: stub.
 */
class PackagePhase(
    private val toolchain: ToolchainManager,
) : BuildPhase {
    override val name: String = "Package & Sign"

    override suspend fun execute(context: BuildContext, isCancelled: () -> Boolean): PhaseResult {
        if (isCancelled()) return PhaseResult.Failure("Отменено")
        // TODO: zip resources + dex → unsigned.apk → zipalign → sign
        return PhaseResult.Success
    }
}
