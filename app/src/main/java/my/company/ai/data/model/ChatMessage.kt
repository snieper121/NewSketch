package my.company.ai.data.model

/** Одно сообщение в диалоге с AI-ассистентом. */
data class ChatMessage(
    val id: Long,
    val role: Role,
    val content: String,
    val timestamp: Long,
    val isStreaming: Boolean = false,
) {
    enum class Role { System, User, Assistant }
}
