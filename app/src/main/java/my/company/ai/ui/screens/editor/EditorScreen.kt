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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IconButton(onClick = { viewModel.setDrawerMode(DrawerMode.Files) }) {
                        Icon(
                            Icons.Default.Folder, "Файлы",
                            tint = if (state.drawerMode == DrawerMode.Files) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { viewModel.setDrawerMode(DrawerMode.Widgets) }) {
                        Icon(
                            Icons.Default.Widgets, "Виджеты",
                            tint = if (state.drawerMode == DrawerMode.Widgets) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = { scope.launch { drawerState.close() }; onOpenChat() }) {
                        Icon(Icons.Outlined.AutoAwesome, "AI")
                    }
                    IconButton(onClick = { scope.launch { drawerState.close() }; onOpenScreens() }) {
                        Icon(Icons.Outlined.AccountTree, "Экраны")
                    }
                    IconButton(onClick = { scope.launch { drawerState.close() }; onOpenStateLogic() }) {
                        Icon(Icons.Outlined.DataObject, "Состояние")
                    }
                    IconButton(onClick = { scope.launch { drawerState.close() }; onOpenResources() }) {
                        Icon(Icons.Outlined.Image, "Ресурсы")
                    }
                    IconButton(onClick = { scope.launch { drawerState.close() }; onOpenBuild() }) {
                        Icon(Icons.Outlined.Build, "Сборка")
                    }
                    IconButton(onClick = { scope.launch { drawerState.close() }; onOpenProjectSettings() }) {
                        Icon(Icons.Outlined.Settings, "Настройки")
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                when (state.drawerMode) {
                    DrawerMode.Files -> {
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
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Нет файлов") }
                        }
                    }
                    DrawerMode.Widgets -> {
                        WidgetPalette(
                            onPick = { type ->
                                viewModel.addWidget(type)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
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
                if (state.activeTab == EditorTab.View && state.openedFile != null && state.hasUnsavedChanges) {
                    FloatingActionButton(onClick = viewModel::saveCurrentFile) {
                        Icon(Icons.Default.Save, "Сохранить")
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    if (state.activeTab == EditorTab.View && state.openedFile != null) {
                        EditorSymbolBar(onInsertSymbol = viewModel::insertTextAtCursor)
                    }
                    BottomAppBar {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            EditorTabItem(
                                label = "View",
                                icon = Icons.Default.Widgets,
                                selected = state.activeTab == EditorTab.View,
                                onClick = { viewModel.setActiveTab(EditorTab.View) },
                            )
                            EditorTabItem(
                                label = "Logic",
                                icon = Icons.Outlined.DataObject,
                                selected = state.activeTab == EditorTab.Logic,
                                onClick = { viewModel.setActiveTab(EditorTab.Logic) },
                            )
                            EditorTabItem(
                                label = "Component",
                                icon = Icons.Outlined.Build,
                                selected = state.activeTab == EditorTab.Component,
                                onClick = { viewModel.setActiveTab(EditorTab.Component) },
                            )
                        }
                    }
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
                    else -> {
                        when (state.activeTab) {
                            EditorTab.View -> ViewTabContent(state, viewModel)
                            EditorTab.Logic -> LogicTabContent(state, viewModel)
                            EditorTab.Component -> ComponentTabContent(state, viewModel)
                        }
                    }
                }
            }
        }
    }

    if (state.showPropertyEditor && state.selectedWidget != null) {
        val widget = state.selectedWidget!!
        PropertyEditorSheet(
            widgetType = widget.type.displayName,
            properties = widget.properties,
            onPropertyChange = viewModel::updateWidgetProperty,
            onDismiss = viewModel::deselectWidget,
        )
    }

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

@Composable
private fun ViewTabContent(state: EditorUiState, viewModel: EditorViewModel) {
    if (state.openedFile != null) {
        CodeEditor(
            content = state.editorContent,
            onContentChange = viewModel::onContentChanged,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        DesignCanvas(
            widgets = state.widgets,
            selectedIndex = state.selectedWidgetIndex,
            onSelect = viewModel::selectWidget,
            onRemoveAt = viewModel::removeWidgetAt,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun LogicTabContent(state: EditorUiState, viewModel: EditorViewModel) {
    EventEditorPanel(
        events = state.events,
        variables = state.variables,
        onAddEvent = viewModel::addEvent,
        onRemoveEvent = viewModel::removeEvent,
        onAddVariable = viewModel::addVariable,
        onRemoveVariable = viewModel::removeVariable,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ComponentTabContent(state: EditorUiState, viewModel: EditorViewModel) {
    ComponentManagerPanel(
        endpoints = state.endpoints,
        tables = state.tables,
        prefs = state.prefs,
        onAddEndpoint = viewModel::addEndpoint,
        onRemoveEndpoint = viewModel::removeEndpoint,
        onAddTable = viewModel::addTable,
        onRemoveTable = viewModel::removeTable,
        onAddPref = viewModel::addPref,
        onRemovePref = viewModel::removePref,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun EditorTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
