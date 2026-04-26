package com.rusian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rusian.app.data.local.entity.WordGlossEntity
import com.rusian.app.data.local.model.WordMeaningRow

@Dao
interface WordGlossDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WordGlossEntity>)

    @Query("DELETE FROM word_gloss")
    suspend fun clear()

    @Query(
        """
        SELECT wordId, meaning, meaningOrder
        FROM word_gloss
        WHERE locale = :locale AND wordId IN (:wordIds)
        ORDER BY wordId ASC, meaningOrder ASC
        """,
    )
    suspend fun getMeanings(wordIds: List<String>, locale: String = "ko"): List<WordMeaningRow>

    @Query("DELETE FROM word_gloss WHERE wordId = :wordId")
    suspend fun deleteByWordId(wordId: String)
}
