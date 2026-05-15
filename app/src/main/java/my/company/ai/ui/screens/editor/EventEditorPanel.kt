package my.company.ai.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Данные события.
 */
data class EditorEvent(
    val name: String,
    val actions: List<EditorAction> = emptyList(),
)

/**
 * Действие в событии.
 */
data class EditorAction(
    val type: String, // "setState", "navigate", "showToast", "custom"
    val target: String = "",
    val expression: String = "",
    val destination: String = "",
    val message: String = "",
)

/**
 * Панель редактирования событий (Logic tab).
 *
 * Отображает:
 * - Секцию переменных состояния
 * - Секцию событий с actions
 * - Кнопки добавления/удаления
 */
@Composable
fun EventEditorPanel(
    events: List<EditorEvent>,
    variables: List<EditorVariable>,
    onAddEvent: (EditorEvent) -> Unit,
    onRemoveEvent: (String) -> Unit,
    onAddVariable: (EditorVariable) -> Unit,
    onRemoveVariable: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddEvent by remember { mutableStateOf(false) }
    var showAddVariable by remember { mutableStateOf(false) }
    var showAddAction by remember { mutableStateOf<String?>(null) } // event name

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // === Секция переменных ===
        item {
            Text("Переменные состояния", style = MaterialTheme.typography.titleMedium)
        }
        items(variables, key = { it.name }) { variable ->
            VariableCard(
                variable = variable,
                onRemove = { onRemoveVariable(variable.name) },
            )
        }
        item {
            OutlinedButton(onClick = { showAddVariable = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить переменную")
            }
        }

        item { HorizontalDivider() }

        // === Секция событий ===
        item {
            Text("События", style = MaterialTheme.typography.titleMedium)
        }
        items(events, key = { it.name }) { event ->
            EventCard(
                event = event,
                onRemove = { onRemoveEvent(event.name) },
                onAddAction = { showAddAction = event.name },
            )
        }
        item {
            OutlinedButton(onClick = { showAddEvent = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить событие")
            }
        }
    }

    // === Диалоги ===

    if (showAddEvent) {
        AddEventDialog(
            onDismiss = { showAddEvent = false },
            onConfirm = { name ->
                onAddEvent(EditorEvent(name = name))
                showAddEvent = false
            },
        )
    }

    if (showAddVariable) {
        AddVariableDialog(
            onDismiss = { showAddVariable = false },
            onConfirm = { name, type, initial ->
                onAddVariable(EditorVariable(name = name, type = type, initialValue = initial))
                showAddVariable = false
            },
        )
    }

    if (showAddAction != null) {
        AddActionDialog(
            eventName = showAddAction!!,
            onDismiss = { showAddAction = null },
            onConfirm = { action ->
                // TODO: добавить action к событию через ViewModel
                showAddAction = null
            },
        )
    }
}

@Composable
private fun VariableCard(
    variable: EditorVariable,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(variable.name, style = MaterialTheme.typography.titleSmall)
                Row {
                    Text(
                        variable.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "= ${variable.initialValue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EventCard(
    event: EditorEvent,
    onRemove: () -> Unit,
    onAddAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    event.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (event.actions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                event.actions.forEach { action ->
                    ActionRow(action)
                }
            }

            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onAddAction) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить действие")
            }
        }
    }
}

@Composable
private fun ActionRow(action: EditorAction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            "→ ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            when (action.type) {
                "setState" -> "setState: ${action.target} = ${action.expression}"
                "navigate" -> "navigate: ${action.destination}"
                "showToast" -> "showToast: \"${action.message}\""
                "custom" -> "custom: ${action.expression}"
                else -> action.type
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое событие") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя события") },
                singleLine = true,
                placeholder = { Text("onClick_btn_1") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVariableDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("String") }
    var initial by remember { mutableStateOf("") }
    val types = listOf("String", "Int", "Boolean", "Float", "List")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая переменная") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                )
                Text("Тип:", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    types.forEachIndexed { i, t ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(i, types.size),
                            onClick = { type = t },
                            selected = type == t,
                        ) { Text(t) }
                    }
                }
                OutlinedTextField(
                    value = initial,
                    onValueChange = { initial = it },
                    label = { Text("Начальное значение") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), type, initial.trim()) }) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddActionDialog(
    eventName: String,
    onDismiss: () -> Unit,
    onConfirm: (EditorAction) -> Unit,
) {
    var type by remember { mutableStateOf("setState") }
    var target by remember { mutableStateOf("") }
    var expression by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val types = listOf("setState", "navigate", "showToast")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Действие для $eventName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Тип:", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    types.forEachIndexed { i, t ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(i, types.size),
                            onClick = { type = t },
                            selected = type == t,
                        ) { Text(t) }
                    }
                }
                when (type) {
                    "setState" -> {
                        OutlinedTextField(target, { target = it }, label = { Text("Переменная") }, singleLine = true)
                        OutlinedTextField(expression, { expression = it }, label = { Text("Выражение") }, singleLine = true)
                    }
                    "navigate" -> {
                        OutlinedTextField(destination, { destination = it }, label = { Text("Экран назначения") }, singleLine = true)
                    }
                    "showToast" -> {
                        OutlinedTextField(message, { message = it }, label = { Text("Сообщение") }, singleLine = true)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val action = when (type) {
                    "setState" -> EditorAction(type = "setState", target = target, expression = expression)
                    "navigate" -> EditorAction(type = "navigate", destination = destination)
                    "showToast" -> EditorAction(type = "showToast", message = message)
                    else -> EditorAction(type = type)
                }
                onConfirm(action)
            }) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

data class EditorVariable(
    val name: String,
    val type: String,
    val initialValue: String,
)
