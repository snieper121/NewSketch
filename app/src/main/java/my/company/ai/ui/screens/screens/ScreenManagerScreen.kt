package my.company.ai.ui.screens.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.company.ai.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenManagerScreen(
    projectId: String,
    onBack: () -> Unit,
    onEditScreen: (String) -> Unit,
    viewModel: ScreenManagerViewModel = viewModel(factory = ViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Экраны") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showAddDialog) {
                        Icon(Icons.Default.Add, "Добавить")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.screens, key = { it.id }) { screen ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (screen.isLauncher) Icons.Default.Home else Icons.Default.WebAsset,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(screen.name, style = MaterialTheme.typography.titleSmall)
                                    if (screen.isLauncher) {
                                        Text(
                                            "launcher",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(onClick = { onEditScreen(screen.id) }) {
                                    Icon(Icons.Default.Edit, "Edit")
                                }
                                IconButton(onClick = { viewModel.deleteScreen(screen.id) }) {
                                    Icon(Icons.Default.Delete, "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог добавления экрана
    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideAddDialog,
            title = { Text("Новый экран") },
            text = {
                OutlinedTextField(
                    value = state.newScreenName,
                    onValueChange = viewModel::updateNewScreenName,
                    label = { Text("Имя") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::addScreen) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideAddDialog) {
                    Text("Отмена")
                }
            },
        )
    }
}
