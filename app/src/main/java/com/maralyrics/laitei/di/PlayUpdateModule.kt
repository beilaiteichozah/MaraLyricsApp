package com.maralyrics.laitei.di

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Google Play In-App Updates (app version updates) — kept fully separate from
// SyncDatabaseUseCase, which checks for new song data, not new app releases.
@Module
@InstallIn(SingletonComponent::class)
object PlayUpdateModule {

    @Provides
    @Singleton
    fun providePlayAppUpdateManager(@ApplicationContext context: Context): AppUpdateManager {
        return AppUpdateManagerFactory.create(context)
    }
}
