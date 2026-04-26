package com.rusian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rusian.app.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversation ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatest(): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConversationEntity): Long

    @Query("UPDATE conversation SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, updatedAt: Long)

    @Query("UPDATE conversation SET modelId = :modelId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateModel(id: Long, modelId: Long?, updatedAt: Long)

    @Query("UPDATE conversation SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchUpdatedAt(id: Long, updatedAt: Long)

    @Query("DELETE FROM conversation WHERE id = :id")
    suspend fun deleteById(id: Long)
}
