package com.rusian.app.data.repository

import com.rusian.app.data.local.dao.DatasetInfoDao
import com.rusian.app.data.local.dao.ReviewEventDao
import com.rusian.app.data.local.dao.ReviewStateDao
import com.rusian.app.data.local.dao.WordDao
import com.rusian.app.data.preferences.UserPreferencesGateway
import com.rusian.app.data.remote.ContentPackDto
import com.rusian.app.data.remote.ManifestDto
import com.rusian.app.data.remote.RemoteSyncDataSource
import com.rusian.app.data.remote.SeedContentDataSource
import com.rusian.app.domain.model.SyncResult
import com.rusian.app.domain.model.SyncStatus
import com.rusian.app.domain.model.UpdateCheckResult
import com.rusian.app.domain.repository.BackupRepository
import com.rusian.app.domain.repository.SyncRepository
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val datasetInfoDao: DatasetInfoDao,
    private val wordDao: WordDao,
    private val reviewStateDao: ReviewStateDao,
    private val reviewEventDao: ReviewEventDao,
    private val preferencesDataSource: UserPreferencesGateway,
    private val remoteDataSource: RemoteSyncDataSource,
    private val assetDataSource: SeedContentDataSource,
    private val contentInstaller: ContentInstallService,
    private val backupRepository: BackupRepository,
) : SyncRepository {

    private val lastMessage = MutableStateFlow<String?>(null)

    override fun observeSyncStatus(): Flow<SyncStatus> {
        return combine(
            datasetInfoDao.observe(),
            preferencesDataSource.preferences,
            lastMessage,
        ) { dataset, prefs, message ->
            SyncStatus(
                lastSyncAt = prefs.lastSyncAt,
                currentDatasetVersion = dataset?.datasetVersion,
                lastMessage = message,
            )
        }
    }

    override suspend fun checkForUpdates(): UpdateCheckResult {
        val prefs = preferencesDataSource.preferences.first()
        val local = datasetInfoDao.get()
        return try {
            val remote = fetchConfiguredRemoteContent(prefs.manifestUrl)
            val changeStats = calculateWordChangeStats(remote.pack)
            val updateAvailable = local?.datasetVersion != remote.datasetVersion ||
                !local.sha256.equals(remote.sha256, ignoreCase = true)
            UpdateCheckResult(
                updateAvailable = updateAvailable,
                remoteVersion = remote.datasetVersion,
                reason = buildUpdateCheckMessage(updateAvailable, changeStats),
                localWordCount = changeStats.localWordCount,
                remoteWordCount = changeStats.remoteWordCount,
                addedWordCount = changeStats.addedWordCount,
                removedWordCount = changeStats.removedWordCount,
            )
        } catch (t: Throwable) {
            UpdateCheckResult(
                updateAvailable = false,
                remoteVersion = null,
                reason = "서버 연결 실패: ${t.message}",
            )
        }
    }

    override suspend fun runIncrementalUpdate(): SyncResult {
        val prefs = preferencesDataSource.preferences.first()
        return try {
            val local = datasetInfoDao.get()
            val remote = fetchConfiguredRemoteContent(prefs.manifestUrl)
            val changeStats = calculateWordChangeStats(remote.pack)
            val shouldSkip = local?.datasetVersion == remote.datasetVersion &&
                local.sha256.equals(remote.sha256, ignoreCase = true)
            if (shouldSkip) {
                val msg = "변경 사항이 없습니다. 현재 단어 ${changeStats.localWordCount}개."
                lastMessage.value = msg
                return SyncResult(
                    success = true,
                    updated = false,
                    message = msg,
                    addedWordCount = 0,
                    totalWordCount = changeStats.localWordCount,
                )
            }
            installRemoteContent(remote, cleanInstall = false)
            val msg = "업데이트 완료: ${remote.datasetVersion} · 새 단어 ${changeStats.addedWordCount}개 · 전체 ${changeStats.remoteWordCount}개"
            lastMessage.value = msg
            SyncResult(
                success = true,
                updated = true,
                message = msg,
                addedWordCount = changeStats.addedWordCount,
                totalWordCount = changeStats.remoteWordCount,
            )
        } catch (t: Throwable) {
            val msg = "업데이트 실패: ${t.message}"
            lastMessage.value = msg
            SyncResult(success = false, updated = false, message = msg)
        }
    }

    override suspend fun runCleanInstall(): SyncResult {
        val backup = backupRepository.createSnapshot("clean-install")
        if (backup.isFailure) {
            val message = "백업 실패로 중단: ${backup.exceptionOrNull()?.message}"
            lastMessage.value = message
            return SyncResult(success = false, updated = false, message = message)
        }

        return try {
            val prefs = preferencesDataSource.preferences.first()
            val remote = fetchConfiguredRemoteContent(prefs.manifestUrl)
            installRemoteContent(remote, cleanInstall = true)
            val msg = "콘텐츠 새로 설치 완료: ${remote.datasetVersion}"
            lastMessage.value = msg
            SyncResult(success = true, updated = true, message = msg)
        } catch (_: Throwable) {
            val localPack = assetDataSource.loadSeedContentPack()
            contentInstaller.install(
                pack = localPack,
                source = "asset-clean-install",
                sha256 = null,
                cleanInstall = true,
                installedAt = System.currentTimeMillis(),
            )
            val msg = "서버에 연결할 수 없어 로컬 시드로 콘텐츠를 새로 설치했습니다."
            lastMessage.value = msg
            SyncResult(success = true, updated = true, message = msg)
        }
    }

    override suspend fun runFactoryReset(): SyncResult {
        val backup = backupRepository.createSnapshot("factory-reset")
        if (backup.isFailure) {
            val message = "백업 실패로 초기화를 중단했습니다."
            lastMessage.value = message
            return SyncResult(success = false, updated = false, message = message)
        }

        reviewStateDao.clear()
        reviewEventDao.clear()
        preferencesDataSource.setFactoryResetArmed(false)
        val message = "초기화 완료: 콘텐츠는 보존하고 학습 진행률만 초기화했습니다."
        lastMessage.value = message
        return SyncResult(success = true, updated = false, message = message)
    }

    private suspend fun fetchConfiguredRemoteContent(url: String): RemoteContent {
        return if (url.contains("content-pack", ignoreCase = true)) {
            val (pack, bytes) = remoteDataSource.fetchContentPack(url)
            RemoteContent(pack = pack, sha256 = sha256(bytes), datasetVersion = pack.datasetVersion)
        } else {
            val manifest = remoteDataSource.fetchManifest(url)
            val (pack, bytes) = remoteDataSource.fetchContentPack(manifest.contentPackUrl)
            val actualSha = sha256(bytes)
            if (!manifest.sha256.isNullOrBlank()) {
                check(actualSha.equals(manifest.sha256, ignoreCase = true)) {
                    "체크섬이 일치하지 않습니다."
                }
            }
            RemoteContent(pack = pack, sha256 = actualSha, datasetVersion = manifest.datasetVersion)
        }
    }

    private suspend fun installRemoteContent(remote: RemoteContent, cleanInstall: Boolean) {
        contentInstaller.install(
            pack = remote.pack,
            source = "remote-content-pack",
            sha256 = remote.sha256,
            cleanInstall = cleanInstall,
            installedAt = System.currentTimeMillis(),
        )
        preferencesDataSource.setLastSyncAt(System.currentTimeMillis())
    }

    private suspend fun calculateWordChangeStats(pack: ContentPackDto): WordChangeStats {
        val localIds = wordDao.getActiveIds().toSet()
        val remoteIds = pack.words.filter { it.active }.map { it.id }.toSet()
        return WordChangeStats(
            localWordCount = localIds.size,
            remoteWordCount = remoteIds.size,
            addedWordCount = (remoteIds - localIds).size,
            removedWordCount = (localIds - remoteIds).size,
        )
    }

    private fun buildUpdateCheckMessage(updateAvailable: Boolean, stats: WordChangeStats): String {
        return if (updateAvailable) {
            "새 데이터셋이 있습니다. 새 단어 ${stats.addedWordCount}개, 전체 ${stats.remoteWordCount}개."
        } else {
            "최신 데이터셋입니다. 현재 단어 ${stats.localWordCount}개."
        }
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder()
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private data class RemoteContent(
        val pack: ContentPackDto,
        val sha256: String,
        val datasetVersion: String,
    )

    private data class WordChangeStats(
        val localWordCount: Int,
        val remoteWordCount: Int,
        val addedWordCount: Int,
        val removedWordCount: Int,
    )
}
