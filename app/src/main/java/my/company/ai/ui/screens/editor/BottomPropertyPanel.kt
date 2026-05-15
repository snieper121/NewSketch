package my.company.ai.ui.screens.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import my.company.ai.data.model.WidgetType

/**
 * Компактная панель свойств внизу экрана.
 *
 * Показывается только в View tab когда виджет выбран.
 * Не показывается в код-редакторе.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomPropertyPanel(
    widget: WidgetItem,
    onPropertyChange: (key: String, value: String) -> Unit,
    onModifierChange: (key: String, value: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            // Заголовок с кнопкой закрытия
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    widget.type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose, modifier = Modifier.height(24.dp)) {
                    Icon(Icons.Default.Close, "Закрыть", modifier = Modifier.height(16.dp))
                }
            }

            HorizontalDivider()

            Spacer(Modifier.height(4.dp))

            // Свойства в зависимости от типа виджета
            when (widget.type) {
                WidgetType.Text -> TextProperties(widget, onPropertyChange)
                WidgetType.Button -> ButtonProperties(widget, onPropertyChange)
                WidgetType.TextField -> TextFieldProperties(widget, onPropertyChange)
                WidgetType.Checkbox -> CheckboxProperties(widget, onPropertyChange)
                else -> GenericProperties(widget, onPropertyChange)
            }

            // Модификаторы (общие для всех)
            Spacer(Modifier.height(4.dp))
            Text("Модификаторы:", style = MaterialTheme.typography.labelSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = widget.modifier["fillMaxWidth"] == "true",
                    onClick = {
                        onModifierChange(
                            "fillMaxWidth",
                            if (widget.modifier["fillMaxWidth"] == "true") "false" else "true",
                        )
                    },
                    label = { Text("fillW", style = MaterialTheme.typography.labelSmall) },
                )
                FilterChip(
                    selected = widget.modifier["fillMaxSize"] == "true",
                    onClick = {
                        onModifierChange(
                            "fillMaxSize",
                            if (widget.modifier["fillMaxSize"] == "true") "false" else "true",
                        )
                    },
                    label = { Text("fillAll", style = MaterialTheme.typography.labelSmall) },
                )
                FilterChip(
                    selected = widget.modifier["padding"]?.isNotEmpty() == true,
                    onClick = { onModifierChange("padding", if (widget.modifier["padding"].isNullOrEmpty()) "8" else "") },
                    label = { Text("pad", style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

// ============================================================
// Свойства для конкретных типов виджетов
// ============================================================

@Composable
private fun TextProperties(widget: WidgetItem, onPropertyChange: (String, String) -> Unit) {
    CompactTextField(
        value = widget.properties["text"].orEmpty(),
        onChange = { onPropertyChange("text", it) },
        label = "Текст",
    )
}

@Composable
private fun ButtonProperties(widget: WidgetItem, onPropertyChange: (String, String) -> Unit) {
    CompactTextField(
        value = widget.properties["text"].orEmpty(),
        onChange = { onPropertyChange("text", it) },
        label = "Текст кнопки",
    )
}

@Composable
private fun TextFieldProperties(widget: WidgetItem, onPropertyChange: (String, String) -> Unit) {
    CompactTextField(
        value = widget.properties["label"].orEmpty(),
        onChange = { onPropertyChange("label", it) },
        label = "Подпись",
    )
}

@Composable
private fun CheckboxProperties(widget: WidgetItem, onPropertyChange: (String, String) -> Unit) {
    CompactTextField(
        value = widget.properties["text"].orEmpty(),
        onChange = { onPropertyChange("text", it) },
        label = "Текст",
    )
}

@Composable
private fun GenericProperties(widget: WidgetItem, onPropertyChange: (String, String) -> Unit) {
    Text(
        "Нет свойств для ${widget.type.displayName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ============================================================
// Компактное текстовое поле
// ============================================================

@Composable
private fun CompactTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        textStyle = MaterialTheme.typography.bodySmall,
        singleLine = true,
    )
}
