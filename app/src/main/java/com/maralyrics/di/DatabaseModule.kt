package com.maralyrics.di

import android.content.Context
import androidx.room.Room
import com.maralyrics.data.local.MaraLyricsDatabase
import com.maralyrics.data.local.dao.FavoriteDao
import com.maralyrics.data.local.dao.RecentViewDao
import com.maralyrics.data.local.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MaraLyricsDatabase {
        return Room.databaseBuilder(
            context,
            MaraLyricsDatabase::class.java,
            MaraLyricsDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideSongDao(db: MaraLyricsDatabase): SongDao = db.songDao()

    @Provides
    fun provideFavoriteDao(db: MaraLyricsDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideRecentViewDao(db: MaraLyricsDatabase): RecentViewDao = db.recentViewDao()
}
