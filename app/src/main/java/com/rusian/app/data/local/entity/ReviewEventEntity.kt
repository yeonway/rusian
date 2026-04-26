package com.rusian.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rusian.app.domain.model.ReviewRating

@Entity(
    tableName = "review_event",
    indices = [Index("studyItemId"), Index("reviewedAt")],
)
data class ReviewEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studyItemId: Long,
    val rating: ReviewRating,
    val reviewedAt: Long,
    val sessionMode: String,
)
