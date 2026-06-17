package com.maralyrics.domain.repository

import com.maralyrics.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongsByCategory(category: SongCategory): Flow<List<Song>>
    fun getRecentlyViewed(limit: Int = 10): Flow<List<Song>>
    fun getFavoriteSongs(): Flow<List<Song>>
    fun getPopularSongs(category: SongCategory, limit: Int = 10): Flow<List<Song>>
    fun getRandomSongs(category: SongCategory, limit: Int = 5): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    fun searchSongs(query: String, category: SongCategory): Flow<List<Song>>
    suspend fun toggleFavorite(songId: Long)
    suspend fun markAsViewed(songId: Long)
    suspend fun saveSongs(songs: List<Song>)
    suspend fun getLocalSongCount(): Int
    suspend fun getDatabaseSizeBytes(): Long
    suspend fun clearAllSongs()
}

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateLanguage(language: AppLanguage)
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateDefaultCategory(category: SongCategory)
    suspend fun updateLastSync(timestamp: Long)
    suspend fun markSetupComplete()
    suspend fun updateDatabaseVersion(version: Int)
    suspend fun getLocalDatabaseVersion(): Int
}

interface SyncRepository {
    suspend fun checkForUpdates(): SyncStatus
    suspend fun downloadAllSongs(onProgress: (DownloadProgress) -> Unit): Result<Int>
    suspend fun downloadIncrementalUpdate(sinceVersion: Int): Result<Int>
    suspend fun getServerVersion(): Result<Int>
}