package my.company.ai.ui.screens.statelogic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.company.ai.data.repository.ProjectRepository

data class StateVariable(
    val name: String,
    val type: String, // "String", "Int", "Boolean", "Float", "List"
    val initialValue: String,
)

data class EventDefinition(
    val name: String,
    val description: String,
)

data class StateLogicUiState(
    val projectId: String = "",
    val selectedTab: Int = 0, // 0=Variables, 1=Events
    val variables: List<StateVariable> = listOf(
        StateVariable("counter", "Int", "0"),
        StateVariable("userName", "String", "\"\""),
        StateVariable("isLoggedIn", "Boolean", "false"),
    ),
    val events: List<EventDefinition> = listOf(
        EventDefinition("onIncrement", "Увеличить counter на 1"),
        EventDefinition("onLogin", "Установить isLoggedIn = true"),
    ),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddVariableDialog: Boolean = false,
    val showAddEventDialog: Boolean = false,
    val newVarName: String = "",
    val newVarType: String = "String",
    val newVarInitial: String = "",
    val newEventName: String = "",
    val newEventDescription: String = "",
)

class StateLogicViewModel(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val projectId: String = requireNotNull(savedStateHandle["projectId"])

    private val _uiState = MutableStateFlow(StateLogicUiState(projectId = projectId))
    val uiState: StateFlow<StateLogicUiState> = _uiState.asStateFlow()

    init { loadState() }

    private fun loadState() {
        viewModelScope.launch {
            runCatching {
                // TODO: загрузить реальное состояние из JSON модели
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

    // === Variables ===

    fun showAddVariableDialog() {
        _uiState.update {
            it.copy(
                showAddVariableDialog = true,
                newVarName = "",
                newVarType = "String",
                newVarInitial = "",
            )
        }
    }

    fun hideAddVariableDialog() {
        _uiState.update { it.copy(showAddVariableDialog = false) }
    }

    fun updateNewVarName(name: String) {
        _uiState.update { it.copy(newVarName = name) }
    }

    fun updateNewVarType(type: String) {
        _uiState.update { it.copy(newVarType = type) }
    }

    fun updateNewVarInitial(value: String) {
        _uiState.update { it.copy(newVarInitial = value) }
    }

    fun addVariable() {
        val name = _uiState.value.newVarName.trim()
        if (name.isBlank()) return

        val variable = StateVariable(
            name = name,
            type = _uiState.value.newVarType,
            initialValue = _uiState.value.newVarInitial.trim(),
        )
        _uiState.update { st ->
            st.copy(
                variables = st.variables + variable,
                showAddVariableDialog = false,
                newVarName = "",
                newVarInitial = "",
            )
        }
        // TODO: сохранить в JSON модель
    }

    fun removeVariable(name: String) {
        _uiState.update { st ->
            st.copy(variables = st.variables.filter { it.name != name })
        }
        // TODO: удалить из JSON модели
    }

    // === Events ===

    fun showAddEventDialog() {
        _uiState.update {
            it.copy(
                showAddEventDialog = true,
                newEventName = "",
                newEventDescription = "",
            )
        }
    }

    fun hideAddEventDialog() {
        _uiState.update { it.copy(showAddEventDialog = false) }
    }

    fun updateNewEventName(name: String) {
        _uiState.update { it.copy(newEventName = name) }
    }

    fun updateNewEventDescription(description: String) {
        _uiState.update { it.copy(newEventDescription = description) }
    }

    fun addEvent() {
        val name = _uiState.value.newEventName.trim()
        if (name.isBlank()) return

        val event = EventDefinition(
            name = name,
            description = _uiState.value.newEventDescription.trim(),
        )
        _uiState.update { st ->
            st.copy(
                events = st.events + event,
                showAddEventDialog = false,
                newEventName = "",
                newEventDescription = "",
            )
        }
        // TODO: сохранить в JSON модель
    }

    fun removeEvent(name: String) {
        _uiState.update { st ->
            st.copy(events = st.events.filter { it.name != name })
        }
        // TODO: удалить из JSON модели
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
