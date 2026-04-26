package com.rusian.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rusian.app.data.preferences.UserPreferencesGateway
import com.rusian.app.domain.model.ThemeMode
import com.rusian.app.domain.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val preferencesGateway: UserPreferencesGateway,
) : ViewModel() {

    private val _onboardingDone = MutableStateFlow<Boolean?>(null)
    val onboardingDone: StateFlow<Boolean?> = _onboardingDone.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _showExpressionCards = MutableStateFlow(false)
    val showExpressionCards: StateFlow<Boolean> = _showExpressionCards.asStateFlow()

    private val _alphabetQuizOptionCount = MutableStateFlow(4)
    val alphabetQuizOptionCount: StateFlow<Int> = _alphabetQuizOptionCount.asStateFlow()

    init {
        viewModelScope.launch {
            studyRepository.ensureSeedImported()
            studyRepository.observeOnboardingDone().collectLatest {
                _onboardingDone.value = it
            }
        }
        viewModelScope.launch {
            preferencesGateway.preferences.collectLatest { prefs ->
                _themeMode.value = prefs.themeMode
                _showExpressionCards.value = prefs.showExpressionCards
                _alphabetQuizOptionCount.value = prefs.alphabetQuizOptionCount
            }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesGateway.setThemeMode(themeMode)
        }
    }
}
