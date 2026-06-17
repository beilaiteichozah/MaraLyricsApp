package com.maralyrics.domain.usecase

import com.maralyrics.domain.model.*
import com.maralyrics.domain.repository.SettingsRepository
import com.maralyrics.domain.repository.SongRepository
import com.maralyrics.domain.repository.SyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class GetHomeScreenDataUseCase @Inject constructor(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<HomeScreenData> {
        return settingsRepository.getSettings().flatMapLatest { settings ->
            val category = settings.defaultCategory
            combine(
                songRepository.getRecentlyViewed(10),
                songRepository.getFavoriteSongs(),
                songRepository.getPopularSongs(category, 10),
                songRepository.getRandomSongs(category, 5)
            ) { recent, favorites, popular, random ->
                HomeScreenData(
                    recentlyViewed = recent.filter { it.category == category },
                    favorites = favorites.filter { it.category == category },
                    popular = popular,
                    random = random,
                    category = category
                )
            }
        }
    }
}

data class HomeScreenData(
    val recentlyViewed: List<Song>,
    val favorites: List<Song>,
    val popular: List<Song>,
    val random: List<Song>,
    val category: SongCategory
)

class SearchSongsUseCase @Inject constructor(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(query: String): Flow<List<Song>> {
        return settingsRepository.getSettings().flatMapLatest { settings ->
            songRepository.searchSongs(query, settings.defaultCategory)
        }
    }

    fun invoke(query: String, category: SongCategory): Flow<List<Song>> =
        songRepository.searchSongs(query, category)
}

class ToggleFavoriteUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(songId: Long) = songRepository.toggleFavorite(songId)
}

class GetSongDetailUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Song? {
        songRepository.markAsViewed(songId)
        return songRepository.getSongById(songId)
    }
}

class SyncDatabaseUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun checkAndSync(): Result<SyncResult> {
        return try {
            val status = syncRepository.checkForUpdates()
            if (status.isAvailable) {
                val settings = settingsRepository.getSettings().first()
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

    suspend fun fullDownload(onProgress: (DownloadProgress) -> Unit): Result<SyncResult> {
        return try {
            val result = syncRepository.downloadAllSongs(onProgress)
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
    suspend fun updateCategory(category: SongCategory) = settingsRepository.updateDefaultCategory(category)
    suspend fun markSetupComplete() = settingsRepository.markSetupComplete()
}
