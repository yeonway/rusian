package com.rusian.app.domain.model

data class OnboardingConfig(
    val dailyGoal: Int,
    val selectedCategoryIds: Set<String>,
)
