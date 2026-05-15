package my.company.ai.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import my.company.ai.data.model.WidgetType
import kotlin.math.roundToInt

/**
 * Canvas в рамке телефона — рендерит виджеты как в реальном приложении.
 *
 * Поддерживает:
 * - Tap на виджет → выделение + BottomPropertyPanel
 * - Long-press на drag handle → перетаскивание
 * - Drop между виджетами → перемещение
 */
@Composable
fun DesignCanvas(
    widgets: List<WidgetItem>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Состояние drag-and-drop
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }
    var widgetPositions by remember { mutableStateOf(mapOf<Int, Float>()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        PhoneCanvas(
            widgets = widgets,
            selectedIndex = selectedIndex,
            draggingIndex = draggingIndex,
            dragOffset = dragOffset,
            dropTargetIndex = dropTargetIndex,
            widgetPositions = widgetPositions,
            onSelect = onSelect,
            onRemoveAt = onRemoveAt,
            onDragStart = { index ->
                draggingIndex = index
                dragOffset = 0f
                dropTargetIndex = null
            },
            onDrag = { offset ->
                dragOffset += offset
                // Определяем drop target на основе позиции
                val currentIdx = draggingIndex ?: return@PhoneCanvas
                val currentPos = widgetPositions[currentIdx] ?: return@PhoneCanvas
                val targetY = currentPos + dragOffset

                // Ищем ближайшую позицию для вставки
                var bestIndex = currentIdx
                var bestDistance = Float.MAX_VALUE
                widgetPositions.forEach { (idx, pos) ->
                    if (idx != currentIdx) {
                        val dist = kotlin.math.abs(targetY - pos)
                        if (dist < bestDistance) {
                            bestDistance = dist
                            bestIndex = if (targetY > pos) idx + 1 else idx
                        }
                    }
                }
                dropTargetIndex = bestIndex.coerceIn(0, widgets.size)
            },
            onDragEnd = {
                val from = draggingIndex
                val to = dropTargetIndex
                if (from != null && to != null && from != to) {
                    val adjustedTo = if (to > from) to - 1 else to
                    onMove(from, adjustedTo)
                }
                draggingIndex = null
                dragOffset = 0f
                dropTargetIndex = null
            },
            onWidgetPosition = { index, y ->
                widgetPositions = widgetPositions + (index to y)
            },
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
    draggingIndex: Int?,
    dragOffset: Float,
    dropTargetIndex: Int?,
    widgetPositions: Map<Int, Float>,
    onSelect: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onDragStart: (Int) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onWidgetPosition: (Int, Float) -> Unit,
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
                    draggingIndex = draggingIndex,
                    dragOffset = dragOffset,
                    dropTargetIndex = dropTargetIndex,
                    onSelect = onSelect,
                    onRemoveAt = onRemoveAt,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onWidgetPosition = onWidgetPosition,
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
 * Контент canvas — рендерит виджеты с drag-and-drop.
 */
@Composable
private fun CanvasContent(
    widgets: List<WidgetItem>,
    selectedIndex: Int?,
    draggingIndex: Int?,
    dragOffset: Float,
    dropTargetIndex: Int?,
    onSelect: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onDragStart: (Int) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onWidgetPosition: (Int, Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        widgets.forEachIndexed { idx, widget ->
            // Drop indicator перед виджетом
            if (dropTargetIndex == idx) {
                DropIndicator()
            }

            // Пропускаем перетаскиваемый виджет (он рендерится отдельно)
            if (draggingIndex != idx) {
                CanvasWidget(
                    widget = widget,
                    isSelected = idx == selectedIndex,
                    onClick = { onSelect(idx) },
                    onRemove = { onRemoveAt(idx) },
                    onDragStart = { onDragStart(idx) },
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onPosition = { y -> onWidgetPosition(idx, y) },
                )
            }
        }

        // Drop indicator в конце
        if (dropTargetIndex == widgets.size) {
            DropIndicator()
        }
    }

    // Перетаскиваемый виджет (поверх остальных)
    if (draggingIndex != null && draggingIndex!! < widgets.size) {
        Box(
            modifier = Modifier
                .offset { IntOffset(0, dragOffset.roundToInt()) }
                .graphicsLayer { alpha = 0.8f }
                .padding(8.dp),
        ) {
            CanvasWidget(
                widget = widgets[draggingIndex!!],
                isSelected = false,
                onClick = {},
                onRemove = {},
                onDragStart = {},
                onDrag = {},
                onDragEnd = {},
                onPosition = {},
            )
        }
    }
}

/**
 * Индикатор места вставки.
 */
@Composable
private fun DropIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
    )
}

/**
 * Рендер виджета на canvas с drag handle.
 */
@Composable
private fun CanvasWidget(
    widget: WidgetItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onPosition: (Float) -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onPosition(coordinates.positionInRoot().y)
            }
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(40.dp)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDrag = { change, offset ->
                            change.consume()
                            onDrag(offset.y)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.DragIndicator,
                contentDescription = "Перетащить",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(20.dp),
            )
        }

        // Содержимое виджета
        Box(modifier = Modifier.weight(1f)) {
            WidgetContent(widget)
        }

        // Кнопка удаления при выделении
        if (isSelected) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
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
