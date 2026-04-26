package com.rusian.app.data.local.model

import com.rusian.app.domain.model.StudyKind

data class StudyCardRow(
    val studyItemId: Long,
    val kind: StudyKind,
    val remoteStableId: String,
    val dueAt: Long?,
    val categoryId: String?,
    val word: String?,
    val stress: String?,
    val transliteration: String?,
    val pronunciationKo: String?,
    val wordTone: String?,
    val situationHint: String?,
    val usageNote: String?,
    val contextTag: String?,
    val exampleRu: String?,
    val exampleKo: String?,
    val uppercase: String?,
    val lowercase: String?,
    val letterRomanization: String?,
    val letterHint: String?,
    val letterType: String?,
    val letterSoundFeel: String?,
    val letterConfusionNote: String?,
    val letterUsageFrequency: String?,
    val letterTmiNote: String?,
)
