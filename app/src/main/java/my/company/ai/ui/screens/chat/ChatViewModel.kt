package my.company.ai.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import my.company.ai.data.model.ChatMessage
import my.company.ai.data.repository.AiRepository

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    @Suppress("unused") savedStateHandle: SavedStateHandle, // projectId будет нужен для контекстных запросов
    private val aiRepository: AiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var nextId: Long = 1L

    fun send(text: String) {
        val userMsg = ChatMessage(
            id = nextId++,
            role = ChatMessage.Role.User,
            content = text,
            timestamp = System.currentTimeMillis(),
        )
        val placeholder = ChatMessage(
            id = nextId++,
            role = ChatMessage.Role.Assistant,
            content = "",
            timestamp = System.currentTimeMillis(),
            isStreaming = true,
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg + placeholder,
            isSending = true,
            error = null,
        )

        viewModelScope.launch {
            val history = _uiState.value.messages.filter { !it.isStreaming }
            aiRepository.chat(history)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = e.message ?: "Ошибка AI",
                        messages = _uiState.value.messages.dropLast(1), // убираем пустой placeholder
                    )
                }
                .onEach { chunk ->
                    val current = _uiState.value.messages
                    val updatedLast = current.last().let {
                        it.copy(content = it.content + chunk.delta, isStreaming = !chunk.isFinal)
                    }
                    _uiState.value = _uiState.value.copy(
                        messages = current.dropLast(1) + updatedLast,
                    )
                }
                .onCompletion {
                    _uiState.value = _uiState.value.copy(isSending = false)
                }
                .collect { /* drained above */ }
        }
    }
}
