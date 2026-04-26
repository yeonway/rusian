package com.rusian.app.data.repository

import com.rusian.app.data.remote.ContentPackDto

interface ContentInstallService {
    suspend fun install(
        pack: ContentPackDto,
        source: String,
        sha256: String?,
        cleanInstall: Boolean,
        installedAt: Long,
    )

    suspend fun installAlphabetOnly(
        pack: ContentPackDto,
        installedAt: Long,
    )
}
