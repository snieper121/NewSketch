package my.company.ai.ui.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.company.ai.data.model.Project
import my.company.ai.data.model.ProjectFile
import my.company.ai.data.model.WidgetType
import my.company.ai.data.repository.ProjectRepository

enum class EditorTab { View, Logic, Component }
enum class DrawerMode { Widgets, Files }

/**
 * Элемент виджета на canvas.
 * Гибридный подход: properties для свойств виджета, modifier для модификаторов.
 */
data class WidgetItem(
    val type: WidgetType,
    val properties: MutableMap<String, String> = mutableMapOf(),
    val modifier: MutableMap<String, String> = mutableMapOf(),
)

data class EditorUiState(
    val project: Project? = null,
    val tree: ProjectFile? = null,
    val expandedPaths: Set<String> = setOf(""),
    val openedFile: String? = null,
    val editorContent: String = "",
    val savedContent: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val activeTab: EditorTab = EditorTab.View,
    val drawerMode: DrawerMode = DrawerMode.Widgets,
    val widgets: List<WidgetItem> = emptyList(),
    val selectedWidgetIndex: Int? = null,
    val showPropertyEditor: Boolean = false,
    val pendingOpenFile: String? = null,
    // Logic tab data
    val events: List<EditorEvent> = emptyList(),
    val variables: List<EditorVariable> = emptyList(),
    // Component tab data
    val endpoints: List<ApiEndpoint> = emptyList(),
    val tables: List<DbTable> = emptyList(),
    val prefs: List<SharedPref> = emptyList(),
) {
    val hasUnsavedChanges: Boolean get() = openedFile != null && editorContent != savedContent
    val selectedWidget: WidgetItem? get() = selectedWidgetIndex?.let { widgets.getOrNull(it) }
}

class EditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val projectId: String = requireNotNull(savedStateHandle["projectId"])

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                val project = projectRepository.getProject(projectId) ?: error("Проект не найден")
                val tree = projectRepository.fileTree(projectId)
                Pair(project, tree)
            }.onSuccess { (project, tree) ->
                _uiState.update {
                    it.copy(project = project, tree = tree, isLoading = false, errorMessage = null)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // === Tab Management ===

    fun setActiveTab(tab: EditorTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setDrawerMode(mode: DrawerMode) {
        _uiState.update { it.copy(drawerMode = mode) }
    }

    fun toggleExpanded(path: String) {
        _uiState.update { st ->
            val next = st.expandedPaths.toMutableSet()
            if (!next.add(path)) next.remove(path)
            st.copy(expandedPaths = next)
        }
    }

    fun requestOpenFile(relativePath: String) {
        val st = _uiState.value
        if (st.openedFile == relativePath) return
        if (st.hasUnsavedChanges) {
            _uiState.update { it.copy(pendingOpenFile = relativePath) }
        } else {
            doOpenFile(relativePath)
        }
    }

    fun confirmDiscardAndOpen() {
        val path = _uiState.value.pendingOpenFile ?: return
        _uiState.update { it.copy(pendingOpenFile = null) }
        doOpenFile(path)
    }

    fun cancelPendingOpen() {
        _uiState.update { it.copy(pendingOpenFile = null) }
    }

    private fun doOpenFile(relativePath: String) {
        viewModelScope.launch {
            runCatching { projectRepository.readFile(projectId, relativePath) }
                .onSuccess { text ->
                    _uiState.update {
                        it.copy(openedFile = relativePath, editorContent = text, savedContent = text, errorMessage = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = "Не удалось открыть: ${e.message}") }
                }
        }
    }

    fun onContentChanged(new: String) {
        _uiState.update { it.copy(editorContent = new) }
    }

    fun saveCurrentFile() {
        val st = _uiState.value
        val path = st.openedFile ?: return
        if (!st.hasUnsavedChanges) return
        val content = st.editorContent
        viewModelScope.launch {
            runCatching { projectRepository.writeFile(projectId, path, content) }
                .onSuccess { _uiState.update { it.copy(savedContent = content, errorMessage = null) } }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = "Ошибка: ${e.message}") } }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun closeFile() {
        _uiState.update { it.copy(openedFile = null, editorContent = "", savedContent = "", pendingOpenFile = null) }
    }

    // === Widget Management ===

    fun addWidget(type: WidgetType) {
        val properties = mutableMapOf<String, String>()
        when (type) {
            WidgetType.Text -> properties["text"] = "Текст"
            WidgetType.Button -> properties["text"] = "Кнопка"
            WidgetType.TextField -> {
                properties["label"] = "Поле ввода"
                properties["value"] = ""
            }
            WidgetType.Checkbox -> properties["text"] = "Чекбокс"
            else -> {}
        }
        val widget = WidgetItem(type = type, properties = properties)
        _uiState.update { it.copy(widgets = it.widgets + widget) }
    }

    fun removeWidgetAt(index: Int) {
        _uiState.update { st ->
            val next = st.widgets.toMutableList().apply { if (index in indices) removeAt(index) }
            st.copy(
                widgets = next,
                selectedWidgetIndex = if (st.selectedWidgetIndex == index) null else st.selectedWidgetIndex,
                showPropertyEditor = if (st.selectedWidgetIndex == index) false else st.showPropertyEditor,
            )
        }
    }

    fun selectWidget(index: Int) {
        _uiState.update { it.copy(selectedWidgetIndex = index, showPropertyEditor = true) }
    }

    fun deselectWidget() {
        _uiState.update { it.copy(selectedWidgetIndex = null, showPropertyEditor = false) }
    }

    fun updateWidgetProperty(key: String, value: String) {
        val index = _uiState.value.selectedWidgetIndex ?: return
        _uiState.update { st ->
            val widgets = st.widgets.toMutableList()
            val widget = widgets.getOrNull(index) ?: return@update st
            widget.properties[key] = value
            st.copy(widgets = widgets)
        }
    }

    fun updateWidgetModifier(key: String, value: String) {
        val index = _uiState.value.selectedWidgetIndex ?: return
        _uiState.update { st ->
            val widgets = st.widgets.toMutableList()
            val widget = widgets.getOrNull(index) ?: return@update st
            widget.modifier[key] = value
            st.copy(widgets = widgets)
        }
    }

    fun insertTextAtCursor(text: String) {
        _uiState.update { st -> st.copy(editorContent = st.editorContent + text) }
    }

    // === Logic Tab: Events ===

    fun addEvent(event: EditorEvent) {
        _uiState.update { st -> st.copy(events = st.events + event) }
    }

    fun removeEvent(name: String) {
        _uiState.update { st -> st.copy(events = st.events.filter { it.name != name }) }
    }

    fun addVariable(variable: EditorVariable) {
        _uiState.update { st -> st.copy(variables = st.variables + variable) }
    }

    fun removeVariable(name: String) {
        _uiState.update { st -> st.copy(variables = st.variables.filter { it.name != name }) }
    }

    // === Component Tab: Endpoints ===

    fun addEndpoint(endpoint: ApiEndpoint) {
        _uiState.update { st -> st.copy(endpoints = st.endpoints + endpoint) }
    }

    fun removeEndpoint(endpoint: ApiEndpoint) {
        _uiState.update { st -> st.copy(endpoints = st.endpoints - endpoint) }
    }

    // === Component Tab: Tables ===

    fun addTable(table: DbTable) {
        _uiState.update { st -> st.copy(tables = st.tables + table) }
    }

    fun removeTable(name: String) {
        _uiState.update { st -> st.copy(tables = st.tables.filter { it.name != name }) }
    }

    // === Component Tab: Preferences ===

    fun addPref(pref: SharedPref) {
        _uiState.update { st -> st.copy(prefs = st.prefs + pref) }
    }

    fun removePref(key: String) {
        _uiState.update { st -> st.copy(prefs = st.prefs.filter { it.key != key }) }
    }
}
