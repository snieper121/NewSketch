package my.company.ai.data.repository

import kotlinx.coroutines.flow.Flow
import my.company.ai.data.model.ChatMessage
import my.company.ai.data.remote.AiService
import my.company.ai.data.remote.ChatChunk

/**
 * Тонкая обёртка над [AiService]: добавляет system-prompt, подходящий
 * для контекста «помощник в разработке Kotlin/Android».
 */
class AiRepository(private val aiService: AiService) {

    fun chat(history: List<ChatMessage>): Flow<ChatChunk> {
        val messages = buildList {
            add(
                ChatMessage(
                    id = 0L,
                    role = ChatMessage.Role.System,
                    content = SYSTEM_PROMPT,
                    timestamp = 0L,
                ),
            )
            addAll(history)
        }
        return aiService.chat(messages)
    }

    private companion object {
        const val SYSTEM_PROMPT = """
            Ты — ассистент разработчика в мобильной IDE, которая создаёт
            Android-приложения **только на Kotlin** с использованием Jetpack
            Compose и Material 3. Отвечай кратко, примерами кода.
            Не предлагай Java. Если пользователь просит UI — используй Compose.
        """
    }
}
