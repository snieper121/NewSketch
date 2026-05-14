package my.company.ai.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.company.ai.data.model.Project
import my.company.ai.data.repository.ProjectRepository

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
)

class ProjectsViewModel(
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val localState = MutableStateFlow(
        ProjectsUiState(isLoading = true),
    )

    val uiState: StateFlow<ProjectsUiState> =
        combine(projectRepository.observeProjects(), localState) { projects, local ->
            local.copy(projects = projects, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProjectsUiState(),
        )

    fun openCreateDialog() = localState.update { it.copy(showCreateDialog = true, error = null) }
    fun dismissCreateDialog() = localState.update { it.copy(showCreateDialog = false) }

    fun createProject(name: String, packageName: String, onCreated: (Project) -> Unit) {
        viewModelScope.launch {
            runCatching { projectRepository.createProject(name, packageName) }
                .onSuccess {
                    localState.update { s -> s.copy(showCreateDialog = false, error = null) }
                    onCreated(it)
                }
                .onFailure { e ->
                    localState.update { s -> s.copy(error = e.message ?: "Ошибка создания") }
                }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch { projectRepository.deleteProject(id) }
    }

    fun dismissError() = localState.update { it.copy(error = null) }
}
