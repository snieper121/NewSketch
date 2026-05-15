package my.company.ai.ui.screens.projectsettings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.company.ai.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSettingsScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: ProjectSettingsViewModel = viewModel(factory = ViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки проекта") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Package name
                    OutlinedTextField(
                        value = state.packageName,
                        onValueChange = viewModel::updatePackageName,
                        label = { Text("Package name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = !isValidPackage(state.packageName),
                        supportingText = {
                            if (!isValidPackage(state.packageName)) {
                                Text(
                                    "Неверный формат (com.example.app)",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )

                    // SDK versions
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.minSdk,
                            onValueChange = viewModel::updateMinSdk,
                            label = { Text("Min SDK") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.targetSdk,
                            onValueChange = viewModel::updateTargetSdk,
                            label = { Text("Target SDK") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }

                    HorizontalDivider()

                    // Permissions
                    Text("Permissions", style = MaterialTheme.typography.titleMedium)
                    state.permissions.forEach { perm ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(perm, style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { viewModel.removePermission(perm) }) {
                                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    OutlinedButton(onClick = viewModel::showAddPermissionDialog) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Добавить permission")
                    }

                    HorizontalDivider()

                    // Dependencies
                    Text("Dependencies", style = MaterialTheme.typography.titleMedium)
                    state.dependencies.forEach { (dep, version) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(dep, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    version,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.removeDependency(dep, version) }) {
                                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    OutlinedButton(onClick = viewModel::showAddDependencyDialog) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Добавить библиотеку")
                    }

                    HorizontalDivider()

                    // Theme
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Primary: #6750A4\nDynamic colors: enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    // Диалог добавления permission
    if (state.showAddPermissionDialog) {
        AddPermissionDialog(
            onDismiss = viewModel::hideAddPermissionDialog,
            onConfirm = { perm ->
                viewModel.addPermission(perm)
                viewModel.hideAddPermissionDialog()
            },
        )
    }

    // Диалог добавления dependency
    if (state.showAddDependencyDialog) {
        AddDependencyDialog(
            onDismiss = viewModel::hideAddDependencyDialog,
            onConfirm = { dep, version ->
                viewModel.addDependency(dep, version)
                viewModel.hideAddDependencyDialog()
            },
        )
    }
}

@Composable
private fun AddPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val suggestions = listOf(
        "INTERNET", "CAMERA", "READ_EXTERNAL_STORAGE",
        "WRITE_EXTERNAL_STORAGE", "ACCESS_FINE_LOCATION",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить Permission") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Permission (без android.permission)") },
                    singleLine = true,
                )
                Text("Быстрый выбор:", style = MaterialTheme.typography.labelSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    suggestions.forEach { s ->
                        FilterChip(
                            selected = false,
                            onClick = { text = s },
                            label = { Text(s) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun AddDependencyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var dep by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить Dependency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    dep,
                    { dep = it },
                    label = { Text("Artifact ID") },
                    singleLine = true,
                )
                OutlinedTextField(
                    version,
                    { version = it },
                    label = { Text("Version (опционально)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(dep.trim(), version.trim()) }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private fun isValidPackage(pkg: String): Boolean {
    return pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+${'$'}"))
}
