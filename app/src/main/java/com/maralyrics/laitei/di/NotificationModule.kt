package com.maralyrics.laitei.di

import com.maralyrics.laitei.presentation.common.notification.NotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationManager(): NotificationManager {
        return NotificationManager()
    }
}
