package my.company.ai.ui.screens.projectsettings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSettingsScreen(
    projectId: String,
    onBack: () -> Unit,
) {
    val packageName = remember { mutableStateOf("com.example.myapp") }
    val minSdk = remember { mutableStateOf("26") }
    val targetSdk = remember { mutableStateOf("35") }
    val permissions = remember { mutableStateListOf("INTERNET") }

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
            // Package
            OutlinedTextField(
                value = packageName.value,
                onValueChange = { packageName.value = it },
                label = { Text("Package name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // SDK
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minSdk.value,
                    onValueChange = { minSdk.value = it },
                    label = { Text("Min SDK") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = targetSdk.value,
                    onValueChange = { targetSdk.value = it },
                    label = { Text("Target SDK") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            HorizontalDivider()

            // Permissions
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            permissions.forEach { perm ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(perm, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { permissions.remove(perm) }) {
                        Text("Удалить")
                    }
                }
            }
            OutlinedButton(onClick = { /* TODO: dialog добавления */ }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить permission")
            }

            HorizontalDivider()

            // Dependencies
            Text("Dependencies", style = MaterialTheme.typography.titleMedium)
            Text(
                "androidx.compose.material3\nandroidx.navigation",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { /* TODO */ }) {
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
