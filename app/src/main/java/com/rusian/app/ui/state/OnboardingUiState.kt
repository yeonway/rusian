package com.rusian.app.ui.state

import com.rusian.app.domain.model.CategoryOption

data class OnboardingUiState(
    val loading: Boolean = true,
    val categories: List<CategoryOption> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val dailyGoal: Int = 20,
    val completed: Boolean = false,
)
