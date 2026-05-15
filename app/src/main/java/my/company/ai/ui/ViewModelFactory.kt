package my.company.ai.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import my.company.ai.AiApp
import my.company.ai.di.AppContainer
import my.company.ai.ui.screens.chat.ChatViewModel
import my.company.ai.ui.screens.compiler.BuildViewModel
import my.company.ai.ui.screens.editor.EditorViewModel
import my.company.ai.ui.screens.projects.ProjectsViewModel
import my.company.ai.ui.screens.settings.SettingsViewModel

/**
 * Централизованная фабрика ViewModel'ей.
 *
 * Все экраны получают свои VM через `viewModel(factory = ViewModelFactory)`
 * — это удерживает ViewModel'и зависимыми только от [AppContainer],
 * без знания о DI-каркасе.  Когда граф зависимостей разрастётся, перейдём на Hilt.
 */
val ViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { ProjectsViewModel(container().projectRepository) }
    initializer {
        EditorViewModel(
            savedStateHandle = createSavedStateHandle(),
            projectRepository = container().projectRepository,
        )
    }
    initializer {
        ChatViewModel(
            savedStateHandle = createSavedStateHandle(),
            aiRepository = container().aiRepository,
        )
    }
    initializer { SettingsViewModel(container().settingsRepository) }
    initializer { BuildViewModel() }
}

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as AiApp).container
