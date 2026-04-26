package com.rusian.app.data.repository

import androidx.room.withTransaction
import com.rusian.app.data.local.RusianDatabase
import com.rusian.app.data.local.dao.AlphabetLetterDao
import com.rusian.app.data.local.dao.CategoryDao
import com.rusian.app.data.local.dao.DatasetInfoDao
import com.rusian.app.data.local.dao.StudyItemDao
import com.rusian.app.data.local.dao.WordDao
import com.rusian.app.data.local.dao.WordGlossDao
import com.rusian.app.data.local.entity.AlphabetLetterEntity
import com.rusian.app.data.local.entity.CategoryEntity
import com.rusian.app.data.local.entity.DatasetInfoEntity
import com.rusian.app.data.local.entity.StudyItemEntity
import com.rusian.app.data.local.entity.WordEntity
import com.rusian.app.data.local.entity.WordGlossEntity
import com.rusian.app.data.remote.ContentPackDto
import com.rusian.app.domain.model.StudyKind
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class ContentInstaller @Inject constructor(
    private val database: RusianDatabase,
    private val categoryDao: CategoryDao,
    private val alphabetDao: AlphabetLetterDao,
    private val wordDao: WordDao,
    private val wordGlossDao: WordGlossDao,
    private val studyItemDao: StudyItemDao,
    private val datasetInfoDao: DatasetInfoDao,
    private val json: Json,
) : ContentInstallService {
    override suspend fun install(
        pack: ContentPackDto,
        source: String,
        sha256: String?,
        cleanInstall: Boolean,
        installedAt: Long,
    ) {
        database.withTransaction {
            if (cleanInstall) {
                categoryDao.clear()
                alphabetDao.clear()
                wordDao.clear()
                wordGlossDao.clear()
            }

            val categories = pack.categories.map {
                CategoryEntity(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    sortOrder = it.sortOrder,
                )
            }
            categoryDao.upsertAll(categories)

            val letters = pack.alphabet.map {
                AlphabetLetterEntity(
                    id = it.id,
                    uppercase = it.uppercase,
                    lowercase = it.lowercase,
                    nameKo = it.nameKo,
                    romanization = it.romanization,
                    pronunciationHint = it.pronunciationHint,
                    letterType = it.letterType.uppercase(),
                    soundFeel = it.soundFeel,
                    confusionGroup = it.confusionGroup,
                    confusionNote = it.confusionNote,
                    usageFrequency = it.usageFrequency,
                    tmiNote = it.tmiNote,
                    sortOrder = it.sortOrder,
                    examplesJson = json.encodeToString(it.examples),
                )
            }
            alphabetDao.upsertAll(letters)

            val words = pack.words.map {
                WordEntity(
                    id = it.id,
                    categoryId = it.categoryId,
                    word = it.word,
                    stress = it.stress,
                    transliteration = it.transliteration,
                    pronunciationKo = it.pronunciationKo,
                    partOfSpeech = it.partOfSpeech,
                    tone = it.tone.uppercase(),
                    situationHint = it.situationHint,
                    usageNote = it.usageNote,
                    pairKey = it.pairKey,
                    contextTag = it.contextTag,
                    difficulty = it.difficulty,
                    exampleRu = it.example?.ru,
                    exampleKo = it.example?.ko,
                    active = it.active,
                )
            }
            wordDao.upsertAll(words)

            val glosses = pack.words.flatMap { word ->
                word.meanings.mapIndexed { index, meaning ->
                    WordGlossEntity(
                        wordId = word.id,
                        locale = pack.glossLanguage,
                        meaningOrder = index,
                        meaning = meaning,
                    )
                }
            }
            if (cleanInstall) {
                wordGlossDao.clear()
            }
            wordGlossDao.upsertAll(glosses)

            val incomingStableIds = mutableListOf<String>()
            val now = installedAt
            for (letter in pack.alphabet) {
                val stableId = "alphabet:${letter.id}"
                incomingStableIds += stableId
                upsertStudyItem(
                    kind = StudyKind.ALPHABET,
                    remoteStableId = stableId,
                    alphabetId = letter.id,
                    wordId = null,
                    now = now,
                )
            }
            for (word in pack.words) {
                val stableId = "word:${word.id}"
                incomingStableIds += stableId
                upsertStudyItem(
                    kind = StudyKind.WORD,
                    remoteStableId = stableId,
                    alphabetId = null,
                    wordId = word.id,
                    now = now,
                )
            }

            if (incomingStableIds.isEmpty()) {
                studyItemDao.hideAll()
                wordDao.retireAll()
            } else {
                studyItemDao.hideItemsNotIn(incomingStableIds)
                val incomingWordIds = pack.words.map { it.id }
                if (incomingWordIds.isNotEmpty()) {
                    wordDao.retireWordsNotIn(incomingWordIds)
                } else {
                    wordDao.retireAll()
                }
            }

            datasetInfoDao.upsert(
                DatasetInfoEntity(
                    schemaVersion = pack.schemaVersion,
                    datasetVersion = pack.datasetVersion,
                    sha256 = sha256,
                    installedAt = installedAt,
                    source = source,
                ),
            )
        }
    }

    override suspend fun installAlphabetOnly(
        pack: ContentPackDto,
        installedAt: Long,
    ) {
        database.withTransaction {
            val letters = pack.alphabet.map {
                AlphabetLetterEntity(
                    id = it.id,
                    uppercase = it.uppercase,
                    lowercase = it.lowercase,
                    nameKo = it.nameKo,
                    romanization = it.romanization,
                    pronunciationHint = it.pronunciationHint,
                    letterType = it.letterType.uppercase(),
                    soundFeel = it.soundFeel,
                    confusionGroup = it.confusionGroup,
                    confusionNote = it.confusionNote,
                    usageFrequency = it.usageFrequency,
                    tmiNote = it.tmiNote,
                    sortOrder = it.sortOrder,
                    examplesJson = json.encodeToString(it.examples),
                )
            }
            alphabetDao.upsertAll(letters)

            for (letter in pack.alphabet) {
                upsertStudyItem(
                    kind = StudyKind.ALPHABET,
                    remoteStableId = "alphabet:${letter.id}",
                    alphabetId = letter.id,
                    wordId = null,
                    now = installedAt,
                )
            }
        }
    }

    private suspend fun upsertStudyItem(
        kind: StudyKind,
        remoteStableId: String,
        alphabetId: String?,
        wordId: String?,
        now: Long,
    ) {
        val existing = studyItemDao.findByRemoteStableId(remoteStableId)
        if (existing == null) {
            studyItemDao.insert(
                StudyItemEntity(
                    kind = kind,
                    remoteStableId = remoteStableId,
                    alphabetId = alphabetId,
                    wordId = wordId,
                    isVisible = true,
                    createdAt = now,
                ),
            )
            return
        }
        studyItemDao.update(
            existing.copy(
                kind = kind,
                alphabetId = alphabetId,
                wordId = wordId,
                isVisible = true,
            ),
        )
    }
}
