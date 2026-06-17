package com.maralyrics.di

import com.maralyrics.data.repository.SettingsRepositoryImpl
import com.maralyrics.data.repository.SongRepositoryImpl
import com.maralyrics.data.repository.SyncRepositoryImpl
import com.maralyrics.domain.repository.SettingsRepository
import com.maralyrics.domain.repository.SongRepository
import com.maralyrics.domain.repository.SyncRepository
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
    abstract fun bindSongRepository(
        songRepositoryImpl: SongRepositoryImpl
    ): SongRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        syncRepositoryImpl: SyncRepositoryImpl
    ): SyncRepository
}
