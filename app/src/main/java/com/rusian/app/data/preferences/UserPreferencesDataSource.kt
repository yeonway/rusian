package com.rusian.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.rusian.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesGateway {
    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            dailyGoal = prefs[KEY_DAILY_GOAL] ?: DEFAULT_DAILY_GOAL,
            selectedCategoryIds = prefs[KEY_SELECTED_CATEGORY_IDS] ?: emptySet(),
            lastSyncAt = prefs[KEY_LAST_SYNC_AT],
            syncPolicy = SyncPolicy.valueOf(prefs[KEY_SYNC_POLICY] ?: SyncPolicy.WIFI_OR_CHARGING.name),
            manifestUrl = prefs[KEY_MANIFEST_URL] ?: DEFAULT_MANIFEST_URL,
            factoryResetArmed = prefs[KEY_FACTORY_RESET_ARMED] ?: false,
            themeMode = ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name),
            lastChatConversationId = prefs[KEY_LAST_CHAT_CONVERSATION_ID],
            showExpressionCards = prefs[KEY_SHOW_EXPRESSION_CARDS] ?: false,
            alphabetQuizOptionCount = prefs[KEY_ALPHABET_QUIZ_OPTION_COUNT] ?: DEFAULT_ALPHABET_QUIZ_OPTION_COUNT,
        )
    }

    override suspend fun completeOnboarding(dailyGoal: Int, selectedCategoryIds: Set<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = true
            prefs[KEY_DAILY_GOAL] = dailyGoal
            prefs[KEY_SELECTED_CATEGORY_IDS] = selectedCategoryIds
        }
    }

    override suspend fun setDailyGoal(dailyGoal: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_DAILY_GOAL] = dailyGoal.coerceIn(5, 100)
        }
    }

    override suspend fun setSelectedCategoryIds(selectedCategoryIds: Set<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_CATEGORY_IDS] = selectedCategoryIds
        }
    }

    override suspend fun setLastSyncAt(timestamp: Long) {
        dataStore.edit { it[KEY_LAST_SYNC_AT] = timestamp }
    }

    override suspend fun setSyncPolicy(syncPolicy: SyncPolicy) {
        dataStore.edit { it[KEY_SYNC_POLICY] = syncPolicy.name }
    }

    override suspend fun setManifestUrl(url: String) {
        dataStore.edit { it[KEY_MANIFEST_URL] = url.trim() }
    }

    override suspend fun setFactoryResetArmed(armed: Boolean) {
        dataStore.edit { it[KEY_FACTORY_RESET_ARMED] = armed }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = themeMode.name }
    }

    override suspend fun setLastChatConversationId(conversationId: Long?) {
        dataStore.edit { prefs ->
            if (conversationId == null) {
                prefs.remove(KEY_LAST_CHAT_CONVERSATION_ID)
            } else {
                prefs[KEY_LAST_CHAT_CONVERSATION_ID] = conversationId
            }
        }
    }

    override suspend fun setShowExpressionCards(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_EXPRESSION_CARDS] = show }
    }

    override suspend fun setAlphabetQuizOptionCount(count: Int) {
        dataStore.edit { it[KEY_ALPHABET_QUIZ_OPTION_COUNT] = count.coerceIn(4, 12) }
    }

    companion object {
        private const val DEFAULT_DAILY_GOAL = 20
        private const val DEFAULT_ALPHABET_QUIZ_OPTION_COUNT = 4
        private const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/yeonway/rusian/refs/heads/main/content-pack.json"

        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_DAILY_GOAL = intPreferencesKey("daily_goal")
        private val KEY_SELECTED_CATEGORY_IDS = stringSetPreferencesKey("selected_categories")
        private val KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        private val KEY_SYNC_POLICY = stringPreferencesKey("sync_policy")
        private val KEY_MANIFEST_URL = stringPreferencesKey("manifest_url")
        private val KEY_FACTORY_RESET_ARMED = booleanPreferencesKey("factory_reset_armed")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_LAST_CHAT_CONVERSATION_ID = longPreferencesKey("last_chat_conversation_id")
        private val KEY_SHOW_EXPRESSION_CARDS = booleanPreferencesKey("show_expression_cards")
        private val KEY_ALPHABET_QUIZ_OPTION_COUNT = intPreferencesKey("alphabet_quiz_option_count")
    }
}
