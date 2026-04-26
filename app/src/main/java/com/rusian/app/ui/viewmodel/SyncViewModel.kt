package com.rusian.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rusian.app.data.preferences.SyncPolicy
import com.rusian.app.data.preferences.UserPreferencesGateway
import com.rusian.app.domain.model.ThemeMode
import com.rusian.app.domain.repository.AppUpdateRepository
import com.rusian.app.domain.repository.BackupRepository
import com.rusian.app.domain.repository.StudyRepository
import com.rusian.app.domain.repository.SyncRepository
import com.rusian.app.ui.state.SyncUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val studyRepository: StudyRepository,
    private val backupRepository: BackupRepository,
    private val preferencesGateway: UserPreferencesGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            syncRepository.observeSyncStatus().collectLatest { status ->
                _state.update { it.copy(status = status) }
            }
        }
        viewModelScope.launch {
            preferencesGateway.preferences.collectLatest { prefs ->
                _state.update {
                    it.copy(
                        themeMode = prefs.themeMode,
                        dailyGoal = prefs.dailyGoal,
                        selectedCategoryIds = prefs.selectedCategoryIds,
                        syncPolicy = prefs.syncPolicy,
                        manifestUrl = prefs.manifestUrl,
                        manifestUrlInput = prefs.manifestUrl,
                        remoteUpdatesConfigured = prefs.manifestUrl.isRemoteManifestConfigured(),
                        showExpressionCards = prefs.showExpressionCards,
                        alphabetQuizOptionCount = prefs.alphabetQuizOptionCount,
                    )
                }
            }
        }
        viewModelScope.launch {
            val categories = studyRepository.getCategoryOptions()
            _state.update { it.copy(categories = categories) }
        }
    }

    fun checkUpdates() {
        if (!_state.value.remoteUpdatesConfigured) {
            _state.update { it.copy(message = REMOTE_UPDATE_DISABLED_MESSAGE) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(checking = true, message = null) }
            val result = syncRepository.checkForUpdates()
            _state.update {
                it.copy(
                    checking = false,
                    updateAvailable = result.updateAvailable,
                    remoteVersion = result.remoteVersion,
                    localWordCount = result.localWordCount,
                    remoteWordCount = result.remoteWordCount,
                    addedWordCount = result.addedWordCount,
                    removedWordCount = result.removedWordCount,
                    message = result.reason,
                )
            }
        }
    }

    fun runIncrementalUpdate() {
        if (!_state.value.remoteUpdatesConfigured) {
            _state.update { it.copy(message = REMOTE_UPDATE_DISABLED_MESSAGE) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(updating = true, message = null) }
            val result = syncRepository.runIncrementalUpdate()
            _state.update {
                it.copy(
                    updating = false,
                    message = result.message,
                    addedWordCount = result.addedWordCount,
                    remoteWordCount = result.totalWordCount,
                    localWordCount = if (result.updated) result.totalWordCount else it.localWordCount,
                )
            }
        }
    }

    fun checkAppUpdate() {
        viewModelScope.launch {
            _state.update { it.copy(checkingAppUpdate = true, message = null) }
            val result = appUpdateRepository.checkLatestRelease()
            val downloaded = result.apkName?.let { appUpdateRepository.isApkDownloaded(it) } ?: false
            _state.update {
                it.copy(
                    checkingAppUpdate = false,
                    appUpdateAvailable = result.updateAvailable,
                    currentAppVersion = result.currentVersion,
                    latestAppVersion = result.latestVersion,
                    appUpdateApkName = result.apkName,
                    appUpdateDownloadUrl = result.downloadUrl,
                    appUpdateDownloaded = downloaded,
                    message = result.message,
                )
            }
        }
    }

    fun downloadAppUpdate() {
        val downloadUrl = _state.value.appUpdateDownloadUrl
        val apkName = _state.value.appUpdateApkName
        if (downloadUrl.isNullOrBlank() || apkName.isNullOrBlank()) {
            _state.update { it.copy(message = "먼저 앱 새 버전 확인을 눌러주세요.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(downloadingAppUpdate = true, message = null) }
            val result = appUpdateRepository.downloadApk(downloadUrl, apkName)
            _state.update {
                it.copy(
                    downloadingAppUpdate = false,
                    appUpdateDownloaded = result.success || appUpdateRepository.isApkDownloaded(apkName),
                    message = result.message,
                )
            }
        }
    }

    fun installDownloadedAppUpdate() {
        val apkName = _state.value.appUpdateApkName
        if (apkName.isNullOrBlank()) {
            _state.update { it.copy(message = "먼저 앱 새 버전 확인을 눌러주세요.") }
            return
        }
        viewModelScope.launch {
            val result = appUpdateRepository.openDownloadedInstaller(apkName)
            _state.update {
                it.copy(
                    appUpdateDownloaded = appUpdateRepository.isApkDownloaded(apkName),
                    message = result.message,
                )
            }
        }
    }

    fun runCleanInstall() {
        viewModelScope.launch {
            _state.update { it.copy(updating = true, message = null) }
            val result = syncRepository.runCleanInstall()
            _state.update { it.copy(updating = false, message = result.message) }
        }
    }

    fun runFactoryReset() {
        viewModelScope.launch {
            _state.update { it.copy(updating = true, message = null) }
            val result = syncRepository.runFactoryReset()
            _state.update { it.copy(updating = false, message = result.message) }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesGateway.setThemeMode(themeMode)
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            preferencesGateway.setDailyGoal(goal)
        }
    }

    fun toggleCategory(categoryId: String) {
        viewModelScope.launch {
            val next = _state.value.selectedCategoryIds.toMutableSet().apply {
                if (contains(categoryId)) remove(categoryId) else add(categoryId)
            }
            preferencesGateway.setSelectedCategoryIds(next)
        }
    }

    fun selectAllCategories() {
        viewModelScope.launch {
            preferencesGateway.setSelectedCategoryIds(_state.value.categories.map { it.id }.toSet())
        }
    }

    fun clearCategoryFilter() {
        viewModelScope.launch {
            preferencesGateway.setSelectedCategoryIds(emptySet())
        }
    }

    fun setSyncPolicy(syncPolicy: SyncPolicy) {
        viewModelScope.launch {
            preferencesGateway.setSyncPolicy(syncPolicy)
        }
    }

    fun onManifestUrlChanged(value: String) {
        _state.update { it.copy(manifestUrlInput = value, message = null) }
    }

    fun saveManifestUrl() {
        viewModelScope.launch {
            preferencesGateway.setManifestUrl(_state.value.manifestUrlInput)
            _state.update { it.copy(message = "업데이트 주소를 저장했습니다.") }
        }
    }

    fun resetManifestUrl() {
        viewModelScope.launch {
            preferencesGateway.setManifestUrl(DEFAULT_MANIFEST_URL)
            _state.update { it.copy(message = "업데이트 주소를 기본값으로 되돌렸습니다.") }
        }
    }

    fun clearLastChatRoom() {
        viewModelScope.launch {
            preferencesGateway.setLastChatConversationId(null)
            _state.update { it.copy(message = "마지막 채팅방 기억을 지웠습니다.") }
        }
    }

    fun setShowExpressionCards(show: Boolean) {
        viewModelScope.launch {
            preferencesGateway.setShowExpressionCards(show)
        }
    }

    fun setAlphabetQuizOptionCount(count: Int) {
        viewModelScope.launch {
            preferencesGateway.setAlphabetQuizOptionCount(count)
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            val result = backupRepository.createSnapshot("manual-settings")
            _state.update {
                it.copy(
                    message = result.fold(
                        onSuccess = { snapshot -> "백업 생성: ${snapshot.path.substringAfterLast('\\')}" },
                        onFailure = { t -> "백업 실패: ${t.message}" },
                    ),
                )
            }
        }
    }

    fun restoreLatestBackup() {
        viewModelScope.launch {
            val result = backupRepository.restoreLatestSnapshot()
            _state.update {
                it.copy(
                    message = if (result.isSuccess) {
                        "최근 백업을 복원했습니다."
                    } else {
                        "복원 실패: ${result.exceptionOrNull()?.message}"
                    },
                )
            }
        }
    }

    private fun String.isRemoteManifestConfigured(): Boolean {
        return isNotBlank() && !contains("example.com", ignoreCase = true)
    }

    private companion object {
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/yeonway/rusian/refs/heads/main/content-pack.json"
        const val REMOTE_UPDATE_DISABLED_MESSAGE = "content-pack 주소가 아직 설정되지 않아 업데이트를 실행할 수 없습니다."
    }
}
