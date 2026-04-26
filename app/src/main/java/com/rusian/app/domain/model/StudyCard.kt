package com.rusian.app.domain.model

data class StudyCard(
    val studyItemId: Long,
    val kind: StudyKind,
    val remoteStableId: String,
    val title: String,
    val subtitle: String?,
    val hint: String?,
    val structureLabel: String?,
    val soundFeel: String?,
    val confusionNote: String?,
    val usageFrequency: String?,
    val tmiNote: String?,
    val tone: String?,
    val situationHint: String?,
    val usageNote: String?,
    val contextTag: String?,
    val meanings: List<String>,
    val exampleRu: String?,
    val exampleKo: String?,
    val dueAt: Long,
)
