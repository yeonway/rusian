package com.rusian.app.data.repository

import com.rusian.app.data.local.dao.DatasetInfoDao
import com.rusian.app.data.local.dao.ReviewEventDao
import com.rusian.app.data.local.dao.ReviewStateDao
import com.rusian.app.data.local.entity.DatasetInfoEntity
import com.rusian.app.data.local.entity.ReviewEventEntity
import com.rusian.app.data.local.entity.ReviewStateEntity
import com.rusian.app.data.preferences.SyncPolicy
import com.rusian.app.data.preferences.UserPreferences
import com.rusian.app.data.preferences.UserPreferencesGateway
import com.rusian.app.data.remote.ContentPackDto
import com.rusian.app.data.remote.ManifestDto
import com.rusian.app.data.remote.RemoteSyncDataSource
import com.rusian.app.data.remote.SeedContentDataSource
import com.rusian.app.domain.repository.BackupRepository
import com.rusian.app.domain.repository.BackupSnapshot
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRepositoryImplTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun `checkForUpdates returns no update when same version`() = runTest {
        val datasetDao = FakeDatasetInfoDao(
            DatasetInfoEntity(
                schemaVersion = 1,
                datasetVersion = "2026.04.23",
                sha256 = null,
                installedAt = 1L,
                source = "test",
            ),
        )
        val repository = buildRepository(
            datasetDao = datasetDao,
            remote = FakeRemote(
                manifest = fakeManifest("2026.04.23", "https://example.com/content-pack.json", "aaa"),
                contentPair = fakeContentPair(),
            ),
        )

        val result = repository.checkForUpdates()

        assertFalse(result.updateAvailable)
    }

    @Test
    fun `runIncrementalUpdate installs when new version and checksum matches`() = runTest {
        val contentPair = fakeContentPair()
        val checksum = sha256(contentPair.second)
        val installer = FakeInstaller()
        val preferences = FakePreferences()
        val repository = buildRepository(
            datasetDao = FakeDatasetInfoDao(
                DatasetInfoEntity(
                    schemaVersion = 1,
                    datasetVersion = "2026.04.01",
                    sha256 = null,
                    installedAt = 1L,
                    source = "test",
                ),
            ),
            remote = FakeRemote(
                manifest = fakeManifest("2026.04.23", "https://example.com/content-pack.json", checksum),
                contentPair = contentPair,
            ),
            installer = installer,
            preferences = preferences,
        )

        val result = repository.runIncrementalUpdate()

        assertTrue(result.success)
        assertTrue(result.updated)
        assertEquals(1, installer.installCalls)
        assertTrue(preferences.preferences.first().lastSyncAt != null)
    }

    @Test
    fun `runIncrementalUpdate fails on checksum mismatch`() = runTest {
        val installer = FakeInstaller()
        val repository = buildRepository(
            remote = FakeRemote(
                manifest = fakeManifest("2026.04.23", "https://example.com/content-pack.json", "deadbeef"),
                contentPair = fakeContentPair(),
            ),
            installer = installer,
        )

        val result = repository.runIncrementalUpdate()

        assertFalse(result.success)
        assertEquals(0, installer.installCalls)
    }

    @Test
    fun `runCleanInstall falls back to local seed when server unavailable`() = runTest {
        val installer = FakeInstaller()
        val repository = buildRepository(
            remote = object : RemoteSyncDataSource {
                override suspend fun fetchManifest(url: String): ManifestDto {
                    error("network down")
                }
                override suspend fun fetchContentPack(url: String): Pair<ContentPackDto, ByteArray> {
                    error("network down")
                }
            },
            installer = installer,
        )

        val result = repository.runCleanInstall()

        assertTrue(result.success)
        assertTrue(result.updated)
        assertEquals(1, installer.installCalls)
        assertEquals("asset-clean-install", installer.lastSource)
        assertTrue(installer.lastCleanInstall)
    }

    private fun buildRepository(
        datasetDao: DatasetInfoDao = FakeDatasetInfoDao(null),
        reviewStateDao: ReviewStateDao = FakeReviewStateDao(),
        reviewEventDao: ReviewEventDao = FakeReviewEventDao(),
        preferences: FakePreferences = FakePreferences(),
        remote: RemoteSyncDataSource = FakeRemote(fakeManifest("2026.04.23", "https://example.com/content-pack.json", "x"), fakeContentPair()),
        seed: SeedContentDataSource = object : SeedContentDataSource {
            override fun loadSeedContentPack(assetName: String): ContentPackDto = fakeContentPair().first
        },
        installer: FakeInstaller = FakeInstaller(),
        backup: BackupRepository = FakeBackupRepository(),
    ): SyncRepositoryImpl {
        return SyncRepositoryImpl(
            datasetInfoDao = datasetDao,
            reviewStateDao = reviewStateDao,
            reviewEventDao = reviewEventDao,
            preferencesDataSource = preferences,
            remoteDataSource = remote,
            assetDataSource = seed,
            contentInstaller = installer,
            backupRepository = backup,
        )
    }

    private fun fakeManifest(version: String, url: String, sha: String): ManifestDto {
        return ManifestDto(
            schemaVersion = 1,
            datasetVersion = version,
            minAppVersion = "1.0.0",
            contentPackUrl = url,
            sha256 = sha,
            publishedAt = "2026-04-23T00:00:00Z",
        )
    }

    private fun fakeContentPair(): Pair<ContentPackDto, ByteArray> {
        val pack = ContentPackDto(
            schemaVersion = 1,
            datasetVersion = "2026.04.23",
            language = "ru",
            glossLanguage = "ko",
            categories = emptyList(),
            alphabet = emptyList(),
            words = emptyList(),
        )
        val bytes = json.encodeToString(ContentPackDto.serializer(), pack).toByteArray()
        return pack to bytes
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private class FakeDatasetInfoDao(initial: DatasetInfoEntity?) : DatasetInfoDao {
        private var value: DatasetInfoEntity? = initial
        private val flow = MutableStateFlow(initial)
        override suspend fun upsert(item: DatasetInfoEntity) {
            value = item
            flow.value = item
        }
        override suspend fun get(): DatasetInfoEntity? = value
        override fun observe(): Flow<DatasetInfoEntity?> = flow
        override suspend fun clear() {
            value = null
            flow.value = null
        }
    }

    private class FakeReviewStateDao : ReviewStateDao {
        override suspend fun upsert(item: ReviewStateEntity) = Unit
        override suspend fun upsertAll(items: List<ReviewStateEntity>) = Unit
        override suspend fun findByStudyItemId(studyItemId: Long): ReviewStateEntity? = null
        override suspend fun getAll(): List<ReviewStateEntity> = emptyList()
        override suspend fun clear() = Unit
    }

    private class FakeReviewEventDao : ReviewEventDao {
        override suspend fun insert(item: ReviewEventEntity) = Unit
        override suspend fun insertAll(items: List<ReviewEventEntity>) = Unit
        override suspend fun getAll(): List<ReviewEventEntity> = emptyList()
        override suspend fun clear() = Unit
    }

    private class FakePreferences : UserPreferencesGateway {
        private val state = MutableStateFlow(
            UserPreferences(
                onboardingCompleted = true,
                dailyGoal = 20,
                selectedCategoryIds = emptySet(),
                lastSyncAt = null,
                syncPolicy = SyncPolicy.WIFI_OR_CHARGING,
                manifestUrl = "https://example.com/manifest.json",
                factoryResetArmed = false,
                themeMode = com.rusian.app.domain.model.ThemeMode.SYSTEM,
                lastChatConversationId = null,
            ),
        )
        override val preferences: Flow<UserPreferences> = state
        override suspend fun completeOnboarding(dailyGoal: Int, selectedCategoryIds: Set<String>) = Unit
        override suspend fun setLastSyncAt(timestamp: Long) {
            state.value = state.value.copy(lastSyncAt = timestamp)
        }
        override suspend fun setFactoryResetArmed(armed: Boolean) {
            state.value = state.value.copy(factoryResetArmed = armed)
        }
        override suspend fun setThemeMode(themeMode: com.rusian.app.domain.model.ThemeMode) {
            state.value = state.value.copy(themeMode = themeMode)
        }
        override suspend fun setLastChatConversationId(conversationId: Long?) {
            state.value = state.value.copy(lastChatConversationId = conversationId)
        }
    }

    private class FakeRemote(
        private val manifest: ManifestDto,
        private val contentPair: Pair<ContentPackDto, ByteArray>,
    ) : RemoteSyncDataSource {
        override suspend fun fetchManifest(url: String): ManifestDto = manifest
        override suspend fun fetchContentPack(url: String): Pair<ContentPackDto, ByteArray> = contentPair
    }

    private class FakeInstaller : ContentInstallService {
        var installCalls: Int = 0
        var lastSource: String? = null
        var lastCleanInstall: Boolean = false
        override suspend fun install(
            pack: ContentPackDto,
            source: String,
            sha256: String?,
            cleanInstall: Boolean,
            installedAt: Long,
        ) {
            installCalls += 1
            lastSource = source
            lastCleanInstall = cleanInstall
        }
    }

    private class FakeBackupRepository : BackupRepository {
        override suspend fun createSnapshot(reason: String): Result<BackupSnapshot> {
            return Result.success(BackupSnapshot(path = "/tmp", createdAt = 1L))
        }
        override suspend fun restoreLatestSnapshot(): Result<Unit> = Result.success(Unit)
    }
}
