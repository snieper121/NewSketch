package my.company.ai.ui.screens.projectsettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.company.ai.data.repository.ProjectRepository

data class ProjectSettingsUiState(
    val projectId: String = "",
    val packageName: String = "com.example.myapp",
    val minSdk: String = "29",
    val targetSdk: String = "36",
    val permissions: List<String> = listOf("INTERNET"),
    val dependencies: List<Pair<String, String>> = listOf(
        "androidx.compose.material3:material3" to "2024.12.01",
        "androidx.navigation:navigation-compose" to "2.9.0",
    ),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddPermissionDialog: Boolean = false,
    val showAddDependencyDialog: Boolean = false,
)

class ProjectSettingsViewModel(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val projectId: String = requireNotNull(savedStateHandle["projectId"])

    private val _uiState = MutableStateFlow(ProjectSettingsUiState(projectId = projectId))
    val uiState: StateFlow<ProjectSettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            runCatching {
                // TODO: загрузить реальные настройки из JSON модели
                // Пока используем дефолтные значения
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun updatePackageName(value: String) {
        _uiState.update { it.copy(packageName = value) }
    }

    fun updateMinSdk(value: String) {
        _uiState.update { it.copy(minSdk = value.filter { c -> c.isDigit() }) }
    }

    fun updateTargetSdk(value: String) {
        _uiState.update { it.copy(targetSdk = value.filter { c -> c.isDigit() }) }
    }

    fun addPermission(permission: String) {
        val perm = permission.trim().uppercase()
        if (perm.isBlank()) return
        _uiState.update { st ->
            if (perm in st.permissions) return@update st
            st.copy(permissions = st.permissions + perm)
        }
    }

    fun removePermission(permission: String) {
        _uiState.update { it.copy(permissions = it.permissions - permission) }
    }

    fun addDependency(artifact: String, version: String) {
        val dep = artifact.trim()
        if (dep.isBlank()) return
        _uiState.update { st ->
            st.copy(dependencies = st.dependencies + (dep to version.trim()))
        }
    }

    fun removeDependency(artifact: String, version: String) {
        _uiState.update { st ->
            st.copy(dependencies = st.dependencies.filter { it.first != artifact || it.second != version })
        }
    }

    fun showAddPermissionDialog() {
        _uiState.update { it.copy(showAddPermissionDialog = true) }
    }

    fun hideAddPermissionDialog() {
        _uiState.update { it.copy(showAddPermissionDialog = false) }
    }

    fun showAddDependencyDialog() {
        _uiState.update { it.copy(showAddDependencyDialog = true) }
    }

    fun hideAddDependencyDialog() {
        _uiState.update { it.copy(showAddDependencyDialog = false) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            runCatching {
                // TODO: сохранить настройки в JSON модель
            }.onSuccess {
                _uiState.update { it.copy(errorMessage = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = "Ошибка сохранения: ${e.message}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
