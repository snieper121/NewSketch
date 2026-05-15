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

enum class DrawerMode { Widgets, Files }

data class EditorUiState(
    val project: Project? = null,
    val tree: ProjectFile? = null,
    val expandedPaths: Set<String> = setOf(""),
    val openedFile: String? = null,
    val editorContent: String = "",
    val savedContent: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val drawerMode: DrawerMode = DrawerMode.Widgets,
    val widgetTypes: List<WidgetType> = emptyList(),
    val pendingOpenFile: String? = null,
) {
    val hasUnsavedChanges: Boolean get() = openedFile != null && editorContent != savedContent
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

    /**
     * Добавляет виджет в список (пока только UI-state, без записи в файл).
     * Полная интеграция с JSON-моделью — в M1.
     */
    fun addWidget(type: WidgetType) {
        _uiState.update { it.copy(widgetTypes = it.widgetTypes + type) }
    }

    fun removeWidgetAt(index: Int) {
        _uiState.update { st ->
            val next = st.widgetTypes.toMutableList().apply { if (index in indices) removeAt(index) }
            st.copy(widgetTypes = next)
        }
    }

    /**
     * Вставляет символ в текущую позицию курсора в редакторе.
     * M0: просто добавляет в конец содержимого.
     */
    fun insertTextAtCursor(text: String) {
        _uiState.update { st ->
            st.copy(editorContent = st.editorContent + text)
        }
    }
}
