package com.rusian.app.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Singleton
class AssetContentDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) : SeedContentDataSource {
    override fun loadSeedContentPack(assetName: String): ContentPackDto {
        val content = try {
            context.assets.open(assetName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw ContentLoadException("Failed to read asset '$assetName'.", e)
        }
        return try {
            json.decodeFromString(content)
        } catch (e: SerializationException) {
            throw ContentLoadException("Failed to parse '$assetName': ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw ContentLoadException("Failed to parse '$assetName': ${e.message}", e)
        }
    }
}

class ContentLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)
