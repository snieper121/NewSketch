package my.company.ai.ui.screens.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import my.company.ai.data.model.WidgetType

/**
 * Палитра виджетов. На этапе каркаса drag&drop из drawer на канвас
 * не поддерживается платформой (drawer — модальное окно), поэтому клик
 * по строке сразу добавляет виджет в конец списка на канвасе.
 * Реальный D&D перейдёт сюда, когда канвас и drawer окажутся в одном слое
 * (режим «expanded» на планшете) — см. план C.
 */
@Composable
fun WidgetPalette(
    onPick: (WidgetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
    ) {
        items(WidgetType.entries, key = { it.name }) { type ->
            WidgetRow(type = type, onClick = { onPick(type) })
        }
    }
}

@Composable
private fun WidgetRow(type: WidgetType, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = iconFor(type),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.fillMaxWidth()) {
                Text(type.displayName, style = MaterialTheme.typography.bodyLarge)
                if (type.isContainer) {
                    Text(
                        "Контейнер",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun iconFor(type: WidgetType): ImageVector = when (type) {
    WidgetType.Text -> Icons.Default.Label
    WidgetType.Button -> Icons.Default.SmartButton
    WidgetType.TextField -> Icons.Default.TextFields
    WidgetType.Image -> Icons.Default.Image
    WidgetType.Icon -> Icons.Default.Star
    WidgetType.Checkbox -> Icons.Default.CheckBox
    WidgetType.Switch -> Icons.Default.ToggleOn
    WidgetType.Divider -> Icons.Default.HorizontalRule
    WidgetType.Spacer -> Icons.Default.SpaceBar
    WidgetType.Card -> Icons.Default.Dashboard
    WidgetType.Column -> Icons.Default.ViewAgenda
    WidgetType.Row -> Icons.Default.ViewColumn
    WidgetType.Box -> Icons.Default.CropSquare
}

@Composable
@Suppress("unused")
private fun AddHint() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AddBox, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Тапните, чтобы добавить на канвас")
    }
}
