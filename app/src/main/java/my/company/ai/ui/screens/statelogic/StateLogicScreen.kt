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
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Состояние / Логика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Add, "Добавить")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, t ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(t) },
                    )
                }
            }
            when (selectedTab) {
                0 -> VariablesTab(showAdd, { showAdd = false })
                1 -> EventsTab(showAdd, { showAdd = false })
            }
        }
    }
}

@Composable
private fun VariablesTab(showAdd: Boolean, onDismiss: () -> Unit) {
    val vars = remember {
        mutableStateListOf(
            StateVar("counter", "Int", "0"),
            StateVar("userName", "String", "\"\""),
            StateVar("isLoggedIn", "Boolean", "false"),
        )
    }
    if (showAdd) {
        AddVariableDialog(
            onDismiss = onDismiss,
            onConfirm = { name, type, initial ->
                if (name.isNotBlank() && type.isNotBlank()) {
                    vars += StateVar(name, type, initial)
                }
            },
        )
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(vars) { v ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(v.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${v.type} = ${v.initial}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vars.remove(v) }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsTab(showAdd: Boolean, onDismiss: () -> Unit) {
    val events = remember {
        mutableStateListOf(
            EventDef("onIncrement", "counter++"),
            EventDef("onLogin", "navigate(\"Home\")"),
        )
    }
    if (showAdd) {
        AddEventDialog(
            onDismiss = onDismiss,
            onConfirm = { name, desc ->
                if (name.isNotBlank()) {
                    events += EventDef(name, desc)
                }
            },
        )
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(events) { e ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            e.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { events.remove(e) }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddVariableDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("String") }
    var initial by remember { mutableStateOf("") }
    val types = listOf("String", "Int", "Boolean", "Float", "List")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая переменная") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true)
                SingleChoiceSegmentedButtonRow {
                    types.forEach { t ->
                        SegmentedButton(
                            selected = type == t,
                            onClick = { type = t },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = types.indexOf(t),
                                count = types.size,
                            ),
                        ) {
                            Text(t)
                        }
                    }
                }
                OutlinedTextField(initial, { initial = it }, label = { Text("Начальное значение") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), type, initial); onDismiss() }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun AddEventDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое событие") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true)
                OutlinedTextField(desc, { desc = it }, label = { Text("Действие / описание") }, singleLine = false, maxLines = 3)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), desc.trim()); onDismiss() }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
