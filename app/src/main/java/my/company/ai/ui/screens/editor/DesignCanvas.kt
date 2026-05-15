package my.company.ai.ui.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * Визуальный канвас: рекурсивно рендерит дерево виджетов.
 *
 * Tap на виджет → выделение + BottomPropertyPanel.
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(widgets, key = { idx, _ -> idx }) { idx, widget ->
            WidgetPreview(
                widget = widget,
                isSelected = idx == selectedIndex,
                onClick = { onSelect(idx) },
                onRemove = { onRemoveAt(idx) },
                depth = 0,
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
                "Откройте меню (☰) → 💠 и выберите виджет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Рекурсивный рендер виджета с поддержкой вложенности.
 *
 * @param depth уровень вложенности (0 = корень)
 */
@Composable
private fun WidgetPreview(
    widget: WidgetItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    depth: Int,
) {
    val shape = RoundedCornerShape(8.dp)
    val baseModifier = Modifier
        .fillMaxWidth()
        .padding(start = (depth * 16).dp) // Отступ для вложенности

    val cardModifier = if (isSelected) {
        baseModifier
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape)
            .clickable { onClick() }
    } else {
        baseModifier.clickable { onClick() }
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            // Заголовок виджета
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
                if (widget.properties.containsKey("text")) {
                    Text(
                        "\"${widget.properties["text"]}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.height(24.dp)) {
                    Icon(Icons.Default.Close, "Удалить", modifier = Modifier.height(16.dp))
                }
            }

            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // Превью содержимого
            Box(Modifier.padding(4.dp)) {
                renderPreview(widget)
            }
        }
    }
}

/**
 * Рекурсивный рендер превью виджета.
 *
 * Контейнеры (Column, Row, Box) рендерят своих children рекурсивно.
 */
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
        // Контейнеры — рекурсивный рендер
        WidgetType.Column -> ContainerPreview("Column", widget.children)
        WidgetType.Row -> ContainerPreview("Row", widget.children)
        WidgetType.Box -> ContainerPreview("Box", widget.children)
    }
}

/**
 * Превью контейнера с рекурсивным рендером children.
 */
@Composable
private fun ContainerPreview(name: String, children: List<WidgetItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(4.dp),
            )
            .padding(8.dp),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (children.isEmpty()) {
            Text(
                "(пусто)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            children.forEach { child ->
                WidgetPreview(
                    widget = child,
                    isSelected = false,
                    onClick = { /* TODO: выбор дочернего виджета */ },
                    onRemove = { /* TODO: удаление дочернего виджета */ },
                    depth = 0, // Вложенные виджеты рендерятся внутри контейнера
                )
            }
        }
    }
}
