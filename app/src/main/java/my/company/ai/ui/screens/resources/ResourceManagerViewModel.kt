package my.company.ai.ui.screens.resources

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.company.ai.data.repository.ProjectRepository

data class ColorItem(
    val name: String,
    val hex: String,
)

data class StringItem(
    val key: String,
    val value: String,
)

data class ResourceManagerUiState(
    val projectId: String = "",
    val selectedTab: Int = 0, // 0=Colors, 1=Strings, 2=Drawables, 3=Fonts
    val colors: List<ColorItem> = listOf(
        ColorItem("primary", "#6750A4"),
        ColorItem("secondary", "#625B71"),
        ColorItem("error", "#B3261E"),
        ColorItem("background", "#FFFBFE"),
    ),
    val strings: List<StringItem> = listOf(
        StringItem("app_name", "My App"),
    ),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddColorDialog: Boolean = false,
    val showAddStringDialog: Boolean = false,
    val newColorName: String = "",
    val newColorHex: String = "",
    val newStringKey: String = "",
    val newStringValue: String = "",
)

class ResourceManagerViewModel(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val projectId: String = requireNotNull(savedStateHandle["projectId"])

    private val _uiState = MutableStateFlow(ResourceManagerUiState(projectId = projectId))
    val uiState: StateFlow<ResourceManagerUiState> = _uiState.asStateFlow()

    init { loadResources() }

    private fun loadResources() {
        viewModelScope.launch {
            runCatching {
                // TODO: загрузить реальные ресурсы из JSON модели
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    // === Colors ===

    fun showAddColorDialog() {
        _uiState.update { it.copy(showAddColorDialog = true, newColorName = "", newColorHex = "") }
    }

    fun hideAddColorDialog() {
        _uiState.update { it.copy(showAddColorDialog = false) }
    }

    fun updateNewColorName(name: String) {
        _uiState.update { it.copy(newColorName = name) }
    }

    fun updateNewColorHex(hex: String) {
        _uiState.update { it.copy(newColorHex = hex) }
    }

    fun addColor() {
        val name = _uiState.value.newColorName.trim()
        val hex = _uiState.value.newColorHex.trim()
        if (name.isBlank() || hex.isBlank()) return

        val color = ColorItem(name = name, hex = if (hex.startsWith("#")) hex else "#$hex")
        _uiState.update { st ->
            st.copy(
                colors = st.colors + color,
                showAddColorDialog = false,
                newColorName = "",
                newColorHex = "",
            )
        }
        // TODO: сохранить в JSON модель
    }

    fun removeColor(name: String) {
        _uiState.update { st ->
            st.copy(colors = st.colors.filter { it.name != name })
        }
        // TODO: удалить из JSON модели
    }

    // === Strings ===

    fun showAddStringDialog() {
        _uiState.update { it.copy(showAddStringDialog = true, newStringKey = "", newStringValue = "") }
    }

    fun hideAddStringDialog() {
        _uiState.update { it.copy(showAddStringDialog = false) }
    }

    fun updateNewStringKey(key: String) {
        _uiState.update { it.copy(newStringKey = key) }
    }

    fun updateNewStringValue(value: String) {
        _uiState.update { it.copy(newStringValue = value) }
    }

    fun addString() {
        val key = _uiState.value.newStringKey.trim()
        val value = _uiState.value.newStringValue.trim()
        if (key.isBlank()) return

        val string = StringItem(key = key, value = value)
        _uiState.update { st ->
            st.copy(
                strings = st.strings + string,
                showAddStringDialog = false,
                newStringKey = "",
                newStringValue = "",
            )
        }
        // TODO: сохранить в JSON модель
    }

    fun removeString(key: String) {
        _uiState.update { st ->
            st.copy(strings = st.strings.filter { it.key != key })
        }
        // TODO: удалить из JSON модели
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
