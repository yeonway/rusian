package com.rusian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rusian.app.data.local.entity.ModelFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelFileDao {
    @Query("SELECT * FROM model_file ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ModelFileEntity>>

    @Query("SELECT * FROM model_file WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ModelFileEntity?

    @Query("SELECT * FROM model_file ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): ModelFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ModelFileEntity): Long

    @Delete
    suspend fun delete(entity: ModelFileEntity)
}
