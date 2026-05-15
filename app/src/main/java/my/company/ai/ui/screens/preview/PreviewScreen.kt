package my.company.ai.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.company.ai.data.model.WidgetType
import my.company.ai.ui.ViewModelFactory
import my.company.ai.ui.screens.editor.EditorViewModel
import my.company.ai.ui.screens.editor.WidgetItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(factory = ViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Предпросмотр") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            PhoneFrame {
                if (state.widgets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Нет виджетов для предпросмотра",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.widgets.forEach { widget ->
                            PreviewWidget(widget)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneFrame(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .width(320.dp)
            .height(580.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("9:41", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
            content()
        }
    }
}

@Composable
private fun PreviewWidget(widget: WidgetItem) {
    val text = widget.properties["text"] ?: widget.type.defaultText
    when (widget.type) {
        WidgetType.Text -> Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
        WidgetType.Button -> Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(text) }
        WidgetType.TextField -> OutlinedTextField(
            value = widget.properties["value"] ?: "",
            onValueChange = {},
            label = { Text(widget.properties["label"] ?: "") },
            modifier = Modifier.fillMaxWidth(),
        )
        WidgetType.Image -> Box(
            modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { Text("[ Image ]", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        WidgetType.Icon -> Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        WidgetType.Checkbox -> Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = false, onCheckedChange = {})
            Spacer(Modifier.width(8.dp))
            Text(text)
        }
        WidgetType.Switch -> Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = false, onCheckedChange = {})
            Spacer(Modifier.width(8.dp))
            Text(text)
        }
        WidgetType.Divider -> HorizontalDivider()
        WidgetType.Spacer -> Spacer(Modifier.height(16.dp))
        WidgetType.Card -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) { Text("Card") }
        }
        WidgetType.Column -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (widget.children.isEmpty()) Text("(пусто)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else widget.children.forEach { PreviewWidget(it) }
        }
        WidgetType.Row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (widget.children.isEmpty()) Text("(пусто)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else widget.children.forEach { PreviewWidget(it) }
        }
        WidgetType.Box -> Box(Modifier.fillMaxWidth()) {
            if (widget.children.isEmpty()) Text("(пусто)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Column { widget.children.forEach { PreviewWidget(it) } }
        }
    }
}
