package my.company.ai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Запрос Chat Completions (OpenAI-совместимый). */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = false,
    val temperature: Double? = null,
)

@Serializable
data class ChatMessageDto(
    val role: String, // "system" | "user" | "assistant"
    val content: String,
)

/** Не-стриминговый ответ. */
@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChoiceDto> = emptyList(),
)

@Serializable
data class ChoiceDto(
    val index: Int = 0,
    val message: ChatMessageDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)
