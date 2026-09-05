package com.maralyrics.laitei.data.repository

import android.content.Context
import com.maralyrics.laitei.data.local.MaraLyricsDatabase
import com.maralyrics.laitei.data.local.dao.*
import com.maralyrics.laitei.data.local.entity.*
import com.maralyrics.laitei.data.mapper.*
import com.maralyrics.laitei.data.remote.MaraLyricsApiService
import com.maralyrics.laitei.data.remote.dto.*
import com.maralyrics.laitei.domain.model.*
import com.maralyrics.laitei.domain.repository.*
import com.maralyrics.laitei.domain.usecase.InsufficientStorageException
import com.maralyrics.laitei.utils.SearchCandidateCache
import com.maralyrics.laitei.utils.SearchUtils
import com.maralyrics.laitei.utils.StorageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val favoriteDao: FavoriteDao,
    private val recentViewDao: RecentViewDao,
    private val searchCandidateCache: SearchCandidateCache,
    @ApplicationContext private val context: Context
) : SongRepository {

    override fun getSongsByCategory(categories: List<String>?): Flow<List<Song>> {
        val isFiltered = !categories.isNullOrEmpty() && !categories.contains("All")
        return combine(
            songDao.getSongsByCategories(categories ?: emptyList(), isFiltered),
            favoriteDao.getAllFavoriteSongIds()
        ) { list, favorites ->
            list.map { it.toDomain(favorites.contains(it.song.id)) }
        }
    }

    override fun getRecentlyViewed(limit: Int): Flow<List<Song>> {
        return combine(
            songDao.getRecentlyViewed(limit),
            favoriteDao.getAllFavoriteSongIds()
        ) { list, favorites ->
            list.map { it.toDomain(favorites.contains(it.song.id)) }
        }
    }

    override fun getFavoriteSongs(): Flow<List<Song>> {
        return songDao.getFavoriteSongs().map { list ->
            list.map { it.toDomain(true) }
        }
    }

    override fun getPopularSongs(categories: List<String>?, limit: Int): Flow<List<Song>> {
        val isFiltered = !categories.isNullOrEmpty() && !categories.contains("All")
        return combine(
            songDao.getPopularSongs(categories ?: emptyList(), isFiltered, limit),
            favoriteDao.getAllFavoriteSongIds()
        ) { list, favorites ->
            list.map { it.toDomain(favorites.contains(it.song.id)) }
        }
    }

    override fun getRandomSongs(categories: List<String>?, limit: Int): Flow<List<Song>> {
        val isFiltered = !categories.isNullOrEmpty() && !categories.contains("All")
        return combine(
            songDao.getRandomSongs(categories ?: emptyList(), isFiltered, limit),
            favoriteDao.getAllFavoriteSongIds()
        ) { list, favorites ->
            list.map { it.toDomain(favorites.contains(it.song.id)) }
        }
    }

    override suspend fun getSongById(id: Long): Song? {
        val isFavorite = favoriteDao.isFavorite(id)
        return songDao.getSongById(id)?.toDomain(isFavorite)
    }

    override suspend fun getSongBySlug(slug: String): Song? {
        val songWithDetails = songDao.getSongBySlug(slug) ?: return null
        val isFavorite = favoriteDao.isFavorite(songWithDetails.song.id)
        return songWithDetails.toDomain(isFavorite)
    }

    override fun searchSongs(query: String, categories: List<String>?): Flow<List<Song>> {
        val isFiltered = !categories.isNullOrEmpty() && !categories.contains("All")
        val ftsQuery = "*$query*"
        return combine(
            songDao.searchSongs(query, ftsQuery, categories ?: emptyList(), isFiltered),
            favoriteDao.getAllFavoriteSongIds()
        ) { list, favorites ->
            list.map { it.toDomain(favorites.contains(it.song.id)) }
        }
    }

    override fun searchSongsWithFuzzy(query: String, categories: List<String>?): Flow<SearchResponse> = flow {
        if (query.isBlank()) {
            emit(SearchResponse.Empty)
            return@flow
        }

        val isFiltered = !categories.isNullOrEmpty() && !categories.contains("All")
        val exactMatches = songDao.searchSongsFallback(query, categories ?: emptyList(), isFiltered).first()
        if (exactMatches.isNotEmpty()) {
            emitAll(
                favoriteDao.getAllFavoriteSongIds().map { favorites ->
                    SearchResponse.Results(exactMatches.map { it.toDomain(favorites.contains(it.song.id)) })
                }
            )
            return@flow
        }

        val normalizedQuery = SearchUtils.normalize(query)
        val candidates = searchCandidateCache.getCandidates()
        
        if (candidates.isEmpty()) {
            refreshSearchCandidates()
        }

        val results = searchCandidateCache.getCandidates()
            .map { candidate ->
                val similarity = SearchUtils.getSimilarity(normalizedQuery, candidate.normalizedText)
                SearchSuggestion(candidate.id, candidate.text, candidate.type, similarity, candidate.slug)
            }
            .filter { it.score > 0.3 || it.text.contains(query, ignoreCase = true) }
            .sortedByDescending { it.score }
            .take(20)

        if (results.isEmpty()) {
            emit(SearchResponse.Empty)
        } else {
            emit(SearchResponse.Suggestions(results.groupBy { it.type }))
        }
    }

    override suspend fun toggleFavorite(songId: Long): Boolean {
        val isFav = favoriteDao.isFavorite(songId)
        if (isFav) {
            favoriteDao.removeFavorite(songId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(songId = songId))
        }
        return !isFav
    }

    override suspend fun markAsViewed(songId: Long) {
        recentViewDao.insertRecentView(RecentViewEntity(songId = songId))
        recentViewDao.trimRecentViews()
        songDao.incrementViewCount(songId)
    }

    override suspend fun saveSongs(songs: List<Song>) {
        songDao.insertSongs(songs.map { it.toEntity() })
    }

    override suspend fun getLocalSongCount(): Int = songDao.getSongCount()

    override suspend fun getDatabaseSizeBytes(): Long {
        val dbFile = context.getDatabasePath(MaraLyricsDatabase.DATABASE_NAME)
        return listOf(dbFile.path, "${dbFile.path}-wal", "${dbFile.path}-shm")
            .sumOf { java.io.File(it).let { file -> if (file.exists()) file.length() else 0L } }
    }

    override suspend fun clearAllSongs() {
        songDao.deleteAllSongs()
    }

    override fun getSongsByArtist(artistId: Long): Flow<List<Song>> {
        return combine(
            songDao.getSongsByArtist(artistId),
            favoriteDao.getAllFavoriteSongIds()
        ) { list, favorites ->
            list.map { it.toDomain(favorites.contains(it.song.id)) }
        }
    }

    override fun getSongsByComposer(composerId: Long): Flow<List<Song>> {
        return combine(
            songDao.getSongsByComposer(composerId),
            favoriteDao.getAllFavoriteSongIds()
        ) { list, favorites ->
            list.map { it.toDomain(favorites.contains(it.song.id)) }
        }
    }

    override fun getAvailableCategories(): Flow<List<SongCategory>> {
        return songDao.getCategoriesWithCounts().map { list ->
            list.map { SongCategory(it.key, it.key, it.songCount) }
        }
    }

    override fun getFavoriteStats(): Flow<FavoriteStats> {
        return combine(
            favoriteDao.getFavoriteCount(),
            favoriteDao.getFavoriteArtistCount(),
            favoriteDao.getFavoriteComposerCount()
        ) { songCount, artistCount, composerCount ->
            FavoriteStats(songCount, artistCount, composerCount)
        }
    }

    override fun getFavoriteCategories(): Flow<List<String>> = favoriteDao.getFavoriteCategories()

    override suspend fun getFavoriteSongIds(): List<Long> = favoriteDao.getFavoriteIdsList()

    override suspend fun restoreFavorites(songIds: List<Long>) {
        favoriteDao.addFavorites(songIds.map { FavoriteEntity(songId = it) })
    }

    override fun getAllSongs(categories: List<String>?, sortOrder: SongSortOrder): Flow<List<Song>> {
        val isFiltered = !categories.isNullOrEmpty() && !categories.contains("All")
        val cats = categories ?: emptyList()
        val flow = when (sortOrder) {
            SongSortOrder.NUMBER_ASC -> songDao.getAllSongsByNumberAsc(cats, isFiltered)
            SongSortOrder.NUMBER_DESC -> songDao.getAllSongsByNumberDesc(cats, isFiltered)
            SongSortOrder.A_Z -> songDao.getAllSongsByTitleAsc(cats, isFiltered)
            SongSortOrder.Z_A -> songDao.getAllSongsByTitleDesc(cats, isFiltered)
            SongSortOrder.NEWEST -> songDao.getAllSongsByDateDesc(cats, isFiltered)
            SongSortOrder.OLDEST -> songDao.getAllSongsByDateAsc(cats, isFiltered)
            else -> songDao.getAllSongsByNumberAsc(cats, isFiltered)
        }
        return combine(flow, favoriteDao.getAllFavoriteSongIds()) { list, favorites ->
            list.map { it.toDomain(favorites.contains(it.song.id)) }
        }
    }

    override fun getSearchInitializationState(): Flow<SearchInitializationState> {
        return flow {
            emit(SearchInitializationState.LOADING_DATA)
            if (searchCandidateCache.isEmpty()) {
                refreshSearchCandidates()
            }
            emit(SearchInitializationState.READY)
        }
    }

    override suspend fun rebuildSearchIndex() {
        songDao.rebuildSearchIndex()
    }

    override suspend fun refreshSearchCandidates() {
        val raws = songDao.getSearchCandidates()
        val candidates = raws.map { 
            com.maralyrics.laitei.utils.Candidate(
                id = it.id,
                text = it.text,
                normalizedText = SearchUtils.normalize(it.text),
                type = SuggestionType.valueOf(it.type),
                views = it.views,
                slug = it.slug
            )
        }
        searchCandidateCache.update(candidates)
    }
}

@Singleton
class ArtistRepositoryImpl @Inject constructor(
    private val artistDao: ArtistDao,
    private val songDao: SongDao
) : ArtistRepository {
    override fun getArtists(): Flow<List<Artist>> = artistDao.getArtists().map { list ->
        list.map { it.toDomain() }
    }
    override suspend fun getArtistBySlug(slug: String): Artist? =
        artistDao.getArtistBySlug(slug)?.let { it.toDomain(songDao.getSongCountByArtist(it.id)) }
    override suspend fun getArtistById(id: Long): Artist? =
        artistDao.getArtistById(id)?.let { it.toDomain(songDao.getSongCountByArtist(it.id)) }
    override suspend fun saveArtists(artists: List<Artist>) {
        artistDao.insertArtists(artists.map { it.toEntity() })
    }
    override suspend fun clearAllArtists() = artistDao.deleteAll()
    override suspend fun getLocalArtistCount(): Int = artistDao.getArtistCount()
}

@Singleton
class ComposerRepositoryImpl @Inject constructor(
    private val composerDao: ComposerDao,
    private val songDao: SongDao
) : ComposerRepository {
    override fun getComposers(): Flow<List<Composer>> = composerDao.getComposers().map { list ->
        list.map { it.toDomain() }
    }
    override suspend fun getComposerBySlug(slug: String): Composer? =
        composerDao.getComposerBySlug(slug)?.let { it.toDomain(songDao.getSongCountByComposer(it.id)) }
    override suspend fun getComposerById(id: Long): Composer? =
        composerDao.getComposerById(id)?.let { it.toDomain(songDao.getSongCountByComposer(it.id)) }
    override suspend fun saveComposers(composers: List<Composer>) {
        composerDao.insertComposers(composers.map { it.toEntity() })
    }
    override suspend fun clearAllComposers() = composerDao.deleteAll()
    override suspend fun getLocalComposerCount(): Int = composerDao.getComposerCount()
}

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val apiService: MaraLyricsApiService,
    private val feedbackDao: FeedbackDao
) : FeedbackRepository {
    override suspend fun submitFeedback(feedback: Feedback): Result<Unit> {
        return try {
            val response = apiService.submitReport(ReportRequest(
                songSlug = feedback.songSlug,
                songTitle = feedback.songTitle,
                songArtist = feedback.artistName,
                reporterName = feedback.name,
                reporterEmail = feedback.email,
                body = feedback.message,
                turnstileToken = feedback.turnstileToken
            ))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to submit feedback"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveFeedbackLocally(feedback: Feedback) {
        feedbackDao.insertFeedback(feedback.toEntity())
    }

    override suspend fun getUnsyncedFeedback(): List<Feedback> {
        return feedbackDao.getUnsyncedFeedback().map { it.toDomain() }
    }

    override suspend fun markFeedbackAsSynced(id: Long) {
        feedbackDao.markSynced(id)
    }

    override suspend fun deleteSyncedFeedback() {
        feedbackDao.deleteSyncedFeedback()
    }
}

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val apiService: MaraLyricsApiService,
    private val songDao: SongDao,
    private val artistDao: ArtistDao,
    private val composerDao: ComposerDao,
    private val copyrightOwnerDao: CopyrightOwnerDao,
    private val settingsRepository: SettingsRepository,
    private val songRepository: SongRepository
) : SyncRepository {

    private val _updateStatus = MutableStateFlow<SyncStatus?>(null)
    override val updateStatus: StateFlow<SyncStatus?> = _updateStatus.asStateFlow()

    private fun validateStorage(requiredBytes: Long, stage: StorageUtils.StorageValidationStage): StorageUtils.SpaceInfo {
        val info = StorageUtils.getSpaceInfo(requiredBytes)
        android.util.Log.d(
            "StorageValidation",
            "stage=$stage available=${info.availableBytes} required=${info.requiredBytes} " +
                    "used=${info.usedPercentage} warning=${info.isLowStorageWarning} allowed=${info.isEnoughSpace}"
        )
        return info
    }

    override suspend fun checkForUpdates(): SyncStatus {
        val localVersion = settingsRepository.getLocalDatabaseVersion()
        val localCursor = settingsRepository.getLastSyncCursor()
        val localCounts = settingsRepository.getLastKnownCounts()

        return try {
            val response = apiService.getDatabaseVersion()
            if (response.isSuccessful) {
                val body = response.body() ?: return SyncStatus(false, localVersion, localVersion)
                val serverVersion = body.version
                val serverCursor = body.updatedAt
                val serverCounts = body.counts

                val isVersionNewer = serverVersion > localVersion
                val isCursorDifferent = serverCursor != localCursor
                val areCountsDifferent = serverCounts != localCounts
                
                val isUpdateAvailable = isVersionNewer || isCursorDifferent || areCountsDifferent

                if (!isUpdateAvailable) {
                    _updateStatus.value = null
                    return SyncStatus(
                        isAvailable = false,
                        localVersion = localVersion,
                        serverVersion = serverVersion,
                        newSongs = 0,
                        newArtists = 0,
                        newComposers = 0,
                        newCopyrightOwners = 0,
                        requiresFullRefresh = false
                    )
                }

                val deletionOccurred = serverCounts.any { (key, count) ->
                    val localCount = localCounts[key] ?: 0
                    (count as? Int ?: 0) < (localCount as? Int ?: 0)
                }

                val status = SyncStatus(
                    isAvailable = isUpdateAvailable,
                    localVersion = localVersion,
                    serverVersion = serverVersion,
                    newSongs = if (deletionOccurred) serverCounts["songs"] ?: 0 else (serverCounts["songs"] ?: 0) - (localCounts["songs"] ?: 0),
                    newArtists = if (deletionOccurred) serverCounts["artists"] ?: 0 else (serverCounts["artists"] ?: 0) - (localCounts["artists"] ?: 0),
                    newComposers = if (deletionOccurred) serverCounts["composers"] ?: 0 else (serverCounts["composers"] ?: 0) - (localCounts["composers"] ?: 0),
                    newCopyrightOwners = if (deletionOccurred) serverCounts["copyright_owners"] ?: 0 else (serverCounts["copyright_owners"] ?: 0) - (localCounts["copyright_owners"] ?: 0),
                    requiresFullRefresh = deletionOccurred || localCursor.isEmpty()
                )
                _updateStatus.value = if (isUpdateAvailable) status else null
                status
            } else {
                SyncStatus(false, localVersion, localVersion)
            }
        } catch (e: java.io.IOException) {
            // No connectivity (or the request otherwise couldn't reach the server) —
            // let this propagate so callers can tell it apart from a genuine "no update".
            throw e
        } catch (e: Exception) {
            SyncStatus(false, localVersion, localVersion)
        }
    }

    override suspend fun downloadBootstrap(onProgress: (DownloadProgress) -> Unit): Result<Int> {
        return try {
            val response = apiService.getBootstrap()
            if (response.isSuccessful) {
                val contentLength = response.headers()["Content-Length"]?.toLongOrNull()
                
                val requiredBytes = if (contentLength != null) {
                    StorageUtils.calculateRequiredSpace(contentLength, contentLength)
                } else {
                    StorageUtils.UNKNOWN_SIZE_THRESHOLD
                }
                
                val preDownloadInfo = validateStorage(requiredBytes, StorageUtils.StorageValidationStage.PRE_DOWNLOAD)
                if (!preDownloadInfo.isEnoughSpace) {
                    return Result.failure(InsufficientStorageException(preDownloadInfo))
                }

                val preExtractionInfo = validateStorage(requiredBytes, StorageUtils.StorageValidationStage.PRE_EXTRACTION)
                if (!preExtractionInfo.isEnoughSpace) {
                    return Result.failure(InsufficientStorageException(preExtractionInfo))
                }

                onProgress(DownloadProgress(0, 0, 0f, "Starting Full Download...", requiredBytes))
                val body = response.body() ?: return Result.failure(Exception("Empty response"))
                
                songDao.deleteAllSongs()
                songDao.deleteAllSongArtists()
                songDao.deleteAllSongComposers()
                artistDao.deleteAll()
                composerDao.deleteAll()
                copyrightOwnerDao.deleteAll()

                artistDao.insertArtists(body.artists.map { it.toEntity() })
                composerDao.insertComposers(body.composers.map { it.toEntity() })
                copyrightOwnerDao.insertAll(body.copyrightOwners.map { it.toEntity() })
                songDao.insertSongs(body.songs.map { it.toEntity() })

                val artistSlugToId = body.artists.associate { it.slug to it.id }
                val composerSlugToId = body.composers.associate { it.slug to it.id }

                val songArtists = body.songs.flatMap { dto ->
                    val contributors = dto.artists.ifEmpty { 
                        listOfNotNull(dto.artistSlug?.let { SongContributorDto(id = dto.artistId, name = dto.artistName ?: "", slug = it) }) 
                    }
                    contributors.mapIndexed { index, artist ->
                        val resolvedId = artist.id ?: artistSlugToId[artist.slug] ?: 0L
                        SongArtistEntity(dto.id, resolvedId, index)
                    }.filter { it.artistId != 0L }
                }
                android.util.Log.d("Sync", "Linking ${songArtists.size} song-artist relationships")
                songDao.insertSongArtists(songArtists)

                val songComposers = body.songs.flatMap { dto ->
                    val contributors = dto.composers.ifEmpty { 
                        listOfNotNull(dto.composerSlug?.let { SongContributorDto(id = dto.composerId, name = dto.composerName ?: "", slug = it) }) 
                    }
                    contributors.mapIndexed { index, composer ->
                        val resolvedId = composer.id ?: composerSlugToId[composer.slug] ?: 0L
                        SongComposerEntity(dto.id, resolvedId, index)
                    }.filter { it.composerId != 0L }
                }
                songDao.insertSongComposers(songComposers)

                onProgress(DownloadProgress(0, 0, 0.9f, "Finalizing...", requiredBytes))
                settingsRepository.updateDatabaseVersion(body.version)
                
                val versionResponse = apiService.getDatabaseVersion()
                if (versionResponse.isSuccessful) {
                    val vBody = versionResponse.body()
                    if (vBody != null) {
                        settingsRepository.updateLastSyncCursor(vBody.updatedAt)
                        settingsRepository.updateLastKnownCounts(vBody.counts)
                    }
                }

                songRepository.refreshSearchCandidates()
                Result.success(body.songs.size)
            } else {
                Result.failure(Exception("Bootstrap failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun downloadAllSongs(onProgress: (DownloadProgress) -> Unit): Result<Int> {
        return try {
            var page = 1
            var totalDownloaded = 0
            var totalSongs = 0
            do {
                val response = apiService.getAllSongs(page = page, limit = 100)
                if (!response.isSuccessful) return Result.failure(Exception("Songs download failed at page $page"))
                val body = response.body() ?: break
                if (page == 1) totalSongs = body.total
                val songs = body.songs.map { it.toEntity() }
                songDao.insertSongs(songs)

                val artistMap = artistDao.getArtists().first().associate { it.slug to it.id }
                val composerMap = composerDao.getComposers().first().associate { it.slug to it.id }

                val songArtists = body.songs.flatMap { dto ->
                    val contributors = dto.artists.ifEmpty { 
                        listOfNotNull(dto.artistSlug?.let { SongContributorDto(id = dto.artistId, name = dto.artistName ?: "", slug = it) }) 
                    }
                    contributors.mapIndexed { index, artist ->
                        val resolvedId = artist.id ?: artistMap[artist.slug] ?: 0L
                        SongArtistEntity(dto.id, resolvedId, index)
                    }.filter { it.artistId != 0L }
                }
                songDao.insertSongArtists(songArtists)

                val songComposers = body.songs.flatMap { dto ->
                    val contributors = dto.composers.ifEmpty { 
                        listOfNotNull(dto.composerSlug?.let { SongContributorDto(id = dto.composerId, name = dto.composerName ?: "", slug = it) }) 
                    }
                    contributors.mapIndexed { index, composer ->
                        val resolvedId = composer.id ?: composerMap[composer.slug] ?: 0L
                        SongComposerEntity(dto.id, resolvedId, index)
                    }.filter { it.composerId != 0L }
                }
                songDao.insertSongComposers(songComposers)

                totalDownloaded += songs.size
                onProgress(DownloadProgress(totalSongs, totalDownloaded, totalDownloaded.toFloat() / totalSongs, "Downloading Songs..."))
                page++
            } while (totalDownloaded < totalSongs)
            
            songRepository.refreshSearchCandidates()
            Result.success(totalDownloaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadAllArtists(onProgress: (DownloadProgress) -> Unit): Result<Int> {
        return try {
            var page = 1
            var totalDownloaded = 0
            var totalArtists = 0
            do {
                val response = apiService.getAllArtists(page = page, perPage = 100)
                if (!response.isSuccessful) return Result.failure(Exception("Artists download failed at page $page"))
                val body = response.body() ?: break
                if (page == 1) totalArtists = body.total
                val artists = body.artists.map { it.toEntity() }
                artistDao.insertArtists(artists)
                totalDownloaded += artists.size
                onProgress(DownloadProgress(totalArtists, totalDownloaded, totalDownloaded.toFloat() / totalArtists, "Downloading Artists..."))
                page++
            } while (totalDownloaded < totalArtists)
            Result.success(totalDownloaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadAllComposers(onProgress: (DownloadProgress) -> Unit): Result<Int> {
        return try {
            var page = 1
            var totalDownloaded = 0
            var totalComposers = 0
            do {
                val response = apiService.getAllComposers(page = page, perPage = 100)
                if (!response.isSuccessful) return Result.failure(Exception("Composers download failed at page $page"))
                val body = response.body() ?: break
                if (page == 1) totalComposers = body.total
                val composers = body.composers.map { it.toEntity() }
                composerDao.insertComposers(composers)
                totalDownloaded += composers.size
                onProgress(DownloadProgress(totalComposers, totalDownloaded, totalDownloaded.toFloat() / totalComposers, "Downloading Composers..."))
                page++
            } while (totalDownloaded < totalComposers)
            Result.success(totalDownloaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadIncrementalUpdate(since: String, onProgress: ((DownloadProgress) -> Unit)?): Result<Int> {
        return try {
            val response = apiService.getBootstrap(since = since)
            if (response.isSuccessful) {
                val contentLength = response.headers()["Content-Length"]?.toLongOrNull()

                val requiredBytes = if (contentLength != null) {
                    StorageUtils.calculateRequiredSpace(contentLength, contentLength)
                } else {
                    StorageUtils.UNKNOWN_SIZE_THRESHOLD
                }

                val preDownloadInfo = validateStorage(requiredBytes, StorageUtils.StorageValidationStage.PRE_DOWNLOAD)
                if (!preDownloadInfo.isEnoughSpace) {
                    return Result.failure(InsufficientStorageException(preDownloadInfo))
                }

                onProgress?.invoke(DownloadProgress(0, 0, 0f, "Checking for updates...", requiredBytes))

                val body = response.body() ?: return Result.failure(Exception("Empty response"))
                
                val totalItems = body.songs.size + body.artists.size + body.composers.size
                var processedItems = 0

                fun notifyProgress(label: String) {
                    val progress = if (totalItems > 0) processedItems.toFloat() / totalItems else 1f
                    onProgress?.invoke(DownloadProgress(processedItems, totalItems, progress, label, requiredBytes))
                }

                if (body.artists.isNotEmpty()) {
                    notifyProgress("Updating Artists...")
                    artistDao.insertArtists(body.artists.map { it.toEntity() })
                    processedItems += body.artists.size
                }

                if (body.composers.isNotEmpty()) {
                    notifyProgress("Updating Composers...")
                    composerDao.insertComposers(body.composers.map { it.toEntity() })
                    processedItems += body.composers.size
                }

                if (body.copyrightOwners.isNotEmpty()) {
                    notifyProgress("Updating Copyright Owners...")
                    copyrightOwnerDao.insertAll(body.copyrightOwners.map { it.toEntity() })
                    processedItems += body.copyrightOwners.size
                }

                if (body.songs.isNotEmpty()) {
                    notifyProgress("Updating Songs...")
                    songDao.insertSongs(body.songs.map { it.toEntity() })

                    val artistMap = artistDao.getArtists().first().associate { it.slug to it.id }
                    val composerMap = composerDao.getComposers().first().associate { it.slug to it.id }

                    body.songs.forEach { dto ->
                        songDao.deleteSongArtistsBySongId(dto.id)
                        songDao.deleteSongComposersBySongId(dto.id)
                    }

                    val songArtists = body.songs.flatMap { dto ->
                        val contributors = dto.artists.ifEmpty { 
                            listOfNotNull(dto.artistSlug?.let { SongContributorDto(id = dto.artistId, name = dto.artistName ?: "", slug = it) }) 
                        }
                        contributors.mapIndexed { index, artist ->
                            val resolvedId = artist.id ?: artistMap[artist.slug] ?: 0L
                            SongArtistEntity(dto.id, resolvedId, index)
                        }.filter { it.artistId != 0L }
                    }
                    songDao.insertSongArtists(songArtists)

                    val songComposers = body.songs.flatMap { dto ->
                        val contributors = dto.composers.ifEmpty { 
                            listOfNotNull(dto.composerSlug?.let { SongContributorDto(id = dto.composerId, name = dto.composerName ?: "", slug = it) }) 
                        }
                        contributors.mapIndexed { index, composer ->
                            val resolvedId = composer.id ?: composerMap[composer.slug] ?: 0L
                            SongComposerEntity(dto.id, resolvedId, index)
                        }.filter { it.composerId != 0L }
                    }
                    songDao.insertSongComposers(songComposers)
                    processedItems += body.songs.size
                }

                notifyProgress("Finalizing...")
                settingsRepository.updateDatabaseVersion(body.version)
                
                val versionResponse = apiService.getDatabaseVersion()
                if (versionResponse.isSuccessful) {
                    val vBody = versionResponse.body()
                    if (vBody != null) {
                        settingsRepository.updateLastSyncCursor(vBody.updatedAt)
                        settingsRepository.updateLastKnownCounts(vBody.counts)
                    }
                }

                songRepository.refreshSearchCandidates()
                Result.success(body.songs.size)
            } else {
                Result.failure(Exception("Incremental sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getServerVersion(): Result<Int> {
        return try {
            val response = apiService.getDatabaseVersion()
            if (response.isSuccessful) {
                Result.success(response.body()?.version ?: 0)
            } else {
                Result.failure(Exception("Failed to get server version: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLocalCopyrightOwnerCount(): Int {
        return copyrightOwnerDao.getCount()
    }
}
