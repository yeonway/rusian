package com.rusian.app.domain.model

data class SyncStatus(
    val lastSyncAt: Long?,
    val currentDatasetVersion: String?,
    val lastMessage: String?,
)

data class UpdateCheckResult(
    val updateAvailable: Boolean,
    val remoteVersion: String?,
    val reason: String,
    val localWordCount: Int = 0,
    val remoteWordCount: Int = 0,
    val addedWordCount: Int = 0,
    val removedWordCount: Int = 0,
)

data class SyncResult(
    val success: Boolean,
    val updated: Boolean,
    val message: String,
    val addedWordCount: Int = 0,
    val totalWordCount: Int = 0,
)
