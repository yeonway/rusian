package com.rusian.app.data.preferences

import com.rusian.app.domain.model.ThemeMode

data class UserPreferences(
    val onboardingCompleted: Boolean,
    val dailyGoal: Int,
    val selectedCategoryIds: Set<String>,
    val lastSyncAt: Long?,
    val syncPolicy: SyncPolicy,
    val manifestUrl: String,
    val factoryResetArmed: Boolean,
    val themeMode: ThemeMode,
    val lastChatConversationId: Long?,
    val showExpressionCards: Boolean,
    val alphabetQuizOptionCount: Int,
)
