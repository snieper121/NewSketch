package my.company.ai.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import my.company.ai.data.model.WidgetType

/**
 * Визуальный канвас: рендерит превью виджетов, которые **уже записаны**
 * в `MainActivity.kt`.
 *
 * Каждая карточка показывает, какой Compose-код сгенерирован, и позволяет
 * удалить этот виджет (удаление перегенерирует файл без него).
 */
@Composable
fun DesignCanvas(
    widgets: List<WidgetType>,
    onRemoveAt: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (widgets.isEmpty()) {
        EmptyCanvas(modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(widgets, key = { idx, _ -> idx }) { idx, type ->
            WidgetPreview(type = type, onRemove = { onRemoveAt(idx) })
        }
    }
}

@Composable
private fun EmptyCanvas(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Откройте меню (гамбургер слева) и выберите виджет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WidgetPreview(type: WidgetType, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    type.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Удалить")
                }
            }
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Box(Modifier.padding(4.dp)) { renderPreview(type) }
        }
    }
}

@Composable
private fun renderPreview(type: WidgetType) {
    when (type) {
        WidgetType.Text -> Text("Текст")
        WidgetType.Button -> Button(onClick = {}) { Text("Кнопка") }
        WidgetType.TextField -> OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Поле ввода") },
            modifier = Modifier.fillMaxWidth(),
        )
        WidgetType.Image -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            contentAlignment = Alignment.Center,
        ) { Text("[ image ]", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        WidgetType.Icon -> Icon(Icons.Default.Widgets, contentDescription = null)
        WidgetType.Checkbox -> Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = false, onCheckedChange = {})
            Text("Чекбокс")
        }
        WidgetType.Switch -> Switch(checked = false, onCheckedChange = {})
        WidgetType.Divider -> HorizontalDivider()
        WidgetType.Spacer -> Spacer(Modifier.height(16.dp))
        WidgetType.Card -> Card(Modifier.fillMaxWidth()) {
            Text("Карточка", Modifier.padding(16.dp))
        }
        WidgetType.Column -> Column(Modifier.fillMaxWidth()) {
            Text("Column", style = MaterialTheme.typography.labelSmall)
        }
        WidgetType.Row -> Row(Modifier.fillMaxWidth()) {
            Text("Row", style = MaterialTheme.typography.labelSmall)
        }
        WidgetType.Box -> Box(Modifier.fillMaxWidth()) {
            Text("Box", style = MaterialTheme.typography.labelSmall)
        }
    }
}
