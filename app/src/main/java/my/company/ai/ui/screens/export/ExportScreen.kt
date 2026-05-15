package my.company.ai.ui.screens.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.company.ai.ui.ViewModelFactory
import my.company.ai.ui.screens.compiler.BuildStatus
import my.company.ai.ui.screens.compiler.BuildViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    projectId: String,
    onBack: () -> Unit,
    onBuild: () -> Unit,
    viewModel: BuildViewModel = viewModel(factory = ViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Экспорт") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Заголовок
            Text("Экспорт проекта", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Соберите APK или экспортируйте проект в Gradle формат",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            // Сборка APK
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Сборка APK", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Собрать APK файл для установки на устройство",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))

                    // Статус сборки
                    when (state) {
                        is BuildStatus.Idle -> {
                            Button(onClick = onBuild, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Build, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Собрать APK")
                            }
                        }
                        is BuildStatus.Running -> {
                            val running = state as BuildStatus.Running
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.height(24.dp).width(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("${running.phase}... ${(running.progress * 100).toInt()}%")
                            }
                        }
                        is BuildStatus.Success -> {
                            val success = state as BuildStatus.Success
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Готово! ${success.durationMs}ms")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Share, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Поделиться APK")
                            }
                        }
                        is BuildStatus.Error -> {
                            val error = state as BuildStatus.Error
                            Text(
                                "Ошибка: ${error.message}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onBuild, modifier = Modifier.fillMaxWidth()) {
                                Text("Повторить")
                            }
                        }
                    }
                }
            }

            // Экспорт в Gradle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Экспорт в Gradle", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Экспортировать проект как стандартный Gradle проект для Android Studio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Экспорт (скоро)")
                    }
                }
            }
        }
    }
}
