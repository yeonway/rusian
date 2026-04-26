package com.rusian.app.ui.state

import com.rusian.app.domain.model.StudyCard

data class StudyUiState(
    val loading: Boolean = true,
    val cards: List<StudyCard> = emptyList(),
    val currentIndex: Int = 0,
    val revealed: Boolean = false,
    val message: String? = null,
    val sessionAnswered: Int = 0,
    val sessionCorrect: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val weakLetterCount: Int = 0,
    val weakLetterIds: Set<String> = emptySet(),
    val lastAlphabetResult: Boolean? = null,
    val lastEvaluatedStableId: String? = null,
) {
    val currentCard: StudyCard?
        get() = cards.getOrNull(currentIndex)
}
