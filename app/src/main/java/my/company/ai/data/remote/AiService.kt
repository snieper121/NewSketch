package my.company.ai.data.remote

import kotlinx.coroutines.flow.Flow
import my.company.ai.data.model.ChatMessage

/**
 * Абстракция над провайдером LLM (ADR-008).
 *
 * Реализации — [OpenAiCompatibleService] и потенциально другие
 * (локальные сервера, альтернативные API).  ViewModel зависит только
 * от интерфейса, что позволяет легко подменять провайдера.
 */
interface AiService {

    /**
     * Отправляет историю диалога и возвращает **поток** чанков ответа.
     *
     * Для не-стримового бэкенда реализация эмитит один элемент с полным текстом.
     */
    fun chat(messages: List<ChatMessage>): Flow<ChatChunk>
}

/** Один фрагмент потокового ответа модели. */
data class ChatChunk(
    /** Прирост текста относительно предыдущего чанка (delta). */
    val delta: String,
    /** true, если это последний чанк. */
    val isFinal: Boolean,
)
