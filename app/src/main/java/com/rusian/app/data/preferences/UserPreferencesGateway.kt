package com.rusian.app.data.preferences

import com.rusian.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesGateway {
    val preferences: Flow<UserPreferences>
    suspend fun completeOnboarding(dailyGoal: Int, selectedCategoryIds: Set<String>)
    suspend fun setDailyGoal(dailyGoal: Int)
    suspend fun setSelectedCategoryIds(selectedCategoryIds: Set<String>)
    suspend fun setLastSyncAt(timestamp: Long)
    suspend fun setSyncPolicy(syncPolicy: SyncPolicy)
    suspend fun setManifestUrl(url: String)
    suspend fun setFactoryResetArmed(armed: Boolean)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setLastChatConversationId(conversationId: Long?)
    suspend fun setShowExpressionCards(show: Boolean)
    suspend fun setAlphabetQuizOptionCount(count: Int)
}
