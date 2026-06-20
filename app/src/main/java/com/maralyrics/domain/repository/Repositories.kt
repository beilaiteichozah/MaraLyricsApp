package com.maralyrics.domain.repository

import com.maralyrics.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongsByCategory(category: String?): Flow<List<Song>>
    fun getRecentlyViewed(limit: Int = 10): Flow<List<Song>>
    fun getFavoriteSongs(): Flow<List<Song>>
    fun getPopularSongs(category: String?, limit: Int = 10): Flow<List<Song>>
    fun getRandomSongs(category: String?, limit: Int = 5): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    suspend fun getSongBySlug(slug: String): Song?
    fun searchSongs(query: String, category: String?): Flow<List<Song>>
    fun searchSongsWithFuzzy(query: String, category: String?): Flow<SearchResponse>
    suspend fun toggleFavorite(songId: Long)
    suspend fun markAsViewed(songId: Long)
    suspend fun saveSongs(songs: List<Song>)
    suspend fun getLocalSongCount(): Int
    suspend fun getDatabaseSizeBytes(): Long
    suspend fun clearAllSongs()
    fun getSongsByArtist(artistId: Long): Flow<List<Song>>
    fun getSongsByComposer(composerId: Long): Flow<List<Song>>
    fun getAvailableCategories(): Flow<List<SongCategory>>
    suspend fun getFavoriteSongIds(): List<Long>
    suspend fun restoreFavorites(songIds: List<Long>)
    fun getAllSongs(category: String?, sortOrder: com.maralyrics.domain.model.SongSortOrder): Flow<List<Song>>
    fun getSearchInitializationState(): Flow<com.maralyrics.domain.model.SearchInitializationState>
    suspend fun rebuildSearchIndex()
    suspend fun refreshSearchCandidates()
}

interface ArtistRepository {
    fun getArtists(): Flow<List<Artist>>
    suspend fun getArtistBySlug(slug: String): Artist?
    suspend fun getArtistById(id: Long): Artist?
    suspend fun saveArtists(artists: List<Artist>)
    suspend fun clearAllArtists()
}

interface ComposerRepository {
    fun getComposers(): Flow<List<Composer>>
    suspend fun getComposerBySlug(slug: String): Composer?
    suspend fun getComposerById(id: Long): Composer?
    suspend fun saveComposers(composers: List<Composer>)
    suspend fun clearAllComposers()
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
    suspend fun updateDefaultCategory(category: String)
    suspend fun updateLastSync(timestamp: Long)
    suspend fun markSetupComplete()
    suspend fun updateDatabaseVersion(version: Int)
    suspend fun getLocalDatabaseVersion(): Int
    suspend fun updateAutoSync(enabled: Boolean)
    suspend fun updateWifiOnly(enabled: Boolean)
    suspend fun updateDefaultFontSize(size: Int)
    suspend fun updateLineSpacing(spacing: Float)
    suspend fun updateColorTheme(theme: com.maralyrics.domain.model.AppColorTheme)
}

interface SyncRepository {
    suspend fun checkForUpdates(): SyncStatus
    suspend fun downloadBootstrap(onProgress: (DownloadProgress) -> Unit): Result<Int>
    suspend fun downloadAllSongs(onProgress: (DownloadProgress) -> Unit): Result<Int>
    suspend fun downloadAllArtists(onProgress: (DownloadProgress) -> Unit): Result<Int>
    suspend fun downloadAllComposers(onProgress: (DownloadProgress) -> Unit): Result<Int>
    suspend fun downloadIncrementalUpdate(sinceVersion: Int): Result<Int>
    suspend fun getServerVersion(): Result<Int>
}
