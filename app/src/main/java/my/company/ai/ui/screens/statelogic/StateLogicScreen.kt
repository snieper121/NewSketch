package my.company.ai.ui.screens.statelogic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class StateVar(val name: String, val type: String, val initial: String)
data class EventDef(val name: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateLogicScreen(projectId: String, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Переменные", "События")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Состояние / Логика") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { IconButton(onClick = { /* TODO: add */ }) { Icon(Icons.Default.Add, "Добавить") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, t -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) }) }
            }
            when (selectedTab) {
                0 -> VariablesTab()
                1 -> EventsTab()
            }
        }
    }
}

@Composable
private fun VariablesTab() {
    val vars = remember { mutableStateListOf(StateVar("counter", "Int", "0"), StateVar("userName", "String", "\"\""), StateVar("isLoggedIn", "Boolean", "false")) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(vars) { v ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(v.name, style = MaterialTheme.typography.titleSmall)
                        Text("${v.type} = ${v.initial}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vars.remove(v) }) { Icon(Icons.Default.Delete, "Удалить") }
                }
            }
        }
    }
}

@Composable
private fun EventsTab() {
    val events = remember { mutableStateListOf(EventDef("onIncrement", "counter++"), EventDef("onLogin", "navigate(\"Home\")")) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events) { e ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.titleSmall)
                        Text(e.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { events.remove(e) }) { Icon(Icons.Default.Delete, "Удалить") }
                }
            }
        }
    }
}
