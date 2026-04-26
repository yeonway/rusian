package com.rusian.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rusian.app.data.local.entity.ReviewStateEntity
import com.rusian.app.data.remote.AlphabetLetterDto
import com.rusian.app.data.remote.CategoryDto
import com.rusian.app.data.remote.ContentPackDto
import com.rusian.app.data.remote.WordDto
import com.rusian.app.data.repository.ContentInstaller
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RusianDatabaseTest {

    private lateinit var db: RusianDatabase
    private lateinit var installer: ContentInstaller

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RusianDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        installer = ContentInstaller(
            database = db,
            categoryDao = db.categoryDao(),
            alphabetDao = db.alphabetLetterDao(),
            wordDao = db.wordDao(),
            wordGlossDao = db.wordGlossDao(),
            studyItemDao = db.studyItemDao(),
            datasetInfoDao = db.datasetInfoDao(),
            json = Json,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun dueQueryReturnsInsertedItems() = runBlocking {
        installer.install(
            pack = pack("v1", words = listOf(word("word_1", "привет"))),
            source = "test",
            sha256 = null,
            cleanInstall = false,
            installedAt = 1L,
        )

        val rows = db.studyItemDao().getDueRows(now = Long.MAX_VALUE, kind = null, limit = 10)

        assertEquals(2, rows.size) // 1 alphabet + 1 word
        assertTrue(rows.any { it.remoteStableId == "word:word_1" })
    }

    @Test
    fun cleanInstallKeepsReviewStateWhenStableIdMatches() = runBlocking {
        installer.install(
            pack = pack("v1", words = listOf(word("word_1", "привет"))),
            source = "test",
            sha256 = null,
            cleanInstall = false,
            installedAt = 1L,
        )
        val item = db.studyItemDao().findByRemoteStableId("word:word_1")
        assertNotNull(item)
        db.reviewStateDao().upsert(
            ReviewStateEntity(
                studyItemId = item!!.id,
                easeFactor = 2.5,
                intervalDays = 3,
                repetition = 2,
                lapseCount = 0,
                nextReviewAt = 10L,
                lastReviewedAt = 1L,
            ),
        )

        installer.install(
            pack = pack("v2", words = listOf(word("word_1", "здравствуй"))),
            source = "test",
            sha256 = null,
            cleanInstall = true,
            installedAt = 2L,
        )

        val state = db.reviewStateDao().findByStudyItemId(item.id)
        assertNotNull(state)
        assertEquals(2, state!!.repetition)
    }

    @Test
    fun cleanInstallRetiresMissingWordsAndHidesStudyItems() = runBlocking {
        installer.install(
            pack = pack(
                "v1",
                words = listOf(
                    word("word_1", "привет"),
                    word("word_2", "пока"),
                ),
            ),
            source = "test",
            sha256 = null,
            cleanInstall = false,
            installedAt = 1L,
        )

        installer.install(
            pack = pack("v2", words = listOf(word("word_1", "здравствуй"))),
            source = "test",
            sha256 = null,
            cleanInstall = true,
            installedAt = 2L,
        )

        val retiredWord = db.wordDao().findById("word_2")
        val retiredItem = db.studyItemDao().findByRemoteStableId("word:word_2")

        assertNotNull(retiredWord)
        assertFalse(retiredWord!!.active)
        assertNotNull(retiredItem)
        assertFalse(retiredItem!!.isVisible)
    }

    private fun pack(version: String, words: List<WordDto>): ContentPackDto {
        return ContentPackDto(
            schemaVersion = 1,
            datasetVersion = version,
            language = "ru",
            glossLanguage = "ko",
            categories = listOf(
                CategoryDto(
                    id = "daily",
                    name = "일상",
                    description = null,
                    sortOrder = 1,
                ),
            ),
            alphabet = listOf(
                AlphabetLetterDto(
                    id = "a",
                    uppercase = "А",
                    lowercase = "а",
                    nameKo = "아",
                    romanization = "a",
                    pronunciationHint = "아",
                    sortOrder = 1,
                    examples = listOf("арбуз"),
                ),
            ),
            words = words,
        )
    }

    private fun word(id: String, value: String): WordDto {
        return WordDto(
            id = id,
            categoryId = "daily",
            word = value,
            meanings = listOf("인사"),
            active = true,
        )
    }
}
