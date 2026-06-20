package com.maralyrics.di

import android.content.Context
import androidx.room.Room
import com.maralyrics.data.local.MaraLyricsDatabase
import com.maralyrics.data.local.dao.*
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
        )
        .addMigrations(
            MaraLyricsDatabase.MIGRATION_1_2, 
            MaraLyricsDatabase.MIGRATION_2_3,
            MaraLyricsDatabase.MIGRATION_3_4,
            MaraLyricsDatabase.MIGRATION_4_5,
            MaraLyricsDatabase.MIGRATION_5_6
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideSongDao(db: MaraLyricsDatabase): SongDao = db.songDao()

    @Provides
    fun provideArtistDao(db: MaraLyricsDatabase): ArtistDao = db.artistDao()

    @Provides
    fun provideComposerDao(db: MaraLyricsDatabase): ComposerDao = db.composerDao()

    @Provides
    fun provideCopyrightOwnerDao(db: MaraLyricsDatabase): CopyrightOwnerDao = db.copyrightOwnerDao()

    @Provides
    fun provideFavoriteDao(db: MaraLyricsDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideRecentViewDao(db: MaraLyricsDatabase): RecentViewDao = db.recentViewDao()

    @Provides
    fun provideFeedbackDao(db: MaraLyricsDatabase): FeedbackDao = db.feedbackDao()

    @Provides
    fun provideSyncMetadataDao(db: MaraLyricsDatabase): SyncMetadataDao = db.syncMetadataDao()
}
