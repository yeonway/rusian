package com.rusian.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ManifestDto(
    val schemaVersion: Int,
    val datasetVersion: String,
    val minAppVersion: String,
    val contentPackUrl: String,
    val sha256: String? = null,
    val publishedAt: String,
)

@Serializable
data class ContentPackDto(
    val schemaVersion: Int,
    val datasetVersion: String,
    val language: String,
    val glossLanguage: String,
    val categories: List<CategoryDto>,
    val alphabet: List<AlphabetLetterDto>,
    val words: List<WordDto>,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val sortOrder: Int,
)

@Serializable
data class AlphabetLetterDto(
    val id: String,
    val uppercase: String,
    val lowercase: String,
    val nameKo: String? = null,
    val romanization: String? = null,
    val pronunciationHint: String? = null,
    val letterType: String = "CONSONANT",
    val soundFeel: String? = null,
    val confusionGroup: String? = null,
    val confusionNote: String? = null,
    val usageFrequency: String? = null,
    val tmiNote: String? = null,
    val sortOrder: Int,
    val examples: List<String> = emptyList(),
)

@Serializable
data class WordDto(
    val id: String,
    val categoryId: String,
    val word: String,
    val stress: String? = null,
    val transliteration: String? = null,
    val pronunciationKo: String? = null,
    val partOfSpeech: String? = null,
    val tone: String = "NEUTRAL",
    val situationHint: String? = null,
    val usageNote: String? = null,
    val pairKey: String? = null,
    val contextTag: String? = null,
    val meanings: List<String>,
    val example: WordExampleDto? = null,
    val difficulty: Int = 1,
    val tags: List<String> = emptyList(),
    val active: Boolean = true,
)

@Serializable
data class WordExampleDto(
    val ru: String? = null,
    val ko: String? = null,
)
