package com.rusian.app.domain.model

data class AppUpdateInfo(
    val updateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String?,
    val apkName: String?,
    val downloadUrl: String?,
    val message: String,
)

data class AppInstallResult(
    val success: Boolean,
    val message: String,
)
