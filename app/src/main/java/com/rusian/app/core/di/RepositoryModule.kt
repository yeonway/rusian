package com.rusian.app.core.di

import com.rusian.app.data.chat.LocalModelRunner
import com.rusian.app.data.chat.LlamaCppLocalModelRunner
import com.rusian.app.data.preferences.UserPreferencesDataSource
import com.rusian.app.data.preferences.UserPreferencesGateway
import com.rusian.app.data.remote.AssetContentDataSource
import com.rusian.app.data.remote.RemoteContentDataSource
import com.rusian.app.data.remote.RemoteSyncDataSource
import com.rusian.app.data.remote.SeedContentDataSource
import com.rusian.app.data.repository.BackupRepositoryImpl
import com.rusian.app.data.repository.ChatRepositoryImpl
import com.rusian.app.data.repository.ContentInstallService
import com.rusian.app.data.repository.ContentInstaller
import com.rusian.app.data.repository.AppUpdateRepositoryImpl
import com.rusian.app.data.repository.StudyRepositoryImpl
import com.rusian.app.data.repository.SyncRepositoryImpl
import com.rusian.app.domain.repository.AppUpdateRepository
import com.rusian.app.domain.repository.BackupRepository
import com.rusian.app.domain.repository.ChatRepository
import com.rusian.app.domain.repository.StudyRepository
import com.rusian.app.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStudyRepository(impl: StudyRepositoryImpl): StudyRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(impl: AppUpdateRepositoryImpl): AppUpdateRepository

    @Binds
    @Singleton
    abstract fun bindLocalModelRunner(impl: LlamaCppLocalModelRunner): LocalModelRunner

    @Binds
    @Singleton
    abstract fun bindRemoteSyncDataSource(impl: RemoteContentDataSource): RemoteSyncDataSource

    @Binds
    @Singleton
    abstract fun bindSeedContentDataSource(impl: AssetContentDataSource): SeedContentDataSource

    @Binds
    @Singleton
    abstract fun bindContentInstallService(impl: ContentInstaller): ContentInstallService

    @Binds
    @Singleton
    abstract fun bindUserPreferencesGateway(impl: UserPreferencesDataSource): UserPreferencesGateway
}
