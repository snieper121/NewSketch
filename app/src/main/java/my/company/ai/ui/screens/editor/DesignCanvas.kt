package my.company.ai.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import my.company.ai.data.model.WidgetType

/**
 * Canvas в рамке телефона — рендерит виджеты как в реальном приложении.
 *
 * Рамка занимает почти весь экран редактора с отступами 12dp.
 * Tap на виджет → выделение + BottomPropertyPanel.
 */
@Composable
fun DesignCanvas(
    widgets: List<WidgetItem>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        PhoneCanvas(
            widgets = widgets,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            onRemoveAt = onRemoveAt,
        )
    }
}

/**
 * Рамка телефона с canvas внутри.
 */
@Composable
private fun PhoneCanvas(
    widgets: List<WidgetItem>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .border(4.dp, MaterialTheme.colorScheme.outline, shape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "9:41",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Контент
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
        ) {
            if (widgets.isEmpty()) {
                EmptyCanvas()
            } else {
                CanvasContent(
                    widgets = widgets,
                    selectedIndex = selectedIndex,
                    onSelect = onSelect,
                    onRemoveAt = onRemoveAt,
                )
            }
        }
    }
}

/**
 * Пустое состояние canvas.
 */
@Composable
private fun EmptyCanvas() {
    Box(
        modifier = Modifier
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
 * Контент canvas — рендерит виджеты как в реальном приложении.
 */
@Composable
private fun CanvasContent(
    widgets: List<WidgetItem>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        widgets.forEachIndexed { idx, widget ->
            CanvasWidget(
                widget = widget,
                isSelected = idx == selectedIndex,
                onClick = { onSelect(idx) },
                onRemove = { onRemoveAt(idx) },
            )
        }
    }
}

/**
 * Рендер виджета на canvas — как в реальном приложении.
 *
 * Tap → выделение (рамка + кнопка удаления).
 * Виджет рендерится как реальный Compose элемент.
 */
@Composable
private fun CanvasWidget(
    widget: WidgetItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    val baseModifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }

    val widgetModifier = if (isSelected) {
        baseModifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
    } else {
        baseModifier
    }

    Box(modifier = widgetModifier) {
        // Рендер виджета
        WidgetContent(widget)

        // Кнопка удаления при выделении
        if (isSelected) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .height(24.dp)
                    .width(24.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    "Удалить",
                    modifier = Modifier.height(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Рекурсивный рендер содержимого виджета.
 *
 * Виджеты рендерятся как реальные Compose элементы:
 * - Text → Text
 * - Button → Button
 * - TextField → OutlinedTextField
 * - Column → Column (вертикальный список)
 * - Row → Row (горизонтальный список)
 * - Box → Box (наложение)
 */
@Composable
private fun WidgetContent(widget: WidgetItem) {
    val text = widget.properties["text"] ?: widget.type.defaultText
    when (widget.type) {
        WidgetType.Text -> Text(
            text,
            modifier = Modifier.padding(8.dp),
        )
        WidgetType.Button -> Button(
            onClick = {},
            modifier = Modifier.padding(8.dp),
        ) { Text(text) }
        WidgetType.TextField -> OutlinedTextField(
            value = widget.properties["value"] ?: "",
            onValueChange = {},
            label = { Text(widget.properties["label"] ?: "") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        )
        WidgetType.Image -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { Text("[ Image ]", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        WidgetType.Icon -> Icon(
            Icons.Default.Widgets,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
        )
        WidgetType.Checkbox -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            Checkbox(checked = false, onCheckedChange = {})
            Spacer(Modifier.width(8.dp))
            Text(text)
        }
        WidgetType.Switch -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            Switch(checked = false, onCheckedChange = {})
            Spacer(Modifier.width(8.dp))
            Text(text)
        }
        WidgetType.Divider -> HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        WidgetType.Spacer -> Spacer(Modifier.height(16.dp))
        WidgetType.Card -> Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(Modifier.padding(16.dp)) {
                if (widget.children.isEmpty()) {
                    Text("Card", style = MaterialTheme.typography.bodySmall)
                } else {
                    widget.children.forEach { child -> WidgetContent(child) }
                }
            }
        }
        // Контейнеры — реальный layout
        WidgetType.Column -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (widget.children.isEmpty()) {
                Text(
                    "(пустой Column)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            } else {
                widget.children.forEach { child -> WidgetContent(child) }
            }
        }
        WidgetType.Row -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (widget.children.isEmpty()) {
                Text(
                    "(пустой Row)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            } else {
                widget.children.forEach { child -> WidgetContent(child) }
            }
        }
        WidgetType.Box -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
        ) {
            if (widget.children.isEmpty()) {
                Text(
                    "(пустой Box)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            } else {
                widget.children.forEach { child -> WidgetContent(child) }
            }
        }
    }
}
