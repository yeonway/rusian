package com.rusian.app.domain.repository

import com.rusian.app.domain.model.AppInstallResult
import com.rusian.app.domain.model.AppUpdateInfo

interface AppUpdateRepository {
    suspend fun checkLatestRelease(): AppUpdateInfo
    suspend fun isApkDownloaded(apkName: String): Boolean
    suspend fun downloadApk(downloadUrl: String, apkName: String): AppInstallResult
    suspend fun openDownloadedInstaller(apkName: String): AppInstallResult
}
