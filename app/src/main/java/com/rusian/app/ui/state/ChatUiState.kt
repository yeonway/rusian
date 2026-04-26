package com.rusian.app.ui.state

import com.rusian.app.domain.model.ChatMessage
import com.rusian.app.domain.model.Conversation
import com.rusian.app.domain.model.ModelFile

data class ChatUiState(
    val models: List<ModelFile> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val selectedConversationId: Long? = null,
    val roomNameInput: String = "",
    val renameInput: String = "",
    val messageInput: String = "",
    val loading: Boolean = false,
    val importingModel: Boolean = false,
    val sending: Boolean = false,
    val addingStudyData: Boolean = false,
    val generationProgress: Float? = null,
    val generationTokensPerSecond: Float? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)
