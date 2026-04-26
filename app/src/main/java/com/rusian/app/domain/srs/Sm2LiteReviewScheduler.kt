package com.rusian.app.domain.srs

import com.rusian.app.domain.model.ReviewRating
import com.rusian.app.domain.model.ReviewStateModel
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

class Sm2LiteReviewScheduler @Inject constructor() : ReviewScheduler {

    override fun createInitial(now: Long): ReviewStateModel {
        val next = now + DAY_MS
        return ReviewStateModel(
            easeFactor = DEFAULT_EASE,
            intervalDays = 1,
            repetition = 0,
            lapseCount = 0,
            nextReviewAt = next,
            lastReviewedAt = null,
        )
    }

    override fun update(previous: ReviewStateModel, rating: ReviewRating, now: Long): ReviewStateModel {
        val updatedEase = when (rating) {
            ReviewRating.AGAIN -> max(MIN_EASE, previous.easeFactor - 0.20)
            ReviewRating.HARD -> max(MIN_EASE, previous.easeFactor - 0.15)
            ReviewRating.GOOD -> previous.easeFactor
            ReviewRating.EASY -> previous.easeFactor + 0.15
        }

        val repetition = when (rating) {
            ReviewRating.AGAIN -> 0
            else -> previous.repetition + 1
        }

        val interval = when (rating) {
            ReviewRating.AGAIN -> 1
            ReviewRating.HARD -> {
                if (previous.intervalDays <= 1) 1 else max(1, (previous.intervalDays * 1.2).roundToInt())
            }
            ReviewRating.GOOD -> {
                when (previous.repetition) {
                    0 -> 1
                    1 -> max(previous.intervalDays, 3)
                    else -> max(1, (previous.intervalDays * updatedEase).roundToInt())
                }
            }
            ReviewRating.EASY -> {
                when (previous.repetition) {
                    0 -> 2
                    1 -> max(previous.intervalDays, 4)
                    else -> max(1, (previous.intervalDays * updatedEase * 1.3).roundToInt())
                }
            }
        }

        return ReviewStateModel(
            easeFactor = updatedEase,
            intervalDays = interval,
            repetition = repetition,
            lapseCount = if (rating == ReviewRating.AGAIN) previous.lapseCount + 1 else previous.lapseCount,
            nextReviewAt = now + (interval * DAY_MS),
            lastReviewedAt = now,
        )
    }

    private companion object {
        private const val DAY_MS = 86_400_000L
        private const val DEFAULT_EASE = 2.5
        private const val MIN_EASE = 1.3
    }
}
