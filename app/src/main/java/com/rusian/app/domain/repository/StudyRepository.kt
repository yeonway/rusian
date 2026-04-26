package com.rusian.app.domain.repository

import com.rusian.app.domain.model.CategoryProgress
import com.rusian.app.domain.model.CategoryOption
import com.rusian.app.domain.model.OnboardingConfig
import com.rusian.app.domain.model.ReviewRating
import com.rusian.app.domain.model.StudyCard
import com.rusian.app.domain.model.StudyKind
import kotlinx.coroutines.flow.Flow

interface StudyRepository {
    suspend fun ensureSeedImported()
    suspend fun setOnboarding(config: OnboardingConfig)
    suspend fun getCategoryOptions(): List<CategoryOption>
    fun observeOnboardingDone(): Flow<Boolean>
    suspend fun getDueCards(mode: StudyKind?, limit: Int): List<StudyCard>
    suspend fun countDueCards(mode: StudyKind?): Int
    suspend fun submitReview(studyItemId: Long, rating: ReviewRating, reviewedAt: Long)
    suspend fun getCategoryProgress(): List<CategoryProgress>
}
