package my.company.ai.ui.screens.resources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ресурсы") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { IconButton(onClick = { /* TODO: add resource */ }) { Icon(Icons.Default.Add, "Добавить") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, title -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) }) }
            }
            when (selectedTab) {
                0 -> ColorsTab()
                1 -> StringsTab()
                2 -> DrawablesTab()
                3 -> FontsTab()
            }
        }
    }
}

@Composable
private fun ColorsTab() {
    val colors = remember { mutableStateListOf("primary" to "#6750A4", "secondary" to "#958DA5", "error" to "#B3261E", "background" to "#FFFBFE") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(colors) { (name, hex) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(parseColor(hex)))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    Text(hex, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StringsTab() {
    val strings = remember { mutableStateListOf("app_name" to "My App", "greeting" to "Hello!") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(strings) { (key, value) ->
            Row(Modifier.fillMaxWidth()) {
                Text(key, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun parseColor(hex: String): Color {
    val h = hex.removePrefix("#")
    return try { Color(("FF$h").toLong(16)) } catch (_: Exception) { Color.Gray }
}
