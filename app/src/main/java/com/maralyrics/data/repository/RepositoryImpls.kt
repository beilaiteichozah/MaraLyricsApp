package com.maralyrics.data.repository

import android.util.Log
import com.maralyrics.data.local.dao.*
import com.maralyrics.data.local.entity.*
import com.maralyrics.data.remote.MaraLyricsApiService
import com.maralyrics.data.remote.dto.*
import com.maralyrics.domain.model.*
import com.maralyrics.domain.repository.*
import com.maralyrics.utils.Candidate
import com.maralyrics.utils.SearchCandidateCache
import com.maralyrics.utils.SearchUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val favoriteDao: FavoriteDao,
    private val recentViewDao: RecentViewDao,
    private val searchCandidateCache: SearchCandidateCache,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : SongRepository {

    override fun getSongsByCategory(category: String?): Flow<List<Song>> =
        songDao.getSongsByCategory(category)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.song.id)) }
            }

    override fun getRecentlyViewed(limit: Int): Flow<List<Song>> =
        songDao.getRecentlyViewed(limit)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.song.id)) }
            }

    override fun getFavoriteSongs(): Flow<List<Song>> =
        songDao.getFavoriteSongs().map { songs ->
            songs.map { it.toDomain(isFavorite = true) }
        }

    override fun getPopularSongs(category: String?, limit: Int): Flow<List<Song>> =
        songDao.getPopularSongs(category, limit)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.song.id)) }
            }

    override fun getRandomSongs(category: String?, limit: Int): Flow<List<Song>> =
        songDao.getRandomSongs(category, limit)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.song.id)) }
            }

    override suspend fun getSongById(id: Long): Song? {
        return songDao.getSongById(id)?.let { entity ->
            val isFav = favoriteDao.isFavorite(id)
            entity.toDomain(isFav)
        }
    }

    override suspend fun getSongBySlug(slug: String): Song? {
        return songDao.getSongBySlug(slug)?.let { entity ->
            val isFav = favoriteDao.isFavorite(entity.song.id)
            entity.toDomain(isFav)
        }
    }

    override fun searchSongs(query: String, category: String?): Flow<List<Song>> =
        searchSongsWithFuzzy(query, category).map { response ->
            when (response) {
                is SearchResponse.Results -> response.songs
                is SearchResponse.Suggestions -> emptyList() // Legacy API expects only songs
                is SearchResponse.Empty -> emptyList()
            }
        }

    override fun searchSongsWithFuzzy(query: String, category: String?): Flow<SearchResponse> {
        if (query.isBlank()) return flowOf(SearchResponse.Empty)

        val startTime = System.currentTimeMillis()
        val sanitizedQuery = query.trim()
        val ftsQuery = sanitizeFtsQuery(sanitizedQuery)

        return flow {
            // 1. Try Exact/FTS Search
            val results = fetchExactResults(sanitizedQuery, ftsQuery, category).first()
            
            if (results.isNotEmpty()) {
                emit(SearchResponse.Results(results))
                logSearch(sanitizedQuery, ftsQuery, results.size, startTime, "EXACT")
                return@flow
            }

            // 2. No exact results, try Fuzzy Suggestions if query >= 2
            if (sanitizedQuery.length >= 2) {
                val suggestions = calculateFuzzySuggestions(sanitizedQuery)
                if (suggestions.isNotEmpty()) {
                    emit(SearchResponse.Suggestions(suggestions))
                    val totalSuggestions = suggestions.values.sumOf { it.size }
                    logSearch(sanitizedQuery, ftsQuery, totalSuggestions, startTime, "FUZZY")
                    return@flow
                }
            }

            emit(SearchResponse.Empty)
            logSearch(sanitizedQuery, ftsQuery, 0, startTime, "EMPTY")
        }.onStart {
            ensureCandidatesCached()
        }.catch { e ->
            Log.e("Search", "Search failed", e)
            emit(SearchResponse.Empty)
        }
    }

    private fun fetchExactResults(query: String, ftsQuery: String, category: String?): Flow<List<Song>> {
        return flow {
            val isIndexReady = try {
                val exists = songDao.isSearchIndexPresent() > 0
                val songCount = songDao.getSongCount()
                val indexCount = songDao.getSearchIndexCount()
                exists && songCount == indexCount
            } catch (e: Exception) { false }

            if (isIndexReady && ftsQuery.isNotBlank()) {
                emitAll(songDao.searchSongs(query, "$ftsQuery*", category))
            } else {
                emitAll(songDao.searchSongsFallback(query, category))
            }
        }.combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
            songs.map { it.toDomain(favoriteIds.contains(it.song.id)) }
        }
    }

    private suspend fun calculateFuzzySuggestions(query: String): Map<SuggestionType, List<SearchSuggestion>> {
        return withContext(Dispatchers.Default) {
            val normalizedQuery = SearchUtils.normalize(query)
            val candidates = searchCandidateCache.getCandidates()
            
            // Performance guard: Limit candidate pool if too large
            val processedCandidates = if (candidates.size > 5000) {
                candidates.filter { it.type != SuggestionType.SONG || it.views > 0 }
                    .take(5000)
            } else candidates

            val scored = processedCandidates.map { candidate ->
                val similarity = SearchUtils.getSimilarity(normalizedQuery, candidate.normalizedText)
                SearchSuggestion(
                    id = candidate.id,
                    text = candidate.text,
                    type = candidate.type,
                    score = similarity,
                    slug = candidate.slug
                )
            }

            // Dynamic thresholding
            var threshold = 0.60
            var results = scored.filter { it.score >= threshold }
            
            while (results.size < 7 && threshold > 0.40) {
                threshold -= 0.05
                results = scored.filter { it.score >= threshold }
            }

            results.sortedByDescending { it.score }
                .distinctBy { it.text + it.type } // Avoid dupes if same text appears in multiple songs
                .take(7)
                .groupBy { it.type }
        }
    }

    private suspend fun ensureCandidatesCached() {
        if (searchCandidateCache.isEmpty()) {
            refreshSearchCandidates()
        }
    }

    override suspend fun refreshSearchCandidates() {
        withContext(Dispatchers.IO) {
            try {
                val raw = songDao.getSearchCandidates()
                val candidates = raw.map {
                    Candidate(
                        id = it.id,
                        text = it.text,
                        normalizedText = SearchUtils.normalize(it.text),
                        type = SuggestionType.valueOf(it.type),
                        views = it.views,
                        slug = it.slug
                    )
                }
                searchCandidateCache.update(candidates)
                Log.d("Search", "Cache refreshed: ${candidates.size} candidates")
            } catch (e: Exception) {
                Log.e("Search", "Failed to refresh candidates", e)
            }
        }
    }

    private fun logSearch(query: String, fts: String, count: Int, start: Long, type: String) {
        val duration = System.currentTimeMillis() - start
        Log.d("Search", "[$type] Query: $query | FTS: $fts* | Count: $count | Time: ${duration}ms")
    }

    private fun sanitizeFtsQuery(query: String): String {
        return query.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ") // Keep only letters, numbers, spaces
            .replace(Regex("\\s+"), " ") // Normalize spaces
            .trim()
    }

    override suspend fun toggleFavorite(songId: Long) {
        if (favoriteDao.isFavorite(songId)) {
            favoriteDao.removeFavorite(songId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(songId = songId))
        }
    }

    override suspend fun markAsViewed(songId: Long) {
        recentViewDao.deleteRecentView(songId)
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

    override suspend fun getFavoriteSongIds(): List<Long> = favoriteDao.getFavoriteIdsList()

    override suspend fun restoreFavorites(songIds: List<Long>) {
        favoriteDao.addFavorites(songIds.map { FavoriteEntity(songId = it) })
    }

    override fun getAllSongs(category: String?, sortOrder: SongSortOrder): Flow<List<Song>> {
        val flow = when (sortOrder) {
            SongSortOrder.NUMBER_ASC -> songDao.getAllSongsByNumberAsc(category)
            SongSortOrder.NUMBER_DESC -> songDao.getAllSongsByNumberDesc(category)
            SongSortOrder.A_Z, SongSortOrder.Z_A -> songDao.getAllSongsByTitleAsc(category)
            SongSortOrder.NEWEST -> songDao.getAllSongsByDateDesc(category)
            SongSortOrder.OLDEST -> songDao.getAllSongsByDateAsc(category)
        }
        return flow.combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
            val domainSongs = songs.map { it.toDomain(favoriteIds.contains(it.song.id)) }
            
            if (sortOrder == SongSortOrder.A_Z || sortOrder == SongSortOrder.Z_A) {
                domainSongs.sortedWith { s1, s2 ->
                    val cmp = com.maralyrics.utils.MaraAlphabetUtils.compare(s1.title, s2.title)
                    if (sortOrder == SongSortOrder.A_Z) cmp else -cmp
                }
            } else {
                domainSongs
            }
        }
    }

    override fun getSearchInitializationState(): Flow<SearchInitializationState> = flow {
        while (true) {
            val songCount = songDao.getSongCount()
            val indexCount = try {
                songDao.getSearchIndexCount()
            } catch (e: Exception) {
                0
            }
            
            val state = when {
                songCount == 0 -> SearchInitializationState.LOADING_DATA
                indexCount < songCount -> SearchInitializationState.BUILDING_INDEX
                else -> SearchInitializationState.READY
            }
            emit(state)
            if (state == SearchInitializationState.READY) break
            kotlinx.coroutines.delay(1000)
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    override suspend fun rebuildSearchIndex() {
        songDao.rebuildSearchIndex()
    }

    override fun getSongsByArtist(artistId: Long): Flow<List<Song>> =
        songDao.getSongsByArtist(artistId)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.song.id)) }
            }

    override fun getSongsByComposer(composerId: Long): Flow<List<Song>> =
        songDao.getSongsByComposer(composerId)
            .combine(favoriteDao.getAllFavoriteSongIds()) { songs, favoriteIds ->
                songs.map { it.toDomain(favoriteIds.contains(it.song.id)) }
            }

    override fun getAvailableCategories(): Flow<List<SongCategory>> =
        songDao.getCategoriesWithCounts().map { counts ->
            counts.map { count ->
                SongCategory(
                    key = count.key,
                    displayName = count.key, // Can be mapped to localized string in UI if needed
                    songCount = count.songCount
                )
            }
        }

    private fun SongWithArtistAndComposer.toDomain(isFavorite: Boolean = false) = Song(
        id = song.id,
        slug = song.slug,
        title = song.title,
        lyrics = song.lyrics,
        category = if (song.category.isNullOrBlank() || song.category == "Uncategorized") "Uncategorized" else song.category,
        createdAt = song.createdAt,
        artistId = song.artistId,
        artistName = artist?.name,
        artistSlug = artist?.slug,
        composerId = song.composerId,
        composerName = composer?.name,
        composerSlug = composer?.slug,
        copyrightOwnerId = song.copyrightOwnerId,
        copyrightOwnerName = copyrightOwner?.name,
        isFavorite = isFavorite,
        views = song.views
    )

    private fun Song.toEntity() = SongEntity(
        id = id,
        slug = slug,
        title = title,
        lyrics = lyrics,
        category = category,
        createdAt = createdAt,
        artistId = artistId,
        composerId = composerId,
        copyrightOwnerId = copyrightOwnerId,
        views = views
    )
}

@Singleton
class ArtistRepositoryImpl @Inject constructor(
    private val artistDao: ArtistDao,
    private val songDao: SongDao
) : ArtistRepository {
    override fun getArtists(): Flow<List<Artist>> = artistDao.getArtists().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getArtistBySlug(slug: String): Artist? {
        return artistDao.getArtistBySlug(slug)?.let { entity ->
            val count = songDao.getSongCountByArtist(entity.id)
            entity.toDomain(count)
        }
    }

    override suspend fun getArtistById(id: Long): Artist? {
        return artistDao.getArtistById(id)?.let { entity ->
            val count = songDao.getSongCountByArtist(entity.id)
            entity.toDomain(count)
        }
    }

    override suspend fun saveArtists(artists: List<Artist>) {
        artistDao.insertArtists(artists.map { it.toEntity() })
    }

    override suspend fun clearAllArtists() = artistDao.deleteAll()

    private fun ArtistEntity.toDomain(songCount: Int = 0) = Artist(
        id = id,
        slug = slug,
        name = name,
        bio = bio,
        imageUrl = imageUrl,
        socialLinks = socialLinks?.let { 
            try {
                val map = Json.decodeFromString<Map<String, String>>(it)
                map.map { (platform, url) -> SocialLink(platform, url) }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList(),
        songCount = songCount
    )

    private fun Artist.toEntity() = ArtistEntity(
        id = id,
        slug = slug,
        name = name,
        bio = bio,
        imageUrl = imageUrl,
        socialLinks = Json.encodeToString(socialLinks.associate { it.platform to it.url })
    )
}

@Singleton
class ComposerRepositoryImpl @Inject constructor(
    private val composerDao: ComposerDao,
    private val songDao: SongDao
) : ComposerRepository {
    override fun getComposers(): Flow<List<Composer>> = composerDao.getComposers().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getComposerBySlug(slug: String): Composer? {
        return composerDao.getComposerBySlug(slug)?.let { entity ->
            val count = songDao.getSongCountByComposer(entity.id)
            entity.toDomain(count)
        }
    }

    override suspend fun getComposerById(id: Long): Composer? {
        return composerDao.getComposerById(id)?.let { entity ->
            val count = songDao.getSongCountByComposer(entity.id)
            entity.toDomain(count)
        }
    }

    override suspend fun saveComposers(composers: List<Composer>) {
        composerDao.insertComposers(composers.map { it.toEntity() })
    }

    override suspend fun clearAllComposers() = composerDao.deleteAll()

    private fun ComposerEntity.toDomain(songCount: Int = 0) = Composer(
        id = id,
        slug = slug,
        name = name,
        bio = bio,
        imageUrl = imageUrl,
        socialLinks = socialLinks?.let { 
            try {
                val map = Json.decodeFromString<Map<String, String>>(it)
                map.map { (platform, url) -> SocialLink(platform, url) }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList(),
        songCount = songCount
    )

    private fun Composer.toEntity() = ComposerEntity(
        id = id,
        slug = slug,
        name = name,
        bio = bio,
        imageUrl = imageUrl,
        socialLinks = Json.encodeToString(socialLinks.associate { it.platform to it.url })
    )
}

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val feedbackDao: FeedbackDao,
    private val apiService: MaraLyricsApiService
) : FeedbackRepository {
    override suspend fun submitFeedback(feedback: Feedback): Result<Unit> {
        return try {
            val response = apiService.submitReport(
                ReportRequest(
                    songSlug = feedback.songSlug,
                    songTitle = feedback.songTitle,
                    songArtist = feedback.artistName,
                    reporterName = feedback.name,
                    reporterEmail = feedback.email,
                    body = feedback.message
                )
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Submission failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveFeedbackLocally(feedback: Feedback) {
        feedbackDao.insertFeedback(
            FeedbackEntity(
                songId = feedback.songId,
                songSlug = feedback.songSlug,
                songTitle = feedback.songTitle,
                songArtist = feedback.artistName,
                name = feedback.name,
                email = feedback.email,
                message = feedback.message
            )
        )
    }

    override suspend fun getUnsyncedFeedback(): List<Feedback> {
        return feedbackDao.getUnsyncedFeedback().map { entity ->
            Feedback(
                id = entity.id,
                songId = entity.songId,
                songSlug = entity.songSlug,
                songTitle = entity.songTitle,
                artistName = entity.songArtist,
                name = entity.name,
                email = entity.email,
                message = entity.message,
                isSynced = entity.isSynced
            )
        }
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

    override suspend fun checkForUpdates(): SyncStatus {
        val localVersion = settingsRepository.getLocalDatabaseVersion()
        val serverVersion = getServerVersion().getOrDefault(localVersion)
        return SyncStatus(
            isAvailable = serverVersion > localVersion,
            localVersion = localVersion,
            serverVersion = serverVersion
        )
    }

    override suspend fun downloadBootstrap(onProgress: (DownloadProgress) -> Unit): Result<Int> {
        return try {
            onProgress(DownloadProgress(0, 0, 0f, "Starting Download..."))
            
            // 1. Download Artists
            val artistResult = downloadAllArtists { progress -> onProgress(progress) }
            if (artistResult.isFailure) return artistResult

            // 2. Download Composers
            val composerResult = downloadAllComposers { progress -> onProgress(progress) }
            if (composerResult.isFailure) return composerResult

            // 3. Download Songs
            val songResult = downloadAllSongs { progress -> onProgress(progress) }
            if (songResult.isFailure) return songResult

            songRepository.refreshSearchCandidates()
            Result.success(songResult.getOrDefault(0))
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
                val response = apiService.getAllSongs(page = page)
                if (!response.isSuccessful) return Result.failure(Exception("Songs download failed at page $page"))
                val body = response.body() ?: break
                if (page == 1) totalSongs = body.total
                val songs = body.songs.map { it.toEntity() }
                songDao.insertSongs(songs)
                totalDownloaded += songs.size
                onProgress(DownloadProgress(totalSongs, totalDownloaded, if(totalSongs > 0) totalDownloaded.toFloat()/totalSongs * 100 else 0f, "Songs"))
                page++
            } while (totalDownloaded < totalSongs && totalSongs > 0)
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
                val response = apiService.getAllArtists(page = page)
                if (!response.isSuccessful) return Result.failure(Exception("Artists download failed at page $page"))
                val body = response.body() ?: break
                if (page == 1) totalArtists = body.total
                val artists = body.artists.map { it.toEntity() }
                artistDao.insertArtists(artists)
                totalDownloaded += artists.size
                onProgress(DownloadProgress(totalArtists, totalDownloaded, if(totalArtists > 0) totalDownloaded.toFloat()/totalArtists * 100 else 0f, "Artists"))
                page++
            } while (totalDownloaded < totalArtists && totalArtists > 0)
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
                val response = apiService.getAllComposers(page = page)
                if (!response.isSuccessful) return Result.failure(Exception("Composers download failed at page $page"))
                val body = response.body() ?: break
                if (page == 1) totalComposers = body.total
                val composers = body.composers.map { it.toEntity() }
                composerDao.insertComposers(composers)
                totalDownloaded += composers.size
                onProgress(DownloadProgress(totalComposers, totalDownloaded, if(totalComposers > 0) totalDownloaded.toFloat()/totalComposers * 100 else 0f, "Composers"))
                page++
            } while (totalDownloaded < totalComposers && totalComposers > 0)
            Result.success(totalDownloaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadIncrementalUpdate(sinceVersion: Int): Result<Int> {
        // Ensure artists and composers are synced first for relationship consistency
        downloadAllArtists { }
        downloadAllComposers { }
        val result = downloadAllSongs { }
        if (result.isSuccess) {
            songRepository.refreshSearchCandidates()
        }
        return result
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

    private fun String.toEpochMillis(): Long {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            sdf.parse(this)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun SongDto.toEntity() = SongEntity(
        id = id,
        slug = slug,
        title = title,
        lyrics = lyrics,
        category = category ?: "Uncategorized",
        createdAt = createdAt.toEpochMillis(),
        views = views,
        artistId = artistId,
        composerId = composerId,
        copyrightOwnerId = copyrightOwnerId
    )

    private fun ArtistDto.toEntity() = ArtistEntity(
        id = id,
        slug = slug,
        name = name,
        bio = bio,
        imageUrl = imageUrl,
        socialLinks = socialLinks?.let { Json.encodeToString(it) }
    )

    private fun ComposerDto.toEntity() = ComposerEntity(
        id = id,
        slug = slug,
        name = name,
        bio = bio,
        imageUrl = imageUrl,
        socialLinks = socialLinks?.let { Json.encodeToString(it) }
    )

    private fun CopyrightOwnerDto.toEntity() = CopyrightOwnerEntity(
        id = id,
        name = name,
        slug = slug,
        fullLegalName = fullLegalName,
        organization = organization,
        territory = territory,
        email = email,
        website = website,
        address = address,
        ipiNumber = ipiNumber,
        isrcPrefix = isrcPrefix,
        proAffiliation = proAffiliation,
        notes = notes,
        createdAt = createdAt.toEpochMillis()
    )
}
