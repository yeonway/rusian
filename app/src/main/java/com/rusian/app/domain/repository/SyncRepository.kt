package com.rusian.app.domain.repository

import com.rusian.app.domain.model.SyncResult
import com.rusian.app.domain.model.SyncStatus
import com.rusian.app.domain.model.UpdateCheckResult
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    fun observeSyncStatus(): Flow<SyncStatus>
    suspend fun checkForUpdates(): UpdateCheckResult
    suspend fun runIncrementalUpdate(): SyncResult
    suspend fun runCleanInstall(): SyncResult
    suspend fun runFactoryReset(): SyncResult
}
