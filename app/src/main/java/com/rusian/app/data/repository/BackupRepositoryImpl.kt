package com.rusian.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.rusian.app.data.local.RusianDatabase
import com.rusian.app.data.local.dao.ReviewEventDao
import com.rusian.app.data.local.dao.ReviewStateDao
import com.rusian.app.data.local.entity.ReviewEventEntity
import com.rusian.app.data.local.entity.ReviewStateEntity
import com.rusian.app.domain.repository.BackupRepository
import com.rusian.app.domain.repository.BackupSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: RusianDatabase,
    private val reviewStateDao: ReviewStateDao,
    private val reviewEventDao: ReviewEventDao,
    private val json: Json,
) : BackupRepository {

    override suspend fun createSnapshot(reason: String): Result<BackupSnapshot> = runCatching {
        val now = System.currentTimeMillis()
        val snapshot = BackupPayload(
            createdAt = now,
            reason = reason,
            reviewStates = reviewStateDao.getAll().map { it.toDto() },
            reviewEvents = reviewEventDao.getAll().map { it.toDto() },
        )
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(dir, "backup-$now.json")
        file.writeText(json.encodeToString(BackupPayload.serializer(), snapshot))
        BackupSnapshot(path = file.absolutePath, createdAt = now)
    }

    override suspend fun restoreLatestSnapshot(): Result<Unit> = runCatching {
        val dir = File(context.filesDir, "backups")
        val latest = dir.listFiles()
            ?.filter { it.extension.lowercase() == "json" }
            ?.maxByOrNull { it.lastModified() }
            ?: throw IllegalStateException("No backup file")
        val payload = json.decodeFromString(BackupPayload.serializer(), latest.readText())
        database.withTransaction {
            reviewStateDao.clear()
            reviewEventDao.clear()
            reviewStateDao.upsertAll(payload.reviewStates.map { it.toEntity() })
            reviewEventDao.insertAll(payload.reviewEvents.map { it.toEntity() })
        }
    }

    @Serializable
    private data class BackupPayload(
        val createdAt: Long,
        val reason: String,
        val reviewStates: List<ReviewStateDto>,
        val reviewEvents: List<ReviewEventDto>,
    )

    @Serializable
    private data class ReviewStateDto(
        val studyItemId: Long,
        val easeFactor: Double,
        val intervalDays: Int,
        val repetition: Int,
        val lapseCount: Int,
        val nextReviewAt: Long,
        val lastReviewedAt: Long?,
    )

    @Serializable
    private data class ReviewEventDto(
        val studyItemId: Long,
        val rating: String,
        val reviewedAt: Long,
        val sessionMode: String,
    )

    private fun ReviewStateEntity.toDto() = ReviewStateDto(
        studyItemId = studyItemId,
        easeFactor = easeFactor,
        intervalDays = intervalDays,
        repetition = repetition,
        lapseCount = lapseCount,
        nextReviewAt = nextReviewAt,
        lastReviewedAt = lastReviewedAt,
    )

    private fun ReviewEventEntity.toDto() = ReviewEventDto(
        studyItemId = studyItemId,
        rating = rating.name,
        reviewedAt = reviewedAt,
        sessionMode = sessionMode,
    )

    private fun ReviewStateDto.toEntity() = ReviewStateEntity(
        studyItemId = studyItemId,
        easeFactor = easeFactor,
        intervalDays = intervalDays,
        repetition = repetition,
        lapseCount = lapseCount,
        nextReviewAt = nextReviewAt,
        lastReviewedAt = lastReviewedAt,
    )

    private fun ReviewEventDto.toEntity() = ReviewEventEntity(
        studyItemId = studyItemId,
        rating = enumValueOf(rating),
        reviewedAt = reviewedAt,
        sessionMode = sessionMode,
    )
}
