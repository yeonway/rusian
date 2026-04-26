package com.rusian.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_state")
data class ReviewStateEntity(
    @PrimaryKey val studyItemId: Long,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetition: Int,
    val lapseCount: Int,
    val nextReviewAt: Long,
    val lastReviewedAt: Long?,
)
