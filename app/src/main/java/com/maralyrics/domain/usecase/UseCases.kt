package com.maralyrics.domain.usecase

import com.maralyrics.domain.model.*
import com.maralyrics.domain.repository.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class SearchSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(query: String, categories: List<String>?): Flow<List<Song>> {
        return songRepository.searchSongs(query, categories)
    }
}

class SearchSongsWithFuzzyUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(query: String, categories: List<String>?): Flow<SearchResponse> {
        return songRepository.searchSongsWithFuzzy(query, categories)
    }
}

class GetAvailableCategoriesUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<SongCategory>> = songRepository.getAvailableCategories()
}

class GetFavoriteSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<Song>> = songRepository.getFavoriteSongs()
}

class GetFavoriteStatsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<FavoriteStats> = songRepository.getFavoriteStats()
}

class GetFavoriteCategoriesUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<String>> = songRepository.getFavoriteCategories()
}

class GetSurpriseSongUseCase @Inject constructor(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Song? {
        val favorites = songRepository.getFavoriteSongs().first()
        if (favorites.isEmpty()) return null
        
        val lastId = settingsRepository.getLastSurpriseSongId().first()
        val candidates = if (favorites.size > 1) {
            favorites.filter { it.id != lastId }
        } else {
            favorites
        }
        
        val selected = candidates.random()
        settingsRepository.setLastSurpriseSongId(selected.id)
        settingsRepository.incrementSurpriseMeUses()
        return selected
    }
}

class ToggleFavoriteUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Boolean = songRepository.toggleFavorite(songId)
}

class GetSongDetailUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Song? {
        songRepository.markAsViewed(songId)
        return songRepository.getSongById(songId)
    }

    suspend fun getBySlug(slug: String): Song? {
        val song = songRepository.getSongBySlug(slug)
        song?.let { songRepository.markAsViewed(it.id) }
        return song
    }
}

class SubmitFeedbackUseCase @Inject constructor(
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke(feedback: Feedback): Result<Unit> {
        return try {
            val result = feedbackRepository.submitFeedback(feedback)
            if (result.isFailure) {
                feedbackRepository.saveFeedbackLocally(feedback)
            }
            result
        } catch (e: Exception) {
            feedbackRepository.saveFeedbackLocally(feedback)
            Result.failure(e)
        }
    }
}

class SyncDatabaseUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    private val feedbackRepository: FeedbackRepository,
    private val connectivityObserver: com.maralyrics.utils.ConnectivityObserver
) {
    suspend fun checkAndSync(isAutomatic: Boolean = false): Result<SyncResult> {
        return try {
            val settings = settingsRepository.getSettings().first()
            
            if (isAutomatic && !settings.autoSyncEnabled) {
                return Result.success(SyncResult(0, settings.databaseVersion))
            }

            if (settings.wifiOnlySync && !connectivityObserver.isWifiConnected()) {
                // If it's manual, we might still want to sync if user clicked "Sync Now", 
                // but usually wifi-only applies to automatic syncs.
                // Request said "Auto-Sync toggle — Wi-Fi only vs always".
                // I'll assume if wifiOnlySync is true, we ONLY sync on wifi, even if manual?
                // Most apps allow manual sync even on mobile data.
                if (isAutomatic) {
                    return Result.success(SyncResult(0, settings.databaseVersion))
                }
            }

            // Sync feedback first
            syncPendingFeedback()

            val status = syncRepository.checkForUpdates()
            if (status.isAvailable) {
                val result = syncRepository.downloadIncrementalUpdate(settings.databaseVersion)
                result.map { count ->
                    settingsRepository.updateLastSync(System.currentTimeMillis())
                    settingsRepository.updateDatabaseVersion(status.serverVersion)
                    SyncResult(updatedSongs = count, newVersion = status.serverVersion)
                }
            } else {
                Result.success(SyncResult(updatedSongs = 0, newVersion = status.localVersion))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncPendingFeedback() {
        val unsynced = feedbackRepository.getUnsyncedFeedback()
        unsynced.forEach { feedback ->
            val result = feedbackRepository.submitFeedback(feedback)
            if (result.isSuccess) {
                feedbackRepository.markFeedbackAsSynced(feedback.id)
            }
        }
        feedbackRepository.deleteSyncedFeedback()
    }

    suspend fun fullDownload(onProgress: (DownloadProgress) -> Unit): Result<SyncResult> {
        return try {
            val result = syncRepository.downloadBootstrap { progress ->
                onProgress(progress)
            }

            result.map { count ->
                val serverVersion = syncRepository.getServerVersion().getOrDefault(1)
                settingsRepository.updateLastSync(System.currentTimeMillis())
                settingsRepository.updateDatabaseVersion(serverVersion)
                SyncResult(updatedSongs = count, newVersion = serverVersion)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SyncResult(
    val updatedSongs: Int,
    val newVersion: Int
)

class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.getSettings()
}

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend fun updateLanguage(language: AppLanguage) = settingsRepository.updateLanguage(language)
    suspend fun updateTheme(theme: AppTheme) = settingsRepository.updateTheme(theme)
    suspend fun updateCategories(categories: List<String>) = settingsRepository.updateDefaultCategories(categories)
    suspend fun markSetupComplete() = settingsRepository.markSetupComplete()
    suspend fun markOnboardingComplete() = settingsRepository.markOnboardingComplete()
    suspend fun resetOnboarding() = settingsRepository.resetOnboarding()
    suspend fun updateAutoSync(enabled: Boolean) = settingsRepository.updateAutoSync(enabled)
    suspend fun updateWifiOnly(enabled: Boolean) = settingsRepository.updateWifiOnly(enabled)
    suspend fun updateResumeSession(enabled: Boolean) = settingsRepository.updateResumeSession(enabled)
    suspend fun updateDefaultFontSize(size: Int) = settingsRepository.updateDefaultFontSize(size)
    suspend fun updateLineSpacing(spacing: Float) = settingsRepository.updateLineSpacing(spacing)
    suspend fun updateColorTheme(theme: AppColorTheme) = settingsRepository.updateColorTheme(theme)
}
