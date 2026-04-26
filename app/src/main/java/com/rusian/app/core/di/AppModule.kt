package com.rusian.app.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.rusian.app.data.local.RusianDatabase
import com.rusian.app.data.local.dao.AlphabetLetterDao
import com.rusian.app.data.local.dao.ChatMessageDao
import com.rusian.app.data.local.dao.CategoryDao
import com.rusian.app.data.local.dao.ConversationDao
import com.rusian.app.data.local.dao.DatasetInfoDao
import com.rusian.app.data.local.dao.ModelFileDao
import com.rusian.app.data.local.dao.ReviewEventDao
import com.rusian.app.data.local.dao.ReviewStateDao
import com.rusian.app.data.local.dao.StudyItemDao
import com.rusian.app.data.local.dao.WordDao
import com.rusian.app.data.local.dao.WordGlossDao
import com.rusian.app.domain.srs.ReviewScheduler
import com.rusian.app.domain.srs.Sm2LiteReviewScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("user_preferences.preferences_pb") },
        )
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RusianDatabase {
        return Room.databaseBuilder(
            context,
            RusianDatabase::class.java,
            "rusian.db",
        ).addMigrations(RusianDatabase.MIGRATION_1_2)
            .addMigrations(RusianDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideCategoryDao(db: RusianDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideAlphabetLetterDao(db: RusianDatabase): AlphabetLetterDao = db.alphabetLetterDao()

    @Provides
    fun provideWordDao(db: RusianDatabase): WordDao = db.wordDao()

    @Provides
    fun provideWordGlossDao(db: RusianDatabase): WordGlossDao = db.wordGlossDao()

    @Provides
    fun provideStudyItemDao(db: RusianDatabase): StudyItemDao = db.studyItemDao()

    @Provides
    fun provideReviewStateDao(db: RusianDatabase): ReviewStateDao = db.reviewStateDao()

    @Provides
    fun provideReviewEventDao(db: RusianDatabase): ReviewEventDao = db.reviewEventDao()

    @Provides
    fun provideDatasetInfoDao(db: RusianDatabase): DatasetInfoDao = db.datasetInfoDao()

    @Provides
    fun provideModelFileDao(db: RusianDatabase): ModelFileDao = db.modelFileDao()

    @Provides
    fun provideConversationDao(db: RusianDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideChatMessageDao(db: RusianDatabase): ChatMessageDao = db.chatMessageDao()

    @Provides
    @Singleton
    fun provideReviewScheduler(): ReviewScheduler = Sm2LiteReviewScheduler()

}
