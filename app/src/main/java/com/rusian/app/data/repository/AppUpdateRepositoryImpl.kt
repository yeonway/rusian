package com.rusian.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.rusian.app.BuildConfig
import com.rusian.app.domain.model.AppInstallResult
import com.rusian.app.domain.model.AppUpdateInfo
import com.rusian.app.domain.repository.AppUpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class AppUpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val json: Json,
) : AppUpdateRepository {

    override suspend fun checkLatestRelease(): AppUpdateInfo = withContext(Dispatchers.IO) {
        runCatching {
            val release = fetchLatestRelease()
            val asset = release.assets.firstOrNull { asset ->
                asset.name.endsWith(".apk", ignoreCase = true) &&
                    asset.browserDownloadUrl.isNotBlank()
            } ?: return@withContext AppUpdateInfo(
                updateAvailable = false,
                currentVersion = BuildConfig.VERSION_NAME,
                latestVersion = release.tagName,
                apkName = null,
                downloadUrl = null,
                message = "릴리즈에 APK 파일이 없습니다.",
            )
            val latestVersion = release.tagName.trim().removePrefix("v")
            val updateAvailable = compareVersions(latestVersion, BuildConfig.VERSION_NAME) > 0
            AppUpdateInfo(
                updateAvailable = updateAvailable,
                currentVersion = BuildConfig.VERSION_NAME,
                latestVersion = release.tagName,
                apkName = asset.name,
                downloadUrl = asset.browserDownloadUrl,
                message = if (updateAvailable) {
                    "새 앱 버전이 있습니다: ${release.tagName}"
                } else {
                    "현재 앱이 최신 버전입니다: ${BuildConfig.VERSION_NAME}"
                },
            )
        }.getOrElse { t ->
            AppUpdateInfo(
                updateAvailable = false,
                currentVersion = BuildConfig.VERSION_NAME,
                latestVersion = null,
                apkName = null,
                downloadUrl = null,
                message = "앱 업데이트 확인 실패: ${t.message}",
            )
        }
    }

    override suspend fun isApkDownloaded(apkName: String): Boolean = withContext(Dispatchers.IO) {
        val apkFile = apkFileFor(apkName)
        apkFile.exists() && apkFile.length() > 0L
    }

    override suspend fun downloadApk(
        downloadUrl: String,
        apkName: String,
    ): AppInstallResult = withContext(Dispatchers.IO) {
        runCatching {
            downloadApkFile(downloadUrl, apkName)
            AppInstallResult(
                success = true,
                message = "업데이트 파일을 다운로드했습니다. 설치하기를 누르면 업데이트됩니다.",
            )
        }.getOrElse { t ->
            AppInstallResult(
                success = false,
                message = "앱 다운로드 실패: ${t.message}",
            )
        }
    }

    override suspend fun openDownloadedInstaller(apkName: String): AppInstallResult = withContext(Dispatchers.IO) {
        runCatching {
            val apkFile = apkFileFor(apkName)
            check(apkFile.exists() && apkFile.length() > 0L) { "다운로드된 APK가 없습니다." }
            openInstaller(apkFile)
            AppInstallResult(
                success = true,
                message = "설치 화면을 열었습니다. 설치를 누르면 업데이트됩니다.",
            )
        }.getOrElse { t ->
            AppInstallResult(
                success = false,
                message = "설치 화면 열기 실패: ${t.message}",
            )
        }
    }

    private fun fetchLatestRelease(): GithubReleaseDto {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "GitHub HTTP ${response.code}" }
            val body = response.body?.string().orEmpty()
            check(body.isNotBlank()) { "GitHub 응답이 비어 있습니다." }
            return json.decodeFromString<GithubReleaseDto>(body)
        }
    }

    private fun downloadApkFile(downloadUrl: String, apkName: String): File {
        val apkFile = apkFileFor(apkName)
        val request = Request.Builder()
            .url(downloadUrl)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "APK 다운로드 HTTP ${response.code}" }
            val body = response.body ?: error("APK 응답이 비어 있습니다.")
            apkFile.outputStream().use { output ->
                body.byteStream().use { input -> input.copyTo(output) }
            }
        }
        check(apkFile.length() > 0L) { "다운로드된 APK가 비어 있습니다." }
        return apkFile
    }

    private fun apkFileFor(apkName: String): File {
        val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val safeName = apkName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(updateDir, safeName)
    }

    private fun openInstaller(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            error("권한 화면을 열었습니다. '이 출처의 앱 설치 허용'을 켠 뒤 다시 다운로드/설치를 눌러주세요.")
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val installIntent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(installIntent)
    }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val rightParts = right.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until size) {
            val diff = leftParts.getOrElse(index) { 0 } - rightParts.getOrElse(index) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }

    @Serializable
    private data class GithubReleaseDto(
        @SerialName("tag_name") val tagName: String,
        val assets: List<GithubAssetDto> = emptyList(),
    )

    @Serializable
    private data class GithubAssetDto(
        val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
    )

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/yeonway/rusian/releases/latest"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
