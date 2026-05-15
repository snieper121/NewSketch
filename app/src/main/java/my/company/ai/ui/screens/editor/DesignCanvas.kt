package my.company.ai.ui.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import my.company.ai.data.model.WidgetType

/**
 * Визуальный канвас: рендерит превью виджетов с поддержкой выбора.
 *
 * Tap на виджет → выделение + открытие PropertyEditorSheet.
 * Кнопка удаления → удаление виджета из списка.
 */
@Composable
fun DesignCanvas(
    widgets: List<WidgetItem>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
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
        itemsIndexed(widgets, key = { idx, _ -> idx }) { idx, widget ->
            WidgetPreview(
                widget = widget,
                isSelected = idx == selectedIndex,
                onClick = { onSelect(idx) },
                onRemove = { onRemoveAt(idx) },
            )
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
private fun WidgetPreview(
    widget: WidgetItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = borderStroke,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    widget.type.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                // Показываем ключевые свойства
                if (widget.properties.containsKey("text")) {
                    Text(
                        "\"${widget.properties["text"]}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Удалить")
                }
            }
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Box(Modifier.padding(4.dp)) { renderPreview(widget) }
        }
    }
}

@Composable
private fun renderPreview(widget: WidgetItem) {
    val text = widget.properties["text"] ?: widget.type.defaultText
    when (widget.type) {
        WidgetType.Text -> Text(text)
        WidgetType.Button -> Button(onClick = {}) { Text(text) }
        WidgetType.TextField -> OutlinedTextField(
            value = widget.properties["value"] ?: "",
            onValueChange = {},
            placeholder = { Text(widget.properties["label"] ?: "Поле ввода") },
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
            Text(text)
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
