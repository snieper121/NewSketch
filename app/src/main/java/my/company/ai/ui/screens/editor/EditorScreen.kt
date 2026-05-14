package my.company.ai.ui.screens.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Widgets
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
    viewModel: EditorViewModel = viewModel(factory = ViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Показ ошибок через Snackbar.
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
                // Переключатель Folder / Widgets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                ) {
                    IconButton(
                        onClick = { viewModel.setDrawerMode(DrawerMode.Files) },
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "Файлы",
                            tint = if (state.drawerMode == DrawerMode.Files)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setDrawerMode(DrawerMode.Widgets) },
                    ) {
                        Icon(
                            Icons.Default.Widgets,
                            contentDescription = "Виджеты",
                            tint = if (state.drawerMode == DrawerMode.Widgets)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Нет файлов")
                            }
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
                    title = {
                        Text(
                            state.project?.name ?: "Редактор",
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            },
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    actions = {
                        if (state.openedFile != null && state.hasUnsavedChanges) {
                            IconButton(onClick = viewModel::saveCurrentFile) {
                                Icon(Icons.Default.Save, contentDescription = "Сохранить")
                            }
                        }
                        if (state.openedFile != null) {
                            IconButton(onClick = viewModel::closeFile) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть файл")
                            }
                        }
                        IconButton(onClick = onOpenChat) {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "AI-чат",
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                if (state.openedFile != null && state.hasUnsavedChanges) {
                    FloatingActionButton(onClick = viewModel::saveCurrentFile) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить")
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.project == null -> Text(
                        "Проект не найден",
                        modifier = Modifier.align(Alignment.Center),
                    )
                    state.openedFile != null -> CodeEditor(
                        content = state.editorContent,
                        onContentChange = viewModel::onContentChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> DesignCanvas(
                        widgets = state.widgetTypes,
                        onRemoveAt = viewModel::removeWidgetAt,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    // Диалог подтверждения потери несохранённых изменений.
    if (state.pendingOpenFile != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingOpen,
            title = { Text("Несохранённые изменения") },
            text = {
                Text(
                    "В текущем файле есть несохранённые изменения. " +
                        "Перейти к другому файлу без сохранения?",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDiscardAndOpen) {
                    Text("Не сохранять")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPendingOpen) {
                    Text("Отмена")
                }
            },
        )
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
