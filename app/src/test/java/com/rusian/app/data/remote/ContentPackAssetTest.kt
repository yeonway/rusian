package com.rusian.app.data.remote

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentPackAssetTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `content pack asset is valid utf8 json`() {
        val file = File("src/main/assets/content-pack.json")
        val pack = json.decodeFromString<ContentPackDto>(file.readText(Charsets.UTF_8))

        assertEquals(2, pack.schemaVersion)
        assertEquals("2026.04.23-words150", pack.datasetVersion)
        assertEquals("ru", pack.language)
        assertEquals("ko", pack.glossLanguage)
        assertTrue(pack.categories.isNotEmpty())
        assertTrue(pack.alphabet.isNotEmpty())
        assertTrue(pack.words.isNotEmpty())
    }
}
