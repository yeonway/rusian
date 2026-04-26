package com.rusian.app.data.remote

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class RemoteContentDataSource @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) : RemoteSyncDataSource {
    override suspend fun fetchManifest(url: String): ManifestDto {
        return get(url) { body -> json.decodeFromString<ManifestDto>(body) }
    }

    override suspend fun fetchContentPack(url: String): Pair<ContentPackDto, ByteArray> {
        val bytes = getBytes(url)
        val body = bytes.decodeToString()
        val pack = json.decodeFromString<ContentPackDto>(body)
        return pack to bytes
    }

    private fun <T> get(url: String, parser: (String) -> T): T {
        val body = getText(url)
        return parser(body)
    }

    private fun getText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            return response.body?.string() ?: throw IOException("Empty body for $url")
        }
    }

    private fun getBytes(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            return response.body?.bytes() ?: throw IOException("Empty body for $url")
        }
    }
}
