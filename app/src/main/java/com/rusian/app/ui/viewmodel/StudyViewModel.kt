package com.rusian.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rusian.app.domain.model.ReviewRating
import com.rusian.app.domain.model.StudyCard
import com.rusian.app.domain.model.StudyKind
import com.rusian.app.domain.repository.StudyRepository
import com.rusian.app.ui.state.StudyUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    private var mode: StudyKind = StudyKind.WORD
    private val weakLetterIds = mutableSetOf<String>()
    private val weakRequeueCounts = mutableMapOf<String, Int>()
    private val wordRequeueCounts = mutableMapOf<String, Int>()

    fun load(mode: StudyKind) {
        this.mode = mode
        weakLetterIds.clear()
        weakRequeueCounts.clear()
        wordRequeueCounts.clear()
        viewModelScope.launch {
            _state.value = StudyUiState(loading = true)
            val cardLimit = if (mode == StudyKind.ALPHABET) ALPHABET_SESSION_LIMIT else DEFAULT_SESSION_LIMIT
            val cards = studyRepository.getDueCards(mode = mode, limit = cardLimit)
            _state.value = StudyUiState(
                loading = false,
                cards = cards,
                message = if (cards.isEmpty()) "오늘 복습할 항목이 없습니다." else null,
            )
        }
    }

    fun revealAnswer() {
        _state.update { it.copy(revealed = true) }
    }

    fun hideAnswer() {
        _state.update { it.copy(revealed = false) }
    }

    fun nextCard() {
        val current = _state.value
        if (current.cards.isEmpty()) return
        val next = current.currentIndex + 1
        if (next > current.cards.lastIndex) {
            load(mode)
            return
        }
        _state.update {
            it.copy(
                currentIndex = next,
                revealed = false,
                lastEvaluatedStableId = null,
                lastAlphabetResult = null,
            )
        }
    }

    fun previousCard() {
        val current = _state.value
        if (current.cards.isEmpty()) return
        val prev = (current.currentIndex - 1).coerceAtLeast(0)
        _state.update {
            it.copy(
                currentIndex = prev,
                revealed = false,
                lastEvaluatedStableId = null,
                lastAlphabetResult = null,
            )
        }
    }

    fun recordAlphabetQuizResult(remoteStableId: String, correct: Boolean) {
        val current = _state.value
        if (current.lastEvaluatedStableId == remoteStableId) return
        val card = current.cards.firstOrNull { it.remoteStableId == remoteStableId } ?: return

        _state.update { state ->
            val answered = state.sessionAnswered + 1
            val correctCount = state.sessionCorrect + if (correct) 1 else 0
            val streak = if (correct) state.currentStreak + 1 else 0
            state.copy(
                sessionAnswered = answered,
                sessionCorrect = correctCount,
                currentStreak = streak,
                bestStreak = max(state.bestStreak, streak),
                lastAlphabetResult = correct,
                lastEvaluatedStableId = remoteStableId,
            )
        }

        if (!correct) {
            weakLetterIds += remoteStableId
            requeueWeakAlphabetCard(remoteStableId)
            _state.update {
                it.copy(
                    weakLetterCount = weakLetterIds.size,
                    weakLetterIds = weakLetterIds.toSet(),
                )
            }
        }

        viewModelScope.launch {
            studyRepository.submitReview(
                studyItemId = card.studyItemId,
                rating = if (correct) ReviewRating.GOOD else ReviewRating.AGAIN,
                reviewedAt = System.currentTimeMillis(),
            )
        }
    }

    fun submit(rating: ReviewRating) {
        val current = _state.value
        val currentCard = current.currentCard ?: return
        val updatedCards = buildWordRequeuedCards(current, currentCard.remoteStableId)
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= updatedCards.size) {
            viewModelScope.launch {
                studyRepository.submitReview(
                    studyItemId = currentCard.studyItemId,
                    rating = rating,
                    reviewedAt = System.currentTimeMillis(),
                )
                load(mode)
            }
            return
        }

        _state.update {
            it.copy(
                cards = updatedCards,
                currentIndex = nextIndex,
                revealed = false,
                message = null,
                lastEvaluatedStableId = null,
                lastAlphabetResult = null,
            )
        }
        viewModelScope.launch {
            studyRepository.submitReview(
                studyItemId = currentCard.studyItemId,
                rating = rating,
                reviewedAt = System.currentTimeMillis(),
            )
        }
    }

    private fun requeueWeakAlphabetCard(remoteStableId: String) {
        if (mode != StudyKind.ALPHABET) return
        val current = _state.value
        val card = current.cards.firstOrNull { it.remoteStableId == remoteStableId } ?: return
        val requeueCount = weakRequeueCounts[remoteStableId] ?: 0
        if (requeueCount >= 2) return

        val lookAheadStart = (current.currentIndex + 1).coerceAtMost(current.cards.size)
        val lookAheadEnd = (lookAheadStart + 3).coerceAtMost(current.cards.size)
        val alreadySoon = current.cards.subList(lookAheadStart, lookAheadEnd)
            .any { it.remoteStableId == remoteStableId }
        if (alreadySoon) return

        val insertAt = (current.currentIndex + 2).coerceAtMost(current.cards.size)
        val updatedCards = current.cards.toMutableList().apply { add(insertAt, card) }
        weakRequeueCounts[remoteStableId] = requeueCount + 1
        _state.update { it.copy(cards = updatedCards) }
    }

    private fun buildWordRequeuedCards(current: StudyUiState, remoteStableId: String): List<StudyCard> {
        if (mode != StudyKind.WORD) return current.cards
        val card = current.cards.firstOrNull { it.remoteStableId == remoteStableId } ?: return current.cards
        val requeueCount = wordRequeueCounts[remoteStableId] ?: 0
        if (requeueCount >= 3) return current.cards

        val remaining = current.cards.lastIndex - current.currentIndex
        if (remaining < 3) return current.cards

        val lookAheadStart = (current.currentIndex + 1).coerceAtMost(current.cards.size)
        val lookAheadEnd = (lookAheadStart + 2).coerceAtMost(current.cards.size)
        val alreadySoon = current.cards.subList(lookAheadStart, lookAheadEnd)
            .any { it.remoteStableId == remoteStableId }
        if (alreadySoon) return current.cards

        val shouldRepeat = current.currentIndex % 2 == 0
        if (!shouldRepeat) return current.cards

        val insertAt = (current.currentIndex + 3).coerceAtMost(current.cards.size)
        wordRequeueCounts[remoteStableId] = requeueCount + 1
        return current.cards.toMutableList().apply { add(insertAt, card) }
    }

    private companion object {
        const val DEFAULT_SESSION_LIMIT = 20
        const val ALPHABET_SESSION_LIMIT = 40
    }
}
