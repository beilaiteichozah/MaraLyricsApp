package com.maralyrics.laitei.di

import com.maralyrics.laitei.data.local.dao.SongDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// GlanceAppWidget/GlanceAppWidgetReceiver instances are created by the OS, not Hilt,
// so widgets reach their dependencies through this entry point instead of constructor injection.
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun songDao(): SongDao
}
