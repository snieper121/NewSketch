package my.company.ai.ui.screens.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import my.company.ai.ui.ViewModelFactory

/**
 * Режим содержимого drawer.
 */
enum class DrawerPanel { Files, Widgets, Navigation }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenBuild: () -> Unit,
    onOpenScreens: () -> Unit,
    onOpenResources: () -> Unit,
    onOpenStateLogic: () -> Unit,
    onOpenProjectSettings: () -> Unit,
    viewModel: EditorViewModel = viewModel(factory = ViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var drawerPanel = remember { androidx.compose.runtime.mutableStateOf(DrawerPanel.Widgets) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                // === Верхняя панель: 3 кнопки переключения ===
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    // Кнопка 1: Дерево файлов
                    IconButton(onClick = { drawerPanel.value = DrawerPanel.Files }) {
                        Icon(
                            Icons.Default.Folder, "Файлы",
                            tint = if (drawerPanel.value == DrawerPanel.Files) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Кнопка 2: Палитра виджетов
                    IconButton(onClick = { drawerPanel.value = DrawerPanel.Widgets }) {
                        Icon(
                            Icons.Default.Widgets, "Виджеты",
                            tint = if (drawerPanel.value == DrawerPanel.Widgets) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Кнопка 3: Навигация
                    IconButton(onClick = { drawerPanel.value = DrawerPanel.Navigation }) {
                        Icon(
                            Icons.Outlined.Tune, "Навигация",
                            tint = if (drawerPanel.value == DrawerPanel.Navigation) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                Spacer(Modifier.height(4.dp))

                // === Содержимое drawer в зависимости от выбранной кнопки ===
                when (drawerPanel.value) {
                    DrawerPanel.Files -> DrawerFilesPanel(state, viewModel, drawerState)
                    DrawerPanel.Widgets -> DrawerWidgetsPanel(viewModel, drawerState)
                    DrawerPanel.Navigation -> DrawerNavigationPanel(
                        state = state,
                        viewModel = viewModel,
                        drawerState = drawerState,
                        onOpenScreens = onOpenScreens,
                        onOpenStateLogic = onOpenStateLogic,
                        onOpenResources = onOpenResources,
                        onOpenBuild = onOpenBuild,
                        onOpenProjectSettings = onOpenProjectSettings,
                        onOpenChat = onOpenChat,
                        onBack = onBack,
                    )
                }
            }
        },
        gesturesEnabled = true,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(state.project?.name ?: "Редактор", maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Меню")
                        }
                    },
                    actions = {
                        if (state.openedFile != null && state.hasUnsavedChanges) {
                            IconButton(onClick = viewModel::saveCurrentFile) {
                                Icon(Icons.Default.Save, "Сохранить")
                            }
                        }
                        if (state.openedFile != null) {
                            IconButton(onClick = viewModel::closeFile) {
                                Icon(Icons.Default.Close, "Закрыть файл")
                            }
                        }
                        IconButton(onClick = onOpenChat) {
                            Icon(Icons.AutoMirrored.Filled.Chat, "AI-чат")
                        }
                    },
                )
            },
            floatingActionButton = {
                if (state.openedFile != null && state.hasUnsavedChanges) {
                    FloatingActionButton(onClick = viewModel::saveCurrentFile) {
                        Icon(Icons.Default.Save, "Сохранить")
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Symbol bar только для код-редактора
                if (state.openedFile != null) {
                    EditorSymbolBar(onInsertSymbol = viewModel::insertTextAtCursor)
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.project == null -> Text("Проект не найден", Modifier.align(Alignment.Center))
                    state.openedFile != null -> CodeEditor(
                        content = state.editorContent,
                        onContentChange = viewModel::onContentChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> DesignCanvas(
                        widgets = state.widgets,
                        selectedIndex = state.selectedWidgetIndex,
                        onSelect = viewModel::selectWidget,
                        onRemoveAt = viewModel::removeWidgetAt,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    // PropertyEditorSheet
    if (state.showPropertyEditor && state.selectedWidget != null) {
        val widget = state.selectedWidget!!
        PropertyEditorSheet(
            widgetType = widget.type.displayName,
            properties = widget.properties,
            onPropertyChange = viewModel::updateWidgetProperty,
            onDismiss = viewModel::deselectWidget,
        )
    }

    // Диалог потери изменений
    if (state.pendingOpenFile != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingOpen,
            title = { Text("Несохранённые изменения") },
            text = { Text("В текущем файле есть несохранённые изменения. Перейти к другому файлу без сохранения?") },
            confirmButton = { TextButton(onClick = viewModel::confirmDiscardAndOpen) { Text("Не сохранять") } },
            dismissButton = { TextButton(onClick = viewModel::cancelPendingOpen) { Text("Отмена") } },
        )
    }
}

// ============================================================
// Drawer: Панель файлов (кнопка 1)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerFilesPanel(
    state: EditorUiState,
    viewModel: EditorViewModel,
    drawerState: DrawerState,
) {
    val scope = rememberCoroutineScope()
    val tree = state.tree
    if (tree != null) {
        FileTreePanel(
            tree = tree,
            openedFile = state.openedFile,
            expandedPaths = state.expandedPaths,
            onToggleExpand = viewModel::toggleExpanded,
            onFileClick = { path ->
                viewModel.requestOpenFile(path)
                scope.launch { drawerState.close() }
            },
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет файлов", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ============================================================
// Drawer: Палитра виджетов (кнопка 2)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerWidgetsPanel(
    viewModel: EditorViewModel,
    drawerState: DrawerState,
) {
    val scope = rememberCoroutineScope()
    WidgetPalette(
        onPick = { type ->
            viewModel.addWidget(type)
            scope.launch { drawerState.close() }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

// ============================================================
// Drawer: Навигация (кнопка 3) — вертикальный список разделов
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerNavigationPanel(
    state: EditorUiState,
    viewModel: EditorViewModel,
    drawerState: DrawerState,
    onOpenScreens: () -> Unit,
    onOpenStateLogic: () -> Unit,
    onOpenResources: () -> Unit,
    onOpenBuild: () -> Unit,
    onOpenProjectSettings: () -> Unit,
    onOpenChat: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Табы редактора
        Text(
            "Редактор",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Widgets, null) },
            label = { Text("View") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.setActiveTab(EditorTab.View)
            },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.DataObject, null) },
            label = { Text("Logic") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.setActiveTab(EditorTab.Logic)
            },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Build, null) },
            label = { Text("Component") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.setActiveTab(EditorTab.Component)
            },
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Навигация по проекту
        Text(
            "Проект",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.AccountTree, null) },
            label = { Text("Экраны") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                onOpenScreens()
            },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.DataObject, null) },
            label = { Text("Состояние и логика") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                onOpenStateLogic()
            },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Image, null) },
            label = { Text("Ресурсы") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                onOpenResources()
            },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Settings, null) },
            label = { Text("Настройки проекта") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                onOpenProjectSettings()
            },
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // Инструменты
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Build, null) },
            label = { Text("Сборка") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                onOpenBuild()
            },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.AutoAwesome, null) },
            label = { Text("AI-чат") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                onOpenChat()
            },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, null) },
            label = { Text("Закрыть проект") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                onBack()
            },
        )
    }
}

// ============================================================
// Код-редактор
// ============================================================

@Composable
private fun CodeEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = content,
        onValueChange = onContentChange,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        maxLines = Int.MAX_VALUE,
    )
}

// ============================================================
// Symbol bar
// ============================================================

@Composable
private fun EditorSymbolBar(onInsertSymbol: (String) -> Unit) {
    val symbols = listOf("/", "*", "\"", "'", ":", "!", "?", "@", "#", "_", "-", "+", "(", ")", "[", "]", "\\", "{", "}", "%")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            symbols.forEach { symbol ->
                TextButton(
                    onClick = { onInsertSymbol(symbol) },
                    modifier = Modifier.sizeIn(minWidth = 32.dp, minHeight = 32.dp),
                    contentPadding = PaddingValues(2.dp),
                ) {
                    Text(symbol, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
