package my.company.ai.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import my.company.ai.data.model.ChatMessage
import my.company.ai.data.remote.dto.ChatCompletionRequest
import my.company.ai.data.remote.dto.ChatCompletionResponse
import my.company.ai.data.remote.dto.ChatMessageDto
import my.company.ai.data.repository.SettingsRepository

/**
 * Клиент OpenAI-совместимого Chat Completions API.
 *
 * Работает с `api.openai.com`, локальными (Ollama, LM Studio, vLLM)
 * и прокси-сервисами (OpenRouter) при условии совпадения контракта.
 *
 * На этапе каркаса используется **не-стриминговый** режим — возвращается один
 * финальный чанк.  Стриминг (SSE) можно добавить без изменения интерфейса
 * [AiService] (ADR-008).
 */
class OpenAiCompatibleService(
    private val client: HttpClient,
    private val settings: SettingsRepository,
) : AiService {

    override fun chat(messages: List<ChatMessage>): Flow<ChatChunk> = flow {
        val cfg = settings.snapshot.first()
        require(cfg.apiKey.isNotBlank()) {
            "API key не задан. Настройки → AI."
        }

        val url = cfg.baseUrl.trimEnd('/') + "/chat/completions"
        val request = ChatCompletionRequest(
            model = cfg.model,
            stream = false,
            messages = messages.map { msg ->
                ChatMessageDto(
                    role = when (msg.role) {
                        ChatMessage.Role.System -> "system"
                        ChatMessage.Role.User -> "user"
                        ChatMessage.Role.Assistant -> "assistant"
                    },
                    content = msg.content,
                )
            },
        )

        val response: ChatCompletionResponse = client.post(url) {
            header(HttpHeaders.Authorization, "Bearer ${cfg.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        val text = response.choices.firstOrNull()?.message?.content.orEmpty()
        emit(ChatChunk(delta = text, isFinal = true))
    }
}
