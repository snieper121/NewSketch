package my.company.ai.ui.screens.compiler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.company.ai.build.BuildPipeline
import my.company.ai.build.model.BuildContext
import my.company.ai.build.model.BuildProgress
import my.company.ai.build.model.PhaseResult
import my.company.ai.build.model.SampleProject
import my.company.ai.build.toolchain.ToolchainDownloader
import my.company.ai.build.toolchain.ToolchainManager
import java.io.File

sealed class BuildStatus {
    data object Idle : BuildStatus()
    data class Running(val phase: String, val progress: Float) : BuildStatus()
    data class Success(val durationMs: Long, val apkFilePath: String?) : BuildStatus()
    data class Error(val message: String) : BuildStatus()
}

data class BuildUiState(
    val status: BuildStatus = BuildStatus.Idle,
    val logs: List<String> = emptyList(),
    val isToolchainReady: Boolean = false,
    val isDownloadingToolchain: Boolean = false,
    val downloadProgress: Float = 0f,
)

class BuildViewModel(
    private val buildPipeline: BuildPipeline,
    private val toolchainManager: ToolchainManager,
    private val toolchainDownloader: ToolchainDownloader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuildUiState())
    val uiState: StateFlow<BuildUiState> = _uiState.asStateFlow()

    private var buildJob: Job? = null
    private var downloadJob: Job? = null

    init {
        _uiState.update {
            it.copy(isToolchainReady = toolchainManager.isToolchainReady())
        }
    }

    fun startBuild(projectId: String) {
        buildJob?.cancel()

        val buildDir = File(System.getProperty("java.io.tmpdir"), "ai_ide_build_$projectId").apply { mkdirs() }
        val projectDir = File(buildDir, "project").apply { mkdirs() }
        val outputApk = File(buildDir, "app.apk")

        val context = BuildContext(
            projectDir = projectDir,
            buildDir = buildDir,
            outputApk = outputApk,
            packageName = SampleProject.PACKAGE,
            minSdk = SampleProject.MIN_SDK,
            targetSdk = SampleProject.TARGET_SDK,
        )

        _uiState.value = BuildUiState(
            status = BuildStatus.Running("Подготовка...", 0f),
            logs = listOf("Начало сборки проекта: $projectId"),
            isToolchainReady = toolchainManager.isToolchainReady(),
        )

        buildJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val logs = mutableListOf<String>()

            launch {
                buildPipeline.logs.collect { log ->
                    logs += log
                    _uiState.update { it.copy(logs = logs.toList()) }
                }
            }

            launch {
                buildPipeline.progress.collect { progress: BuildProgress ->
                    val progressFloat = if (progress.totalSteps > 0) {
                        progress.step.toFloat() / progress.totalSteps
                    } else 0f
                    _uiState.update {
                        it.copy(status = BuildStatus.Running(progress.phaseName, progressFloat))
                    }
                }
            }

            buildPipeline.run(context)

            buildPipeline.result.collect { result: PhaseResult ->
                val duration = System.currentTimeMillis() - startTime
                when (result) {
                    is PhaseResult.Success -> {
                        _uiState.update {
                            it.copy(
                                status = BuildStatus.Success(duration, outputApk.absolutePath),
                                logs = it.logs + "Сборка завершена успешно за ${duration}ms",
                            )
                        }
                    }
                    is PhaseResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                status = BuildStatus.Error(result.message),
                                logs = it.logs + "ОШИБКА: ${result.message}",
                            )
                        }
                    }
                }
            }
        }
    }

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
     * Загружает недостающие инструменты toolchain.
     */
    fun downloadToolchain() {
        downloadJob?.cancel()
        _uiState.update { it.copy(isDownloadingToolchain = true, downloadProgress = 0f) }

        downloadJob = viewModelScope.launch {
            // TODO: реализовать реальную загрузку через ToolchainDownloader
            // M0: показываем placeholder
            _uiState.update {
                it.copy(
                    isDownloadingToolchain = false,
                    isToolchainReady = false,
                    logs = it.logs + "Загрузка toolchain будет доступна после публикации релиза ai-ide-toolchain",
                )
            }
        }
    }

    /**
     * Устанавливает собранный APK через PackageInstaller.
     */
    fun installApk(context: android.content.Context) {
        val apkPath = (_uiState.value.status as? BuildStatus.Success)?.apkFilePath ?: return
        val apkFile = java.io.File(apkPath)
        if (!apkFile.exists()) {
            _uiState.update { it.copy(logs = it.logs + "APK файл не найден: $apkPath") }
            return
        }
        val installer = my.company.ai.build.ApkInstaller(context)
        val result = installer.install(apkFile)
        _uiState.update {
            it.copy(
                logs = it.logs + if (result.isSuccess) {
                    "APK установка запущена"
                } else {
                    "Ошибка установки: ${result.exceptionOrNull()?.message}"
                },
            )
        }
    }

    override fun onCleared() {
        buildJob?.cancel()
        downloadJob?.cancel()
        super.onCleared()
    }
}
