package com.rusian.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "word_gloss",
    primaryKeys = ["wordId", "locale", "meaningOrder"],
    indices = [Index("wordId")],
)
data class WordGlossEntity(
    val wordId: String,
    val locale: String,
    val meaningOrder: Int,
    val meaning: String,
)
