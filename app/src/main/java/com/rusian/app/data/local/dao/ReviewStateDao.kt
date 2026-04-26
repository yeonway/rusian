package com.rusian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rusian.app.data.local.entity.ReviewStateEntity

@Dao
interface ReviewStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReviewStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ReviewStateEntity>)

    @Query("SELECT * FROM review_state WHERE studyItemId = :studyItemId LIMIT 1")
    suspend fun findByStudyItemId(studyItemId: Long): ReviewStateEntity?

    @Query("SELECT * FROM review_state")
    suspend fun getAll(): List<ReviewStateEntity>

    @Query("DELETE FROM review_state")
    suspend fun clear()
}
