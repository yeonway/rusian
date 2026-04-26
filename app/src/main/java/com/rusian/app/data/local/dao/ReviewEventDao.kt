package com.rusian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rusian.app.data.local.entity.ReviewEventEntity

@Dao
interface ReviewEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReviewEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReviewEventEntity>)

    @Query("SELECT * FROM review_event ORDER BY reviewedAt DESC")
    suspend fun getAll(): List<ReviewEventEntity>

    @Query("DELETE FROM review_event")
    suspend fun clear()
}
