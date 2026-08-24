package com.maralyrics.laitei.domain.repository

import com.maralyrics.laitei.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongsByCategory(categories: List<String>?): Flow<List<Song>>
    fun getRecentlyViewed(limit: Int = 10): Flow<List<Song>>
    fun getFavoriteSongs(): Flow<List<Song>>
    fun getPopularSongs(categories: List<String>?, limit: Int = 10): Flow<List<Song>>
    fun getRandomSongs(categories: List<String>?, limit: Int = 5): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    suspend fun getSongBySlug(slug: String): Song?
    fun searchSongs(query: String, categories: List<String>?): Flow<List<Song>>
    fun searchSongsWithFuzzy(query: String, categories: List<String>?): Flow<SearchResponse>
    suspend fun toggleFavorite(songId: Long): Boolean
    suspend fun markAsViewed(songId: Long)
    suspend fun saveSongs(songs: List<Song>)
    suspend fun getLocalSongCount(): Int
    suspend fun getDatabaseSizeBytes(): Long
    suspend fun clearAllSongs()
    fun getSongsByArtist(artistId: Long): Flow<List<Song>>
    fun getSongsByComposer(composerId: Long): Flow<List<Song>>
    fun getAvailableCategories(): Flow<List<SongCategory>>
    fun getFavoriteStats(): Flow<FavoriteStats>
    fun getFavoriteCategories(): Flow<List<String>>
    suspend fun getFavoriteSongIds(): List<Long>
    suspend fun restoreFavorites(songIds: List<Long>)
    fun getAllSongs(categories: List<String>?, sortOrder: com.maralyrics.laitei.domain.model.SongSortOrder): Flow<List<Song>>
    fun getSearchInitializationState(): Flow<com.maralyrics.laitei.domain.model.SearchInitializationState>
    suspend fun rebuildSearchIndex()
    suspend fun refreshSearchCandidates()
}

interface ArtistRepository {
    fun getArtists(): Flow<List<Artist>>
    suspend fun getArtistBySlug(slug: String): Artist?
    suspend fun getArtistById(id: Long): Artist?
    suspend fun saveArtists(artists: List<Artist>)
    suspend fun clearAllArtists()
    suspend fun getLocalArtistCount(): Int
}

interface ComposerRepository {
    fun getComposers(): Flow<List<Composer>>
    suspend fun getComposerBySlug(slug: String): Composer?
    suspend fun getComposerById(id: Long): Composer?
    suspend fun saveComposers(composers: List<Composer>)
    suspend fun clearAllComposers()
    suspend fun getLocalComposerCount(): Int
}

interface FeedbackRepository {
    suspend fun submitFeedback(feedback: Feedback): Result<Unit>
    suspend fun saveFeedbackLocally(feedback: Feedback)
    suspend fun getUnsyncedFeedback(): List<Feedback>
    suspend fun markFeedbackAsSynced(id: Long)
    suspend fun deleteSyncedFeedback()
}

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateLanguage(language: AppLanguage)
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateDefaultCategories(categories: List<String>)
    suspend fun updateLastSync(timestamp: Long)
    suspend fun updateLastSyncCursor(cursor: String)
    suspend fun getLastSyncCursor(): String
    suspend fun updateLastKnownCounts(counts: Map<String, Int>)
    suspend fun getLastKnownCounts(): Map<String, Int>
    suspend fun markSetupComplete()
    suspend fun markOnboardingComplete()
    suspend fun resetOnboarding()
    suspend fun updateDatabaseVersion(version: Int)
    suspend fun getLocalDatabaseVersion(): Int
    suspend fun updateAutoSync(enabled: Boolean)
    suspend fun updateWifiOnly(enabled: Boolean)
    suspend fun updateResumeSession(enabled: Boolean)
    suspend fun updateDefaultFontSize(size: Int)
    suspend fun updateLineSpacing(spacing: Float)
    suspend fun updateColorTheme(theme: com.maralyrics.laitei.domain.model.AppColorTheme)
    fun getLastSurpriseSongId(): Flow<Long?>
    suspend fun setLastSurpriseSongId(id: Long)
    fun getSurpriseMeUses(): Flow<Int>
    suspend fun incrementSurpriseMeUses()
    
    // Session State
    suspend fun saveLastSongId(songId: Long?)
    fun getLastSongId(): Flow<Long?>
    suspend fun saveSongScrollPosition(position: Int)
    fun getSongScrollPosition(): Flow<Int>
    
    // Home State
    suspend fun saveHomeSearchQuery(query: String)
    fun getHomeSearchQuery(): Flow<String>
    suspend fun saveHomeSortOrder(order: com.maralyrics.laitei.domain.model.SongSortOrder)
    fun getHomeSortOrder(): Flow<com.maralyrics.laitei.domain.model.SongSortOrder>
    suspend fun saveHomeLayoutType(type: com.maralyrics.laitei.domain.model.SongLayoutType)
    fun getHomeLayoutType(): Flow<com.maralyrics.laitei.domain.model.SongLayoutType>
    suspend fun saveHomeScrollState(index: Int, offset: Int)
    fun getHomeScrollState(): Flow<Pair<Int, Int>>
    
    // Favorite State
    suspend fun saveFavoriteSearchQuery(query: String)
    fun getFavoriteSearchQuery(): Flow<String>
    suspend fun saveFavoriteSortOrder(order: com.maralyrics.laitei.domain.model.SongSortOrder)
    fun getFavoriteSortOrder(): Flow<com.maralyrics.laitei.domain.model.SongSortOrder>
    suspend fun saveFavoriteCategoryFilter(category: String?)
    fun getFavoriteCategoryFilter(): Flow<String?>
    suspend fun saveFavoriteLayoutType(type: com.maralyrics.laitei.domain.model.SongLayoutType)
    fun getFavoriteLayoutType(): Flow<com.maralyrics.laitei.domain.model.SongLayoutType>
    suspend fun saveFavoriteScrollState(index: Int, offset: Int)
    fun getFavoriteScrollState(): Flow<Pair<Int, Int>>
    
    // Navigation State
    suspend fun saveLastRoute(route: String?)
    fun getLastRoute(): Flow<String?>

    // Privacy Policy
    suspend fun updatePrivacyPolicyAcceptance(accepted: Boolean, version: String, timestamp: Long)
}

interface CreditsRepository {
    fun getCredits(): Flow<CreditsData>
}

interface SyncRepository {
    val updateStatus: kotlinx.coroutines.flow.StateFlow<com.maralyrics.laitei.domain.model.SyncStatus?>
    suspend fun checkForUpdates(): SyncStatus
    suspend fun downloadBootstrap(onProgress: (DownloadProgress) -> Unit): Result<Int>
    suspend fun downloadAllSongs(onProgress: (DownloadProgress) -> Unit): Result<Int>
    suspend fun downloadAllArtists(onProgress: (DownloadProgress) -> Unit): Result<Int>
    suspend fun downloadAllComposers(onProgress: (DownloadProgress) -> Unit): Result<Int>
    suspend fun downloadIncrementalUpdate(since: String, onProgress: ((DownloadProgress) -> Unit)? = null): Result<Int>
    suspend fun getServerVersion(): Result<Int>
    suspend fun getLocalCopyrightOwnerCount(): Int
}
