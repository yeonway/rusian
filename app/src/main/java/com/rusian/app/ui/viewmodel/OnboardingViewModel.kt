package com.rusian.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rusian.app.domain.model.OnboardingConfig
import com.rusian.app.domain.repository.StudyRepository
import com.rusian.app.ui.state.OnboardingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = studyRepository.getCategoryOptions()
            _state.update {
                it.copy(
                    loading = false,
                    categories = categories,
                    selectedCategoryIds = categories.map { c -> c.id }.toSet(),
                )
            }
        }
    }

    fun updateGoal(goal: Int) {
        _state.update { it.copy(dailyGoal = goal.coerceIn(5, 100)) }
    }

    fun toggleCategory(id: String) {
        _state.update { state ->
            val next = state.selectedCategoryIds.toMutableSet()
            if (!next.add(id)) next.remove(id)
            state.copy(selectedCategoryIds = next)
        }
    }

    fun complete() {
        viewModelScope.launch {
            val current = _state.value
            val selected = if (current.selectedCategoryIds.isEmpty()) {
                current.categories.map { it.id }.toSet()
            } else {
                current.selectedCategoryIds
            }
            studyRepository.setOnboarding(
                OnboardingConfig(
                    dailyGoal = current.dailyGoal,
                    selectedCategoryIds = selected,
                ),
            )
            _state.update { it.copy(completed = true) }
        }
    }
}
