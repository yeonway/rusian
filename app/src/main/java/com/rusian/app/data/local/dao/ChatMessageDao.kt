package com.rusian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rusian.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query(
        """
        SELECT * FROM chat_message
        WHERE conversationId = :conversationId
        ORDER BY createdAt ASC, id ASC
        """,
    )
    fun observeByConversationId(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query(
        """
        SELECT * FROM chat_message
        WHERE conversationId = :conversationId
        ORDER BY createdAt DESC, id DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecent(conversationId: Long, limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatMessageEntity): Long
}
