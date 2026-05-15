package my.company.ai.build

import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.PhaseResult

/**
 * Единая фаза сборки. Все фазы реализуют этот интерфейс.
 *
 * Cancellation поддерживается естественно через механизм корутин —
 * если корутина отменена, [execute] выбросит [CancellationException].
 */
interface BuildPhase {
    val name: String
    suspend fun execute(context: BuildContext): PhaseResult
}
