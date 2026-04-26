package com.rusian.app.domain.model

data class ReviewStateModel(
    val easeFactor: Double,
    val intervalDays: Int,
    val repetition: Int,
    val lapseCount: Int,
    val nextReviewAt: Long,
    val lastReviewedAt: Long?,
)
