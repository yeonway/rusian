package com.rusian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rusian.app.data.local.entity.WordEntity

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WordEntity>)

    @Query("UPDATE word SET active = 0 WHERE id NOT IN (:activeWordIds)")
    suspend fun retireWordsNotIn(activeWordIds: List<String>)

    @Query("UPDATE word SET active = 0")
    suspend fun retireAll()

    @Query("SELECT * FROM word WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): WordEntity?

    @Query("SELECT * FROM word WHERE word = :word AND active = 1 LIMIT 1")
    suspend fun findActiveByWord(word: String): WordEntity?

    @Query("SELECT id FROM word WHERE active = 1")
    suspend fun getActiveIds(): List<String>

    @Query("DELETE FROM word")
    suspend fun clear()
}
