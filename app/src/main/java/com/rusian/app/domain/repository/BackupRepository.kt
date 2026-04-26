package com.rusian.app.domain.repository

data class BackupSnapshot(
    val path: String,
    val createdAt: Long,
)

interface BackupRepository {
    suspend fun createSnapshot(reason: String): Result<BackupSnapshot>
    suspend fun restoreLatestSnapshot(): Result<Unit>
}
