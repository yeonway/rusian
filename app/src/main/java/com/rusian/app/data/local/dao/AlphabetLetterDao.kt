package com.rusian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rusian.app.data.local.entity.AlphabetLetterEntity

@Dao
interface AlphabetLetterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AlphabetLetterEntity>)

    @Query("SELECT * FROM alphabet_letter ORDER BY sortOrder ASC")
    suspend fun getAll(): List<AlphabetLetterEntity>

    @Query("SELECT COUNT(*) FROM alphabet_letter")
    suspend fun count(): Int

    @Query("DELETE FROM alphabet_letter")
    suspend fun clear()
}
