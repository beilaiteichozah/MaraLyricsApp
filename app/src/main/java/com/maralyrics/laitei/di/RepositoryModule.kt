package com.maralyrics.laitei.di

import com.maralyrics.laitei.data.local.KeystoreSessionStore
import com.maralyrics.laitei.data.local.SessionStore
import com.maralyrics.laitei.data.repository.*
import com.maralyrics.laitei.domain.repository.*
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
    abstract fun bindArtistRepository(
        artistRepositoryImpl: ArtistRepositoryImpl
    ): ArtistRepository

    @Binds
    @Singleton
    abstract fun bindComposerRepository(
        composerRepositoryImpl: ComposerRepositoryImpl
    ): ComposerRepository

    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(
        feedbackRepositoryImpl: FeedbackRepositoryImpl
    ): FeedbackRepository

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

    @Binds
    @Singleton
    abstract fun bindCreditsRepository(
        creditsRepositoryImpl: CreditsRepositoryImpl
    ): CreditsRepository

    @Binds
    @Singleton
    abstract fun bindSongOfTheDayRepository(
        songOfTheDayRepositoryImpl: SongOfTheDayRepositoryImpl
    ): SongOfTheDayRepository

    @Binds
    @Singleton
    abstract fun bindArticleRepository(
        articleRepositoryImpl: ArticleRepositoryImpl
    ): ArticleRepository

    @Binds
    @Singleton
    abstract fun bindMaraIdolRepository(
        maraIdolRepositoryImpl: MaraIdolRepositoryImpl
    ): MaraIdolRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindSessionTokens(
        accountRepositoryImpl: AccountRepositoryImpl
    ): SessionTokens

    @Binds
    @Singleton
    abstract fun bindSessionStore(
        keystoreSessionStore: KeystoreSessionStore
    ): SessionStore

    @Binds
    @Singleton
    abstract fun bindCreditsExtrasRepository(
        creditsExtrasRepositoryImpl: CreditsExtrasRepositoryImpl
    ): CreditsExtrasRepository

    @Binds
    @Singleton
    abstract fun bindProfileExtrasRepository(
        profileExtrasRepositoryImpl: ProfileExtrasRepositoryImpl
    ): ProfileExtrasRepository

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(
        calendarRepositoryImpl: CalendarRepositoryImpl
    ): CalendarRepository
}
