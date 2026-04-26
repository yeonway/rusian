package com.rusian.app.domain.model

enum class ChatRole {
    USER,
    ASSISTANT,
}

data class ModelFile(
    val id: Long,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val createdAt: Long,
)

data class Conversation(
    val id: Long,
    val title: String,
    val modelId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class ChatMessage(
    val id: Long,
    val conversationId: Long,
    val role: ChatRole,
    val text: String,
    val createdAt: Long,
)
