package my.company.ai.ui.screens.compiler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Статусы сборки для UI.
 */
sealed class BuildStatus {
    data object Idle : BuildStatus()
    data class Running(val phase: String, val progress: Float) : BuildStatus()
    data class Success(val durationMs: Long, val apkFilePath: String?) : BuildStatus()
    data class Error(val message: String) : BuildStatus()
}

/**
 * UI-состояние экрана сборки.
 */
data class BuildUiState(
    val status: BuildStatus = BuildStatus.Idle,
    val logs: List<String> = emptyList(),
)

/**
 * ViewModel для экрана сборки (M0 PoC).
 *
 * Пока — заглушка, имитирующая фазы сборки.
 * После реализации BuildPipeline будет вызывать реальный pipeline.
 */
class BuildViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BuildUiState())
    val uiState: StateFlow<BuildUiState> = _uiState

    private var buildJob: Job? = null

    /**
     * Запуск сборки (M0: hardcoded Hello World).
     */
    fun startBuild(projectId: String) {
        buildJob?.cancel()
        _uiState.value = BuildUiState(status = BuildStatus.Running("Подготовка...", 0f), logs = mutableListOf("Начало сборки проекта: $projectId"))

        buildJob = viewModelScope.launch {
            val phases = listOf(
                "Code generation" to 0.05f,
                "Kotlin compilation" to 0.40f,
                "Resource compilation (aapt2)" to 0.55f,
                "Resource linking (aapt2)" to 0.65f,
                "Java compilation (ecj)" to 0.70f,
                "Dex (d8)" to 0.85f,
                "Merge APK" to 0.90f,
                "Zipalign" to 0.93f,
                "Signing" to 0.97f,
                "Готово" to 1.0f,
            )

            val startTime = System.currentTimeMillis()
            val logs = mutableListOf<String>()

            for ((name, progress) in phases) {
                if (!isActive) break

                delay(300) // имитация работы
                logs += "[$name] ..."
                _uiState.update { it.copy(status = BuildStatus.Running(name, progress), logs = logs.toList()) }
            }

            if (isActive) {
                val duration = System.currentTimeMillis() - startTime
                logs += "Сборка завершена успешно."
                _uiState.update { it.copy(status = BuildStatus.Success(duration, null), logs = logs.toList()) }
            }
        }
    }

    /**
     * Отмена сборки.
     */
    fun cancelBuild() {
        buildJob?.cancel()
        _uiState.update {
            it.copy(
                status = BuildStatus.Error("Сборка отменена пользователем"),
                logs = it.logs + "Сборка отменена.",
            )
        }
    }

    /**
     * Установка APK.
     */
    fun installApk() {
        _uiState.update {
            it.copy(logs = it.logs + "Запрос на установку APK...")
        }
    }
}
