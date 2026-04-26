package com.rusian.app.data.remote

interface RemoteSyncDataSource {
    suspend fun fetchManifest(url: String): ManifestDto
    suspend fun fetchContentPack(url: String): Pair<ContentPackDto, ByteArray>
}

interface SeedContentDataSource {
    fun loadSeedContentPack(assetName: String = "content-pack.json"): ContentPackDto
}
