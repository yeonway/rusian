package com.rusian.app.domain.srs

import com.rusian.app.domain.model.ReviewRating
import com.rusian.app.domain.model.ReviewStateModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2LiteReviewSchedulerTest {

    private val scheduler = Sm2LiteReviewScheduler()

    @Test
    fun `GOOD on first review keeps one day interval`() {
        val now = 1_000_000L
        val initial = scheduler.createInitial(now)

        val updated = scheduler.update(initial, ReviewRating.GOOD, now)

        assertEquals(1, updated.intervalDays)
        assertEquals(1, updated.repetition)
        assertEquals(now + DAY_MS, updated.nextReviewAt)
    }

    @Test
    fun `consecutive GOOD increases interval`() {
        val now = 1_000_000L
        val first = scheduler.update(scheduler.createInitial(now), ReviewRating.GOOD, now)
        val second = scheduler.update(first, ReviewRating.GOOD, now + DAY_MS)

        assertEquals(3, second.intervalDays)
        assertEquals(2, second.repetition)
    }

    @Test
    fun `AGAIN resets repetition and increments lapse`() {
        val now = 1_000_000L
        val first = scheduler.update(scheduler.createInitial(now), ReviewRating.GOOD, now)
        val again = scheduler.update(first, ReviewRating.AGAIN, now + DAY_MS)

        assertEquals(0, again.repetition)
        assertEquals(1, again.lapseCount)
        assertEquals(1, again.intervalDays)
    }

    @Test
    fun `EASY increases ease factor`() {
        val now = 1_000_000L
        val initial = scheduler.createInitial(now)
        val updated = scheduler.update(initial, ReviewRating.EASY, now)

        assertTrue(updated.easeFactor > initial.easeFactor)
        assertEquals(2, updated.intervalDays)
    }

    @Test
    fun `GOOD after AGAIN restarts from short interval`() {
        val now = 1_000_000L
        val firstGood = scheduler.update(scheduler.createInitial(now), ReviewRating.GOOD, now)
        val failed = scheduler.update(firstGood, ReviewRating.AGAIN, now + DAY_MS)
        val recovered = scheduler.update(failed, ReviewRating.GOOD, now + (2 * DAY_MS))

        assertEquals(0, failed.repetition)
        assertEquals(1, failed.intervalDays)
        assertEquals(1, recovered.repetition)
        assertEquals(1, recovered.intervalDays)
    }

    @Test
    fun `HARD uses scaled interval for due calculation`() {
        val now = 1_000_000L
        val previous = ReviewStateModel(
            easeFactor = 2.5,
            intervalDays = 10,
            repetition = 4,
            lapseCount = 0,
            nextReviewAt = now,
            lastReviewedAt = now - DAY_MS,
        )

        val updated = scheduler.update(previous, ReviewRating.HARD, now)

        assertEquals(12, updated.intervalDays)
        assertEquals(now + (12 * DAY_MS), updated.nextReviewAt)
    }

    @Test
    fun `GOOD second repetition does not reduce existing interval`() {
        val now = 1_000_000L
        val previous = ReviewStateModel(
            easeFactor = 2.5,
            intervalDays = 5,
            repetition = 1,
            lapseCount = 0,
            nextReviewAt = now,
            lastReviewedAt = now - DAY_MS,
        )

        val updated = scheduler.update(previous, ReviewRating.GOOD, now)

        assertEquals(5, updated.intervalDays)
    }

    @Test
    fun `EASY second repetition does not reduce existing interval`() {
        val now = 1_000_000L
        val previous = ReviewStateModel(
            easeFactor = 2.5,
            intervalDays = 6,
            repetition = 1,
            lapseCount = 0,
            nextReviewAt = now,
            lastReviewedAt = now - DAY_MS,
        )

        val updated = scheduler.update(previous, ReviewRating.EASY, now)

        assertEquals(6, updated.intervalDays)
    }

    companion object {
        private const val DAY_MS = 86_400_000L
    }
}
