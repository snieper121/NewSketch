package my.company.ai.build

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.BuildProgress
import my.company.ai.build.model.PhaseResult
import timber.log.Timber

/**
 * Оркестратор фаз сборки. Запускает фазы по порядку, публикует прогресс.
 */
class BuildPipeline(private val phases: List<BuildPhase>) {

    private val _progress = MutableSharedFlow<BuildProgress>(extraBufferCapacity = 64)
    val progress: Flow<BuildProgress> = _progress.asSharedFlow()

    private val _logs = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val logs: Flow<String> = _logs.asSharedFlow()

    private val _result = MutableSharedFlow<PhaseResult>(extraBufferCapacity = 1)
    val result: Flow<PhaseResult> = _result.asSharedFlow()

    suspend fun run(context: BuildContext) {
        val total = phases.size
        val startTime = System.currentTimeMillis()

        emitLog("=== Сборка начата ===")
        emitLog("Проект: ${context.packageName}")
        emitLog("Build dir: ${context.buildDir.absolutePath}")

        try {
            executePhases(context, total, startTime)
        } catch (e: CancellationException) {
            emitLog("Сборка отменена. Очистка...")
            cleanup(context)
            _result.emit(PhaseResult.Failure("Отменено пользователем"))
            throw e
        }
    }

    private suspend fun executePhases(context: BuildContext, total: Int, startTime: Long) {
        for ((index, phase) in phases.withIndex()) {
            currentCoroutineContext().ensureActive()

            emitProgress(phase.name, index + 1, total, "Выполнение...")
            emitLog("[${index + 1}/$total] ${phase.name}...")

            val result = try {
                phase.execute(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PhaseResult.Failure("${phase.name}: ${e.message}", e)
            }

            when (result) {
                is PhaseResult.Success -> {
                    emitLog("✓ ${phase.name} завершена")
                }
                is PhaseResult.Failure -> {
                    emitLog("✗ ${phase.name} ОШИБКА: ${result.message}")
                    cleanup(context)
                    _result.emit(result)
                    return
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        emitLog("=== Сборка завершена за ${duration}ms ===")
        _result.emit(PhaseResult.Success)
    }

    /**
     * Удаляет временные файлы сборки.
     */
    private fun cleanup(context: BuildContext) {
        context.buildDir.deleteRecursively()
        Timber.d("Cleaned up build dir: ${context.buildDir.absolutePath}")
    }

    private suspend fun emitProgress(name: String, step: Int, total: Int, message: String) {
        _progress.emit(BuildProgress(name, step, total, message))
    }

    private suspend fun emitLog(message: String) {
        _logs.emit(message)
    }
}
