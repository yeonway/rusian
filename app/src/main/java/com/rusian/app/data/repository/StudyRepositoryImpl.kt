package com.rusian.app.data.repository

import com.rusian.app.data.local.dao.AlphabetLetterDao
import com.rusian.app.data.local.dao.CategoryDao
import com.rusian.app.data.local.dao.DatasetInfoDao
import com.rusian.app.data.local.dao.ReviewEventDao
import com.rusian.app.data.local.dao.ReviewStateDao
import com.rusian.app.data.local.dao.StudyItemDao
import com.rusian.app.data.local.dao.WordGlossDao
import com.rusian.app.data.local.entity.ReviewEventEntity
import com.rusian.app.data.local.entity.ReviewStateEntity
import com.rusian.app.data.preferences.UserPreferencesGateway
import com.rusian.app.data.remote.SeedContentDataSource
import com.rusian.app.domain.model.CategoryProgress
import com.rusian.app.domain.model.CategoryOption
import com.rusian.app.domain.model.OnboardingConfig
import com.rusian.app.domain.model.ReviewRating
import com.rusian.app.domain.model.ReviewStateModel
import com.rusian.app.domain.model.StudyCard
import com.rusian.app.domain.model.StudyKind
import com.rusian.app.domain.repository.StudyRepository
import com.rusian.app.domain.srs.ReviewScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class StudyRepositoryImpl @Inject constructor(
    private val alphabetDao: AlphabetLetterDao,
    private val categoryDao: CategoryDao,
    private val studyItemDao: StudyItemDao,
    private val reviewStateDao: ReviewStateDao,
    private val reviewEventDao: ReviewEventDao,
    private val wordGlossDao: WordGlossDao,
    private val datasetInfoDao: DatasetInfoDao,
    private val assetContentDataSource: SeedContentDataSource,
    private val contentInstaller: ContentInstallService,
    private val preferencesDataSource: UserPreferencesGateway,
    private val reviewScheduler: ReviewScheduler,
) : StudyRepository {

    override suspend fun ensureSeedImported() {
        val localDataset = datasetInfoDao.get()
        val pack = assetContentDataSource.loadSeedContentPack()
        if (localDataset != null) {
            if (
                localDataset.source.startsWith("asset") &&
                localDataset.datasetVersion != pack.datasetVersion
            ) {
                contentInstaller.install(
                    pack = pack,
                    source = "asset-seed",
                    sha256 = null,
                    cleanInstall = false,
                    installedAt = System.currentTimeMillis(),
                )
                return
            }
            if (alphabetDao.count() < RUSSIAN_ALPHABET_COUNT && pack.alphabet.size >= RUSSIAN_ALPHABET_COUNT) {
                contentInstaller.installAlphabetOnly(
                    pack = pack,
                    installedAt = System.currentTimeMillis(),
                )
            }
            return
        }
        contentInstaller.install(
            pack = pack,
            source = "asset-seed",
            sha256 = null,
            cleanInstall = false,
            installedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun setOnboarding(config: OnboardingConfig) {
        preferencesDataSource.completeOnboarding(
            dailyGoal = config.dailyGoal,
            selectedCategoryIds = config.selectedCategoryIds,
        )
    }

    override suspend fun getCategoryOptions(): List<CategoryOption> {
        ensureSeedImported()
        return categoryDao.getAll().map { CategoryOption(id = it.id, name = it.name) }
    }

    override fun observeOnboardingDone(): Flow<Boolean> {
        return preferencesDataSource.preferences.map { it.onboardingCompleted }
    }

    override suspend fun getDueCards(mode: StudyKind?, limit: Int): List<StudyCard> {
        ensureSeedImported()
        val prefs = preferencesDataSource.preferences.first()
        val rows = studyItemDao.getDueRows(
            now = System.currentTimeMillis(),
            kind = mode,
            limit = limit * 3,
        )
        val selectedCategories = prefs.selectedCategoryIds
        val filteredRows = rows.filter { row ->
            if (row.kind == StudyKind.ALPHABET) return@filter true
            selectedCategories.isEmpty() || (row.categoryId != null && selectedCategories.contains(row.categoryId))
        }.take(limit)
        val wordIds = filteredRows.mapNotNull { it.remoteStableId.removePrefix("word:").takeIf { id -> id != it.remoteStableId } }
        val meanings = if (wordIds.isEmpty()) emptyMap() else {
            wordGlossDao.getMeanings(wordIds, locale = "ko")
                .groupBy({ it.wordId }, { it.meaning })
        }
        return filteredRows.map { row ->
            when (row.kind) {
                StudyKind.ALPHABET -> {
                    val title = listOfNotNull(row.uppercase, row.lowercase).joinToString(" ")
                    StudyCard(
                        studyItemId = row.studyItemId,
                        kind = row.kind,
                        remoteStableId = row.remoteStableId,
                        title = title.ifBlank { row.remoteStableId },
                        subtitle = row.letterRomanization,
                        hint = row.letterHint,
                        structureLabel = row.letterType,
                        soundFeel = row.letterSoundFeel,
                        confusionNote = row.letterConfusionNote,
                        usageFrequency = row.letterUsageFrequency,
                        tmiNote = row.letterTmiNote,
                        tone = null,
                        situationHint = null,
                        usageNote = null,
                        contextTag = null,
                        meanings = emptyList(),
                        exampleRu = null,
                        exampleKo = null,
                        dueAt = row.dueAt ?: 0L,
                    )
                }
                StudyKind.WORD -> {
                    val wordId = row.remoteStableId.removePrefix("word:")
                    StudyCard(
                        studyItemId = row.studyItemId,
                        kind = row.kind,
                        remoteStableId = row.remoteStableId,
                        title = row.stress ?: row.word ?: row.remoteStableId,
                        subtitle = row.transliteration,
                        hint = row.pronunciationKo,
                        structureLabel = null,
                        soundFeel = null,
                        confusionNote = null,
                        usageFrequency = null,
                        tmiNote = null,
                        tone = row.wordTone,
                        situationHint = row.situationHint,
                        usageNote = row.usageNote,
                        contextTag = row.contextTag,
                        meanings = meanings[wordId] ?: emptyList(),
                        exampleRu = row.exampleRu,
                        exampleKo = row.exampleKo,
                        dueAt = row.dueAt ?: 0L,
                    )
                }
            }
        }
    }

    override suspend fun countDueCards(mode: StudyKind?): Int {
        ensureSeedImported()
        val prefs = preferencesDataSource.preferences.first()
        val selectedCategories = prefs.selectedCategoryIds
        return studyItemDao.countDueRows(
            now = System.currentTimeMillis(),
            kind = mode,
            selectedCategoryIds = selectedCategories,
            selectedCategoryCount = selectedCategories.size,
        )
    }

    override suspend fun submitReview(studyItemId: Long, rating: ReviewRating, reviewedAt: Long) {
        val previous = reviewStateDao.findByStudyItemId(studyItemId)?.toModel()
            ?: reviewScheduler.createInitial(reviewedAt)
        val updated = reviewScheduler.update(previous, rating, reviewedAt)
        reviewStateDao.upsert(updated.toEntity(studyItemId))
        reviewEventDao.insert(
            ReviewEventEntity(
                studyItemId = studyItemId,
                rating = rating,
                reviewedAt = reviewedAt,
                sessionMode = "mixed",
            ),
        )
    }

    override suspend fun getCategoryProgress(): List<CategoryProgress> {
        ensureSeedImported()
        return studyItemDao.getCategoryProgress().map {
            CategoryProgress(
                categoryId = it.categoryId,
                categoryName = it.categoryName,
                total = it.total,
                learned = it.learned,
            )
        }
    }

    private fun ReviewStateEntity.toModel(): ReviewStateModel {
        return ReviewStateModel(
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            repetition = repetition,
            lapseCount = lapseCount,
            nextReviewAt = nextReviewAt,
            lastReviewedAt = lastReviewedAt,
        )
    }

    private fun ReviewStateModel.toEntity(studyItemId: Long): ReviewStateEntity {
        return ReviewStateEntity(
            studyItemId = studyItemId,
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            repetition = repetition,
            lapseCount = lapseCount,
            nextReviewAt = nextReviewAt,
            lastReviewedAt = lastReviewedAt,
        )
    }

    private companion object {
        const val RUSSIAN_ALPHABET_COUNT = 33
    }
}
