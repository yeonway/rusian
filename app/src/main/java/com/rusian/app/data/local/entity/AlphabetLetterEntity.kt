package com.rusian.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alphabet_letter")
data class AlphabetLetterEntity(
    @PrimaryKey val id: String,
    val uppercase: String,
    val lowercase: String,
    val nameKo: String?,
    val romanization: String?,
    val pronunciationHint: String?,
    val letterType: String,
    val soundFeel: String?,
    val confusionGroup: String?,
    val confusionNote: String?,
    val usageFrequency: String?,
    val tmiNote: String?,
    val sortOrder: Int,
    val examplesJson: String,
)
