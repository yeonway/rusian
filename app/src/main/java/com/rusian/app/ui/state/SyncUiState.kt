package com.rusian.app.ui.state

import com.rusian.app.domain.model.ThemeMode
import com.rusian.app.data.preferences.SyncPolicy
import com.rusian.app.domain.model.CategoryOption
import com.rusian.app.domain.model.SyncStatus

data class SyncUiState(
    val status: SyncStatus = SyncStatus(
        lastSyncAt = null,
        currentDatasetVersion = null,
        lastMessage = null,
    ),
    val checking: Boolean = false,
    val updating: Boolean = false,
    val message: String? = null,
    val updateAvailable: Boolean = false,
    val remoteVersion: String? = null,
    val localWordCount: Int = 0,
    val remoteWordCount: Int = 0,
    val addedWordCount: Int = 0,
    val removedWordCount: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dailyGoal: Int = 20,
    val selectedCategoryIds: Set<String> = emptySet(),
    val categories: List<CategoryOption> = emptyList(),
    val syncPolicy: SyncPolicy = SyncPolicy.WIFI_OR_CHARGING,
    val manifestUrl: String = "",
    val manifestUrlInput: String = "",
    val remoteUpdatesConfigured: Boolean = false,
    val showExpressionCards: Boolean = false,
    val alphabetQuizOptionCount: Int = 4,
    val checkingAppUpdate: Boolean = false,
    val downloadingAppUpdate: Boolean = false,
    val appUpdateAvailable: Boolean = false,
    val currentAppVersion: String = "",
    val latestAppVersion: String? = null,
    val appUpdateApkName: String? = null,
    val appUpdateDownloadUrl: String? = null,
    val appUpdateDownloaded: Boolean = false,
)
