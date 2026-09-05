package com.maralyrics.laitei.domain.usecase

import com.maralyrics.laitei.domain.model.*
import com.maralyrics.laitei.domain.repository.*
import com.maralyrics.laitei.utils.StorageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class SyncResult(
    val updatedSongs: Int,
    val newVersion: Int,
    // Songs specifically (as opposed to artist/composer/copyright-owner-only changes) —
    // used to decide whether the "new songs available" notification should fire.
    val newSongs: Int = 0
)

class InsufficientStorageException(val info: StorageUtils.SpaceInfo) : Exception("Insufficient storage: ${info.additionalNeededBytes} bytes more required")

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
    private val connectivityObserver: com.maralyrics.laitei.utils.ConnectivityObserver
) {
    val updateStatus = syncRepository.updateStatus

    suspend fun checkForUpdates(): Result<SyncStatus> = try {
        Result.success(syncRepository.checkForUpdates())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun checkAndSync(
        isAutomatic: Boolean = false,
        onProgress: ((DownloadProgress) -> Unit)? = null
    ): Result<SyncResult> {
        return try {
            val settings = settingsRepository.getSettings().first()
            
            if (isAutomatic && !settings.autoSyncEnabled) {
                return Result.success(SyncResult(0, settings.databaseVersion))
            }

            if (settings.wifiOnlySync && !connectivityObserver.isWifiConnected()) {
                if (isAutomatic) {
                    return Result.success(SyncResult(0, settings.databaseVersion))
                }
            }

            // Sync pending feedback first
            try {
                val unsynced = feedbackRepository.getUnsyncedFeedback()
                unsynced.forEach { feedback ->
                    val res = feedbackRepository.submitFeedback(feedback)
                    if (res.isSuccess) {
                        feedbackRepository.markFeedbackAsSynced(feedback.id)
                    }
                }
                feedbackRepository.deleteSyncedFeedback()
            } catch (e: Exception) {
                // Ignore feedback sync errors for main data sync
            }

            val status = syncRepository.checkForUpdates()
            if (status.isAvailable) {
                val localCursor = settingsRepository.getLastSyncCursor()
                val result = if (status.requiresFullRefresh) {
                    syncRepository.downloadBootstrap { progress -> onProgress?.invoke(progress) }
                } else {
                    syncRepository.downloadIncrementalUpdate(localCursor, onProgress)
                }

                result.map { count ->
                    settingsRepository.updateLastSync(System.currentTimeMillis())
                    settingsRepository.updateDatabaseVersion(status.serverVersion)
                    SyncResult(
                        updatedSongs = count,
                        newVersion = status.serverVersion,
                        newSongs = if (status.newSongs > 0) status.newSongs else 0
                    )
                }
            } else {
                Result.success(SyncResult(updatedSongs = 0, newVersion = status.localVersion))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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

class GetAvailableCategoriesUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<SongCategory>> = songRepository.getAvailableCategories()
}

class ToggleFavoriteUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Boolean = songRepository.toggleFavorite(songId)
}

class SearchSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(query: String, categories: List<String>?): Flow<List<Song>> =
        songRepository.searchSongs(query, categories)
}

class SearchSongsWithFuzzyUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(query: String, categories: List<String>?): Flow<SearchResponse> =
        songRepository.searchSongsWithFuzzy(query, categories)
}

class GetSongDetailUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Song? = songRepository.getSongById(songId)
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
        val favoriteSongs = songRepository.getFavoriteSongs().first()
        if (favoriteSongs.isEmpty()) return null

        val lastId = settingsRepository.getLastSurpriseSongId().first()
        
        val candidates = if (favoriteSongs.size > 1) {
            favoriteSongs.filter { it.id != lastId }
        } else {
            favoriteSongs
        }

        val selected = candidates.random()
        settingsRepository.setLastSurpriseSongId(selected.id)
        settingsRepository.incrementSurpriseMeUses()
        
        return selected
    }
}
