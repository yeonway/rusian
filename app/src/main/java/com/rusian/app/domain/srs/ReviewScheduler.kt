package com.rusian.app.domain.srs

import com.rusian.app.domain.model.ReviewRating
import com.rusian.app.domain.model.ReviewStateModel

interface ReviewScheduler {
    fun createInitial(now: Long): ReviewStateModel
    fun update(previous: ReviewStateModel, rating: ReviewRating, now: Long): ReviewStateModel
}
