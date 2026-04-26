package com.rusian.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word",
    indices = [Index("categoryId"), Index("active")],
)
data class WordEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val word: String,
    val stress: String?,
    val transliteration: String?,
    val pronunciationKo: String?,
    val partOfSpeech: String?,
    val tone: String,
    val situationHint: String?,
    val usageNote: String?,
    val pairKey: String?,
    val contextTag: String?,
    val difficulty: Int,
    val exampleRu: String?,
    val exampleKo: String?,
    val active: Boolean,
)
