package my.company.ai.ui.screens.resources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceManagerScreen(projectId: String, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Цвета", "Строки", "Drawables", "Шрифты")
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ресурсы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Добавить")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(title) },
                    )
                }
            }
            when (selectedTab) {
                0 -> ColorsTab(showAddDialog, { showAddDialog = false })
                1 -> StringsTab(showAddDialog, { showAddDialog = false })
                2 -> DrawablesTab()
                3 -> FontsTab()
            }
        }
    }
}

@Composable
private fun ColorsTab(showAdd: Boolean, onDismiss: () -> Unit) {
    val colors = remember {
        mutableStateListOf(
            "primary" to "#6750A4",
            "secondary" to "#958DA5",
            "error" to "#B3261E",
            "background" to "#FFFBFE",
        )
    }
    if (showAdd) {
        AddColorDialog(
            onDismiss = onDismiss,
            onConfirm = { name, hex ->
                if (name.isNotBlank() && hex.isNotBlank()) {
                    colors += name to hex
                }
            },
        )
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(colors) { (name, hex) ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(parseColor(hex)))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            hex,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { colors.remove(name to hex) }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun StringsTab(showAdd: Boolean, onDismiss: () -> Unit) {
    val strings = remember {
        mutableStateListOf("app_name" to "My App", "greeting" to "Hello!")
    }
    if (showAdd) {
        AddStringDialog(
            onDismiss = onDismiss,
            onConfirm = { key, value ->
                if (key.isNotBlank() && value.isNotBlank()) {
                    strings += key to value
                }
            },
        )
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(strings) { (key, value) ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(key, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { strings.remove(key to value) }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawablesTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Нет drawables. Нажмите + чтобы добавить.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FontsTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Roboto (по умолчанию)", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddColorDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var hex by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый цвет") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true)
                OutlinedTextField(hex, { hex = it }, label = { Text("HEX (#RRGGBB)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), hex.trim()); onDismiss() }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun AddStringDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая строка") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(key, { key = it }, label = { Text("Ключ") }, singleLine = true)
                OutlinedTextField(value, { value = it }, label = { Text("Значение") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(key.trim(), value.trim()); onDismiss() }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private fun parseColor(hex: String): Color {
    val h = hex.removePrefix("#")
    return try {
        Color(("FF$h").toLong(16))
    } catch (_: Exception) {
        Color.Gray
    }
}
