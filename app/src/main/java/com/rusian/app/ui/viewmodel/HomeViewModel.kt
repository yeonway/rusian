package com.rusian.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rusian.app.domain.model.StudyKind
import com.rusian.app.domain.repository.StudyRepository
import com.rusian.app.ui.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val total = studyRepository.countDueCards(mode = null)
            val alphabet = studyRepository.countDueCards(mode = StudyKind.ALPHABET)
            val words = studyRepository.countDueCards(mode = StudyKind.WORD)
            val progress = studyRepository.getCategoryProgress()
            _state.value = HomeUiState(
                loading = false,
                dueTotal = total,
                dueAlphabet = alphabet,
                dueWords = words,
                categoryProgress = progress,
            )
        }
    }
}
