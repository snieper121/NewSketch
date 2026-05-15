package my.company.ai.ui.screens.projectsettings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSettingsScreen(
    projectId: String,
    onBack: () -> Unit,
) {
    val packageName = remember { mutableStateOf("com.example.myapp") }
    val minSdk = remember { mutableStateOf("29") }
    val targetSdk = remember { mutableStateOf("36") }
    val permissions = remember { mutableStateListOf("INTERNET") }
    val dependencies = remember {
        mutableStateListOf(
            "androidx.compose.material3:material3" to "2024.12.01",
            "androidx.navigation:navigation-compose" to "2.9.0",
        )
    }

    var showAddPerm by remember { mutableStateOf(false) }
    var showAddDep by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = packageName.value,
                onValueChange = { packageName.value = it },
                label = { Text("Package name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = !isValidPackage(packageName.value),
                supportingText = {
                    if (!isValidPackage(packageName.value)) {
                        Text(
                            "Неверный формат (com.example.app)",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minSdk.value,
                    onValueChange = { minSdk.value = it.filter { c -> c.isDigit() } },
                    label = { Text("Min SDK") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = targetSdk.value,
                    onValueChange = { targetSdk.value = it.filter { c -> c.isDigit() } },
                    label = { Text("Target SDK") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            HorizontalDivider()

            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            permissions.forEach { perm ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(perm, style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { permissions.remove(perm) }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            OutlinedButton(onClick = { showAddPerm = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить permission")
            }

            HorizontalDivider()

            Text("Dependencies", style = MaterialTheme.typography.titleMedium)
            dependencies.forEach { (dep, version) ->
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
                    IconButton(onClick = { dependencies.remove(dep to version) }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            OutlinedButton(onClick = { showAddDep = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить библиотеку")
            }

            HorizontalDivider()

            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Text(
                "Primary: #6750A4\nDynamic colors: enabled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showAddPerm) {
        AddPermissionDialog(
            onDismiss = { showAddPerm = false },
            onConfirm = { perm ->
                if (perm.isNotBlank() && !permissions.contains(perm)) {
                    permissions += perm.uppercase()
                }
                showAddPerm = false
            },
        )
    }

    if (showAddDep) {
        AddDependencyDialog(
            onDismiss = { showAddDep = false },
            onConfirm = { dep, version ->
                if (dep.isNotBlank()) {
                    dependencies += dep to version
                }
                showAddDep = false
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
    return pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\$"))
}
