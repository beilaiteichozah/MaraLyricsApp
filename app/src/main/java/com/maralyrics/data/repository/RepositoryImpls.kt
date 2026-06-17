package com.maralyrics.data.repository

import com.maralyrics.data.local.dao.FavoriteDao
import com.maralyrics.data.local.dao.RecentViewDao
import com.maralyrics.data.local.dao.SongDao
import com.maralyrics.data.local.entity.FavoriteEntity
import com.maralyrics.data.local.entity.RecentViewEntity
import com.maralyrics.data.local.entity.SongEntity
import com.maralyrics.data.remote.MaraLyricsApiService
import com.maralyrics.data.remote.dto.SongDto
import com.maralyrics.domain.model.*
import com.maralyrics.domain.repository.SongRepository
import com.maralyrics.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val favoriteDao: FavoriteDao,
    private val recentViewDao: RecentViewDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : SongRepository {

    override fun getSongsByCategory(category: SongCategory): Flow<List<Song>> =
        songDao.getSongsByCategory(category.key)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.id)) }
            }

    override fun getRecentlyViewed(limit: Int): Flow<List<Song>> =
        songDao.getRecentlyViewed(limit)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.id)) }
            }

    override fun getFavoriteSongs(): Flow<List<Song>> =
        songDao.getFavoriteSongs().map { songs ->
            songs.map { it.toDomain(isFavorite = true) }
        }

    override fun getPopularSongs(category: SongCategory, limit: Int): Flow<List<Song>> =
        songDao.getPopularSongs(category.key, limit)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.id)) }
            }

    override fun getRandomSongs(category: SongCategory, limit: Int): Flow<List<Song>> =
        songDao.getRandomSongs(category.key, limit)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.id)) }
            }

    override suspend fun getSongById(id: Long): Song? {
        return songDao.getSongById(id)?.let { entity ->
            val isFav = favoriteDao.isFavorite(id)
            entity.toDomain(isFav)
        }
    }

    override fun searchSongs(query: String, category: SongCategory): Flow<List<Song>> =
        songDao.searchSongs(query, category.key)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.id)) }
            }

    override suspend fun toggleFavorite(songId: Long) {
        if (favoriteDao.isFavorite(songId)) {
            favoriteDao.removeFavorite(songId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(songId = songId))
        }
    }

    override suspend fun markAsViewed(songId: Long) {
        recentViewDao.deleteRecentView(songId) // remove old entry
        recentViewDao.insertRecentView(RecentViewEntity(songId = songId))
        recentViewDao.trimRecentViews()
        songDao.incrementViewCount(songId)
    }

    override suspend fun saveSongs(songs: List<Song>) {
        songDao.insertSongs(songs.map { it.toEntity() })
    }

    override suspend fun getLocalSongCount(): Int = songDao.getSongCount()

    override suspend fun getDatabaseSizeBytes(): Long {
        val dbFile = context.getDatabasePath(com.maralyrics.data.local.MaraLyricsDatabase.DATABASE_NAME)
        return if (dbFile.exists()) dbFile.length() else 0L
    }

    override suspend fun clearAllSongs() = songDao.deleteAllSongs()
}

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val apiService: MaraLyricsApiService,
    private val songDao: SongDao,
    private val settingsDataSource: SettingsDataSource
) : SyncRepository {

    override suspend fun checkForUpdates(): SyncStatus {
        val localVersion = settingsDataSource.getDatabaseVersion()
        val serverResponse = apiService.getDatabaseVersion()
        val serverVersion = serverResponse.body()?.version ?: localVersion
        return SyncStatus(
            isAvailable = serverVersion > localVersion,
            localVersion = localVersion,
            serverVersion = serverVersion
        )
    }

    override suspend fun downloadAllSongs(onProgress: (DownloadProgress) -> Unit): Result<Int> {
        return try {
            var page = 1
            var totalDownloaded = 0
            var totalSongs = 0
            val startTime = System.currentTimeMillis()

            do {
                val response = apiService.getAllSongs(page = page, perPage = 100)
                val body = response.body() ?: break
                if (page == 1) totalSongs = body.total

                val songs = body.songs.map { it.toEntity() }
                songDao.insertSongs(songs)
                totalDownloaded += songs.size

                val elapsed = System.currentTimeMillis() - startTime
                val rate = if (elapsed > 0) totalDownloaded.toFloat() / elapsed * 1000 else 1f
                val remaining = if (rate > 0) ((totalSongs - totalDownloaded) / rate).toLong() else 0L

                onProgress(
                    DownloadProgress(
                        totalSongs = totalSongs,
                        downloadedSongs = totalDownloaded,
                        percentage = if (totalSongs > 0) totalDownloaded.toFloat() / totalSongs * 100 else 0f,
                        estimatedSecondsRemaining = remaining
                    )
                )

                page++
            } while (totalDownloaded < totalSongs)

            Result.success(totalDownloaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadIncrementalUpdate(sinceVersion: Int): Result<Int> {
        return try {
            val response = apiService.getIncrementalUpdates(sinceVersion)
            val body = response.body() ?: return Result.failure(Exception("Empty response"))
            songDao.insertSongs(body.songs.map { it.toEntity() })
            Result.success(body.songs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getServerVersion(): Result<Int> {
        return try {
            val response = apiService.getDatabaseVersion()
            val version = response.body()?.version ?: return Result.failure(Exception("Empty response"))
            Result.success(version)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Mappers
private fun SongEntity.toDomain(isFavorite: Boolean = false) = Song(
    id = id,
    title = title,
    lyrics = lyrics,
    category = SongCategory.fromKey(category),
    songNumber = songNumber,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isFavorite = isFavorite
)

private fun Song.toEntity() = SongEntity(
    id = id,
    title = title,
    lyrics = lyrics,
    category = category.key,
    songNumber = songNumber,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun SongDto.toEntity() = SongEntity(
    id = id,
    title = title,
    lyrics = lyrics,
    category = category,
    songNumber = songNumber,
    createdAt = createdAt,
    updatedAt = updatedAt
)