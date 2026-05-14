package my.company.ai.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import my.company.ai.data.repository.SettingsRepository

class SettingsViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {

    val snapshot = settings.snapshot

    fun setApiKey(value: String) {
        viewModelScope.launch { settings.setApiKey(value) }
    }

    fun setBaseUrl(value: String) {
        viewModelScope.launch { settings.setBaseUrl(value) }
    }

    fun setModel(value: String) {
        viewModelScope.launch { settings.setModel(value) }
    }
}
