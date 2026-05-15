package my.company.ai.ui.screens.editor

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Данные API endpoint'а.
 */
data class ApiEndpoint(
    val method: String, // "GET", "POST", "PUT", "DELETE"
    val path: String,
    val description: String = "",
)

/**
 * Данные таблицы БД.
 */
data class DbTable(
    val name: String,
    val fields: List<String> = emptyList(),
)

/**
 * Данные Shared Preference.
 */
data class SharedPref(
    val key: String,
    val type: String, // "String", "Int", "Boolean"
    val defaultValue: String = "",
)

/**
 * Панель управления компонентами (Component tab).
 *
 * Отображает:
 * - API Endpoints (для сетевых запросов)
 * - Database tables (Room entities)
 * - Shared Preferences
 */
@Composable
fun ComponentManagerPanel(
    endpoints: List<ApiEndpoint>,
    tables: List<DbTable>,
    prefs: List<SharedPref>,
    onAddEndpoint: (ApiEndpoint) -> Unit,
    onRemoveEndpoint: (ApiEndpoint) -> Unit,
    onAddTable: (DbTable) -> Unit,
    onRemoveTable: (String) -> Unit,
    onAddPref: (SharedPref) -> Unit,
    onRemovePref: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddEndpoint by remember { mutableStateOf(false) }
    var showAddTable by remember { mutableStateOf(false) }
    var showAddPref by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // === API Endpoints ===
        item {
            SectionHeader(
                icon = { Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary) },
                title = "API Endpoints",
            )
        }
        if (endpoints.isEmpty()) {
            item {
                Text(
                    "Нет endpoints. Нажмите + чтобы добавить.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(endpoints) { endpoint ->
            EndpointCard(
                endpoint = endpoint,
                onRemove = { onRemoveEndpoint(endpoint) },
            )
        }
        item {
            OutlinedButton(onClick = { showAddEndpoint = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить endpoint")
            }
        }

        item { HorizontalDivider() }

        // === Database ===
        item {
            SectionHeader(
                icon = { Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary) },
                title = "Database",
            )
        }
        if (tables.isEmpty()) {
            item {
                Text(
                    "Нет таблиц. Нажмите + чтобы добавить.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(tables, key = { it.name }) { table ->
            TableCard(
                table = table,
                onRemove = { onRemoveTable(table.name) },
            )
        }
        item {
            OutlinedButton(onClick = { showAddTable = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить таблицу")
            }
        }

        item { HorizontalDivider() }

        // === Shared Preferences ===
        item {
            SectionHeader(
                icon = { Icon(Icons.Default.FlashOn, null, tint = MaterialTheme.colorScheme.primary) },
                title = "Shared Preferences",
            )
        }
        if (prefs.isEmpty()) {
            item {
                Text(
                    "Нет preferences. Нажмите + чтобы добавить.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(prefs, key = { it.key }) { pref ->
            PrefCard(
                pref = pref,
                onRemove = { onRemovePref(pref.key) },
            )
        }
        item {
            OutlinedButton(onClick = { showAddPref = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить preference")
            }
        }
    }

    // === Диалоги ===

    if (showAddEndpoint) {
        AddEndpointDialog(
            onDismiss = { showAddEndpoint = false },
            onConfirm = { endpoint ->
                onAddEndpoint(endpoint)
                showAddEndpoint = false
            },
        )
    }

    if (showAddTable) {
        AddTableDialog(
            onDismiss = { showAddTable = false },
            onConfirm = { table ->
                onAddTable(table)
                showAddTable = false
            },
        )
    }

    if (showAddPref) {
        AddPrefDialog(
            onDismiss = { showAddPref = false },
            onConfirm = { pref ->
                onAddPref(pref)
                showAddPref = false
            },
        )
    }
}

@Composable
private fun SectionHeader(
    icon: @Composable () -> Unit,
    title: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EndpointCard(
    endpoint: ApiEndpoint,
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
                Row {
                    Text(
                        endpoint.method,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(endpoint.path, style = MaterialTheme.typography.bodyMedium)
                }
                if (endpoint.description.isNotEmpty()) {
                    Text(
                        endpoint.description,
                        style = MaterialTheme.typography.bodySmall,
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
private fun TableCard(
    table: DbTable,
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
                Text(table.name, style = MaterialTheme.typography.titleSmall)
                if (table.fields.isNotEmpty()) {
                    Text(
                        table.fields.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
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
private fun PrefCard(
    pref: SharedPref,
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
                Text(pref.key, style = MaterialTheme.typography.titleSmall)
                Row {
                    Text(
                        pref.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "= ${pref.defaultValue}",
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
private fun AddEndpointDialog(
    onDismiss: () -> Unit,
    onConfirm: (ApiEndpoint) -> Unit,
) {
    var method by remember { mutableStateOf("GET") }
    var path by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val methods = listOf("GET", "POST", "PUT", "DELETE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый endpoint") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    methods.forEach { m ->
                        TextButton(onClick = { method = m }) {
                            Text(
                                m,
                                color = if (m == method) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                OutlinedTextField(path, { path = it }, label = { Text("Путь") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Описание") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(ApiEndpoint(method, path.trim(), description.trim())) }) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun AddTableDialog(
    onDismiss: () -> Unit,
    onConfirm: (DbTable) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var fields by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая таблица") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Имя таблицы") }, singleLine = true)
                OutlinedTextField(
                    fields,
                    { fields = it },
                    label = { Text("Поля (через запятую)") },
                    singleLine = true,
                    placeholder = { Text("id:Long, name:String, email:String") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val fieldList = fields.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onConfirm(DbTable(name.trim(), fieldList))
            }) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun AddPrefDialog(
    onDismiss: () -> Unit,
    onConfirm: (SharedPref) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("String") }
    var defaultValue by remember { mutableStateOf("") }
    val types = listOf("String", "Int", "Boolean")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый preference") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(key, { key = it }, label = { Text("Ключ") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.forEach { t ->
                        TextButton(onClick = { type = t }) {
                            Text(
                                t,
                                color = if (t == type) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                OutlinedTextField(defaultValue, { defaultValue = it }, label = { Text("Значение по умолчанию") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(SharedPref(key.trim(), type, defaultValue.trim())) }) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
