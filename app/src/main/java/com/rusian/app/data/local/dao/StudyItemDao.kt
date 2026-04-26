package com.rusian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rusian.app.data.local.entity.StudyItemEntity
import com.rusian.app.data.local.model.CategoryProgressRow
import com.rusian.app.data.local.model.StudyCardRow
import com.rusian.app.domain.model.StudyKind

@Dao
interface StudyItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StudyItemEntity): Long

    @Update
    suspend fun update(item: StudyItemEntity)

    @Query("SELECT * FROM study_item WHERE remoteStableId = :remoteStableId LIMIT 1")
    suspend fun findByRemoteStableId(remoteStableId: String): StudyItemEntity?

    @Query("SELECT * FROM study_item")
    suspend fun getAll(): List<StudyItemEntity>

    @Query("UPDATE study_item SET isVisible = 0")
    suspend fun hideAll()

    @Query(
        """
        SELECT 
            s.id AS studyItemId,
            s.kind AS kind,
            s.remoteStableId AS remoteStableId,
            r.nextReviewAt AS dueAt,
            w.categoryId AS categoryId,
            w.word AS word,
            w.stress AS stress,
            w.transliteration AS transliteration,
            w.pronunciationKo AS pronunciationKo,
            w.tone AS wordTone,
            w.situationHint AS situationHint,
            w.usageNote AS usageNote,
            w.contextTag AS contextTag,
            w.exampleRu AS exampleRu,
            w.exampleKo AS exampleKo,
            a.uppercase AS uppercase,
            a.lowercase AS lowercase,
            a.romanization AS letterRomanization,
            a.pronunciationHint AS letterHint,
            a.letterType AS letterType,
            a.soundFeel AS letterSoundFeel,
            a.confusionNote AS letterConfusionNote,
            a.usageFrequency AS letterUsageFrequency,
            a.tmiNote AS letterTmiNote
        FROM study_item s
        LEFT JOIN review_state r ON r.studyItemId = s.id
        LEFT JOIN word w ON w.id = s.wordId
        LEFT JOIN alphabet_letter a ON a.id = s.alphabetId
        WHERE s.isVisible = 1
          AND (:kind IS NULL OR s.kind = :kind)
          AND (
              (:kind = 'ALPHABET' AND s.kind = 'ALPHABET')
              OR r.nextReviewAt IS NULL
              OR r.nextReviewAt <= :now
          )
        ORDER BY
            CASE WHEN s.kind = 'ALPHABET' THEN 0 ELSE 1 END ASC,
            CASE WHEN s.kind = 'ALPHABET' THEN COALESCE(a.sortOrder, 9999) ELSE COALESCE(r.nextReviewAt, 0) END ASC,
            s.id ASC
        LIMIT :limit
        """,
    )
    suspend fun getDueRows(now: Long, kind: StudyKind?, limit: Int): List<StudyCardRow>

    @Query(
        """
        SELECT COUNT(*)
        FROM study_item s
        LEFT JOIN review_state r ON r.studyItemId = s.id
        LEFT JOIN word w ON w.id = s.wordId
        WHERE s.isVisible = 1
          AND (:kind IS NULL OR s.kind = :kind)
          AND (
              (:kind = 'ALPHABET' AND s.kind = 'ALPHABET')
              OR r.nextReviewAt IS NULL
              OR r.nextReviewAt <= :now
          )
          AND (
              s.kind = 'ALPHABET'
              OR :selectedCategoryCount = 0
              OR (w.categoryId IS NOT NULL AND w.categoryId IN (:selectedCategoryIds))
          )
        """,
    )
    suspend fun countDueRows(
        now: Long,
        kind: StudyKind?,
        selectedCategoryIds: Set<String>,
        selectedCategoryCount: Int,
    ): Int

    @Query("UPDATE study_item SET isVisible = 0 WHERE remoteStableId NOT IN (:remoteStableIds)")
    suspend fun hideItemsNotIn(remoteStableIds: List<String>)

    @Query("UPDATE study_item SET isVisible = 0, wordId = NULL, alphabetId = NULL WHERE remoteStableId = :remoteStableId")
    suspend fun hideByRemoteStableId(remoteStableId: String)

    @Query(
        """
        SELECT
            c.id AS categoryId,
            c.name AS categoryName,
            COUNT(w.id) AS total,
            SUM(CASE WHEN rs.repetition > 0 THEN 1 ELSE 0 END) AS learned
        FROM category c
        LEFT JOIN word w ON w.categoryId = c.id AND w.active = 1
        LEFT JOIN study_item si ON si.wordId = w.id AND si.isVisible = 1
        LEFT JOIN review_state rs ON rs.studyItemId = si.id
        GROUP BY c.id, c.name
        ORDER BY c.sortOrder ASC
        """,
    )
    suspend fun getCategoryProgress(): List<CategoryProgressRow>
}
