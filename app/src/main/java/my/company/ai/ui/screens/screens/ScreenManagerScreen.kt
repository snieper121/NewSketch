package my.company.ai.ui.screens.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenManagerScreen(projectId: String, onBack: () -> Unit, onEditScreen: (String) -> Unit) {
    val screens = remember { mutableStateListOf("MainScreen", "SettingsScreen") }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Экраны") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Добавить") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(screens) { screen ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (screens.indexOf(screen) == 0) Icons.Default.Home else Icons.Default.WebAsset, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(screen, style = MaterialTheme.typography.titleSmall)
                            if (screens.indexOf(screen) == 0) Text("launcher", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onEditScreen(screen) }) { Icon(Icons.Default.Edit, "Edit") }
                        IconButton(onClick = { if (screens.size > 1) screens.remove(screen) }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Новый экран") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { screens += name; showAdd = false } }) { Text("Создать") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } },
        )
    }
}
