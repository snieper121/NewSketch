package my.company.ai.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyEditorSheet(
    widgetType: String,
    properties: Map<String, String>,
    onPropertyChange: (key: String, value: String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("$widgetType — Свойства", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()

            // Common: text
            if ("text" in properties || widgetType in listOf("Text", "Button", "TextField")) {
                OutlinedTextField(
                    value = properties["text"].orEmpty(),
                    onValueChange = { onPropertyChange("text", it) },
                    label = { Text("Text") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Style
            if (widgetType == "Text") {
                var expanded by remember { mutableStateOf(false) }
                val styles = listOf("bodyMedium", "bodyLarge", "headlineSmall", "headlineMedium", "displaySmall")
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = properties["style"].orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Style") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        styles.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = { onPropertyChange("style", s); expanded = false })
                        }
                    }
                }
            }

            // onClick event
            if (widgetType in listOf("Button", "Card")) {
                OutlinedTextField(
                    value = properties["onClick"].orEmpty(),
                    onValueChange = { onPropertyChange("onClick", it) },
                    label = { Text("onClick event") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Label (TextField)
            if (widgetType == "TextField") {
                OutlinedTextField(
                    value = properties["label"].orEmpty(),
                    onValueChange = { onPropertyChange("label", it) },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Modifier section
            Text("Modifier", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = properties["fillMaxWidth"] == "true", onClick = {
                    onPropertyChange("fillMaxWidth", if (properties["fillMaxWidth"] == "true") "false" else "true")
                }, label = { Text("fillMaxWidth") })
                FilterChip(selected = properties["fillMaxSize"] == "true", onClick = {
                    onPropertyChange("fillMaxSize", if (properties["fillMaxSize"] == "true") "false" else "true")
                }, label = { Text("fillMaxSize") })
            }

            // Padding
            OutlinedTextField(
                value = properties["padding"].orEmpty(),
                onValueChange = { onPropertyChange("padding", it) },
                label = { Text("Padding (dp)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
