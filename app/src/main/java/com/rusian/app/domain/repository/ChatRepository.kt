package com.rusian.app.domain.repository

import android.net.Uri
import com.rusian.app.domain.model.ChatMessage
import com.rusian.app.domain.model.Conversation
import com.rusian.app.domain.model.ModelFile
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeModels(): Flow<List<ModelFile>>
    fun observeConversations(): Flow<List<Conversation>>
    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>>
    fun observeLastConversationId(): Flow<Long?>

    suspend fun importModel(uri: Uri): Result<ModelFile>
    suspend fun deleteModel(modelId: Long): Result<Unit>

    suspend fun createConversation(title: String): Result<Conversation>
    suspend fun renameConversation(conversationId: Long, title: String): Result<Unit>
    suspend fun deleteConversation(conversationId: Long): Result<Unit>
    suspend fun setConversationModel(conversationId: Long, modelId: Long?): Result<Unit>
    suspend fun setLastConversationId(conversationId: Long?)

    suspend fun saveUserMessage(conversationId: Long, text: String): Result<ChatMessage>

    suspend fun sendMessage(
        conversationId: Long,
        text: String,
        persistUserMessage: Boolean = true,
        onProgress: (Float) -> Unit = {},
    ): Result<ChatMessage>

    suspend fun generateStudyData(
        conversationId: Long,
        request: String,
        persistUserMessage: Boolean = true,
        onProgress: (Float) -> Unit = {},
    ): Result<Int>

    fun cancelGeneration()
}
