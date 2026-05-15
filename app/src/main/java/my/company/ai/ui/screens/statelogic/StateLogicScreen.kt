package my.company.ai.ui.screens.statelogic

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
fun StateLogicScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: StateLogicViewModel = viewModel(factory = ViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf("Переменные", "События")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Состояние и логика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        when (state.selectedTab) {
                            0 -> viewModel.showAddVariableDialog()
                            1 -> viewModel.showAddEventDialog()
                        }
                    }) {
                        Icon(Icons.Default.Add, "Добавить")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = state.selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title) },
                    )
                }
            }

            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                }
                else -> {
                    when (state.selectedTab) {
                        0 -> VariablesTab(
                            variables = state.variables,
                            onRemove = viewModel::removeVariable,
                        )
                        1 -> EventsTab(
                            events = state.events,
                            onRemove = viewModel::removeEvent,
                        )
                    }
                }
            }
        }
    }

    // Диалог добавления переменной
    if (state.showAddVariableDialog) {
        AddVariableDialog(
            name = state.newVarName,
            type = state.newVarType,
            initialValue = state.newVarInitial,
            onNameChange = viewModel::updateNewVarName,
            onTypeChange = viewModel::updateNewVarType,
            onInitialChange = viewModel::updateNewVarInitial,
            onDismiss = viewModel::hideAddVariableDialog,
            onConfirm = viewModel::addVariable,
        )
    }

    // Диалог добавления события
    if (state.showAddEventDialog) {
        AddEventDialog(
            name = state.newEventName,
            description = state.newEventDescription,
            onNameChange = viewModel::updateNewEventName,
            onDescriptionChange = viewModel::updateNewEventDescription,
            onDismiss = viewModel::hideAddEventDialog,
            onConfirm = viewModel::addEvent,
        )
    }
}

@Composable
private fun VariablesTab(
    variables: List<StateVariable>,
    onRemove: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(variables, key = { it.name }) { variable ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                    IconButton(onClick = { onRemove(variable.name) }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsTab(
    events: List<EventDefinition>,
    onRemove: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(events, key = { it.name }) { event ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(event.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            event.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemove(event.name) }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVariableDialog(
    name: String,
    type: String,
    initialValue: String,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onInitialChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val types = listOf("String", "Int", "Boolean", "Float", "List")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить переменную") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Имя переменной") },
                    singleLine = true,
                )

                Text("Тип:", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    types.forEachIndexed { index, t ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, types.size),
                            onClick = { onTypeChange(t) },
                            selected = type == t,
                        ) {
                            Text(t)
                        }
                    }
                }

                OutlinedTextField(
                    value = initialValue,
                    onValueChange = onInitialChange,
                    label = { Text("Начальное значение") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun AddEventDialog(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить событие") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Имя события") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Описание") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
