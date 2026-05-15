package my.company.ai.build

import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.PhaseResult

/**
 * Единая фаза сборки. Все фазы реализуют этот интерфейс.
 */
interface BuildPhase {
    val name: String

    /**
     * Выполнить фазу. Может быть отменено через [isCancelled].
     */
    suspend fun execute(context: BuildContext, isCancelled: () -> Boolean): PhaseResult
}
