package com.maralyrics.laitei.di

import com.maralyrics.laitei.data.sync.AccountSyncManager
import com.maralyrics.laitei.data.sync.FavoriteStore
import com.maralyrics.laitei.data.sync.FileSyncStateStore
import com.maralyrics.laitei.data.sync.PreferenceStore
import com.maralyrics.laitei.data.sync.RoomFavoriteStore
import com.maralyrics.laitei.data.sync.SettingsPreferenceStore
import com.maralyrics.laitei.data.sync.SyncStateStore
import com.maralyrics.laitei.domain.model.AccountSync
import com.maralyrics.laitei.domain.model.SyncRecorder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncStateStore(store: FileSyncStateStore): SyncStateStore

    @Binds
    @Singleton
    abstract fun bindFavoriteStore(store: RoomFavoriteStore): FavoriteStore

    @Binds
    @Singleton
    abstract fun bindPreferenceStore(store: SettingsPreferenceStore): PreferenceStore

    @Binds
    @Singleton
    abstract fun bindSyncRecorder(manager: AccountSyncManager): SyncRecorder

    @Binds
    @Singleton
    abstract fun bindAccountSync(manager: AccountSyncManager): AccountSync
}
