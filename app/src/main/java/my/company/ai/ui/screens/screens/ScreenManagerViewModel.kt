package my.company.ai.ui.screens.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.company.ai.data.model.ScreenModel
import my.company.ai.data.model.WidgetNode
import my.company.ai.data.repository.ProjectRepository

data class ScreenItem(
    val id: String,
    val name: String,
    val isLauncher: Boolean = false,
)

data class ScreenManagerUiState(
    val projectId: String = "",
    val screens: List<ScreenItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddDialog: Boolean = false,
    val newScreenName: String = "",
)

class ScreenManagerViewModel(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val projectId: String = requireNotNull(savedStateHandle["projectId"])

    private val _uiState = MutableStateFlow(ScreenManagerUiState(projectId = projectId))
    val uiState: StateFlow<ScreenManagerUiState> = _uiState.asStateFlow()

    init { loadScreens() }

    private fun loadScreens() {
        viewModelScope.launch {
            runCatching {
                // TODO: загрузить реальные экраны из JSON модели
                // Пока используем заглушку
                listOf(
                    ScreenItem(id = "main", name = "MainScreen", isLauncher = true),
                    ScreenItem(id = "settings", name = "SettingsScreen"),
                )
            }.onSuccess { screens ->
                _uiState.update { it.copy(screens = screens, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, newScreenName = "") }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun updateNewScreenName(name: String) {
        _uiState.update { it.copy(newScreenName = name) }
    }

    fun addScreen() {
        val name = _uiState.value.newScreenName.trim()
        if (name.isBlank()) return

        val id = name.lowercase().replace(" ", "_")
        val newScreen = ScreenItem(id = id, name = name)

        _uiState.update { st ->
            st.copy(
                screens = st.screens + newScreen,
                showAddDialog = false,
                newScreenName = "",
            )
        }

        // TODO: сохранить в JSON модель
    }

    fun deleteScreen(screenId: String) {
        _uiState.update { st ->
            st.copy(screens = st.screens.filter { it.id != screenId })
        }
        // TODO: удалить из JSON модели
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
