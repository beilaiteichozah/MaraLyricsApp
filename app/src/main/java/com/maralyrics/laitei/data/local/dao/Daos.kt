package com.maralyrics.laitei.data.local.dao

import androidx.room.*
import com.maralyrics.laitei.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Transaction
    @Query("SELECT * FROM songs WHERE (:isFiltered = 0 OR category IN (:categories)) ORDER BY id ASC")
    fun getSongsByCategories(categories: List<String>, isFiltered: Boolean): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT s.* FROM songs s INNER JOIN recent_views r ON s.id = r.song_id ORDER BY r.viewed_at DESC LIMIT :limit")
    fun getRecentlyViewed(limit: Int): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT s.* FROM songs s INNER JOIN favorites f ON s.id = f.song_id ORDER BY f.favorited_at DESC")
    fun getFavoriteSongs(): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:isFiltered = 0 OR category IN (:categories)) ORDER BY views DESC LIMIT :limit")
    fun getPopularSongs(categories: List<String>, isFiltered: Boolean, limit: Int): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:isFiltered = 0 OR category IN (:categories)) ORDER BY RANDOM() LIMIT :limit")
    fun getRandomSongs(categories: List<String>, isFiltered: Boolean, limit: Int): Flow<List<SongWithArtistAndComposer>>

    // Plain entity fetch (no @Relation join) for the Featured Lyrics widget — only the
    // song's own denormalized fields (title/artistName/lyrics) are needed here, and the
    // widget/notifier run outside the usual screen flows where a song is always opened
    // by id, so this avoids a relation-population edge case some rows can hit.
    @Query("SELECT * FROM songs ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomSongEntity(): SongEntity?

    @Transaction
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongWithArtistAndComposer?

    @Transaction
    @Query("SELECT * FROM songs WHERE (:isFiltered = 0 OR category IN (:categories)) ORDER BY id ASC")
    fun getAllSongsByNumberAsc(categories: List<String>, isFiltered: Boolean): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:isFiltered = 0 OR category IN (:categories)) ORDER BY id DESC")
    fun getAllSongsByNumberDesc(categories: List<String>, isFiltered: Boolean): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:isFiltered = 0 OR category IN (:categories)) ORDER BY title ASC")
    fun getAllSongsByTitleAsc(categories: List<String>, isFiltered: Boolean): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:isFiltered = 0 OR category IN (:categories)) ORDER BY title DESC")
    fun getAllSongsByTitleDesc(categories: List<String>, isFiltered: Boolean): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:isFiltered = 0 OR category IN (:categories)) ORDER BY created_at DESC")
    fun getAllSongsByDateDesc(categories: List<String>, isFiltered: Boolean): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:isFiltered = 0 OR category IN (:categories)) ORDER BY created_at ASC")
    fun getAllSongsByDateAsc(categories: List<String>, isFiltered: Boolean): Flow<List<SongWithArtistAndComposer>>

    // Plain entity fetch (no @Relation join) — see getRandomSongEntity() for why.
    // Used to personalize the "new song available" notification for exactly one new song.
    @Query("SELECT * FROM songs ORDER BY created_at DESC LIMIT 1")
    suspend fun getMostRecentSongEntity(): SongEntity?

    @Transaction
    @Query("SELECT * FROM songs WHERE slug = :slug")
    suspend fun getSongBySlug(slug: String): SongWithArtistAndComposer?

    @Transaction
    @Query("SELECT s.* FROM songs s INNER JOIN song_artists sa ON s.id = sa.song_id WHERE sa.artist_id = :artistId ORDER BY s.title ASC")
    fun getSongsByArtist(artistId: Long): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT s.* FROM songs s INNER JOIN song_composers sc ON s.id = sc.song_id WHERE sc.composer_id = :composerId ORDER BY s.title ASC")
    fun getSongsByComposer(composerId: Long): Flow<List<SongWithArtistAndComposer>>

    @Query("SELECT COUNT(*) FROM song_artists WHERE artist_id = :artistId")
    suspend fun getSongCountByArtist(artistId: Long): Int

    @Query("SELECT COUNT(*) FROM song_composers WHERE composer_id = :composerId")
    suspend fun getSongCountByComposer(composerId: Long): Int

    @Transaction
    @Query("""
        SELECT s.* FROM songs s
        WHERE s.id IN (
            SELECT rowid FROM songs_search_index WHERE songs_search_index MATCH :ftsQuery
            UNION
            SELECT id FROM songs WHERE id = :query
            UNION
            SELECT s2.id FROM songs s2 JOIN artists a ON s2.artist_id = a.id WHERE a.name LIKE '%' || :query || '%'
            UNION
            SELECT s3.id FROM songs s3 JOIN composers c ON s3.composer_id = c.id WHERE c.name LIKE '%' || :query || '%'
        )
        AND (:isFiltered = 0 OR s.category IN (:categories))
        ORDER BY 
            CASE WHEN s.id = :query THEN 0 ELSE 1 END,
            s.id ASC
    """)
    fun searchSongs(query: String, ftsQuery: String, categories: List<String>, isFiltered: Boolean): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("""
        SELECT s.* FROM songs s
        LEFT JOIN artists a ON s.artist_id = a.id
        LEFT JOIN composers c ON s.composer_id = c.id
        WHERE (:isFiltered = 0 OR s.category IN (:categories)) AND (
            s.id = :query OR
            s.title LIKE '%' || :query || '%' OR
            s.lyrics LIKE '%' || :query || '%' OR
            a.name LIKE '%' || :query || '%' OR
            c.name LIKE '%' || :query || '%'
        )
        ORDER BY 
            CASE WHEN s.id = :query THEN 0 ELSE 1 END,
            s.id ASC
    """)
    fun searchSongsFallback(query: String, categories: List<String>, isFiltered: Boolean): Flow<List<SongWithArtistAndComposer>>

    @Query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='songs_search_index'")
    suspend fun isSearchIndexPresent(): Int

    @Query("SELECT COUNT(*) FROM songs_search_index")
    suspend fun getSearchIndexCount(): Int

    @Query("INSERT INTO songs_search_index(rowid, title, lyrics) SELECT id, title, lyrics FROM songs")
    suspend fun populateSearchIndex()

    @Query("DELETE FROM songs_search_index")
    suspend fun clearSearchIndex()

    @Transaction
    suspend fun rebuildSearchIndex() {
        clearSearchIndex()
        populateSearchIndex()
    }

    @Query("SELECT category as `key`, COUNT(*) as songCount FROM songs GROUP BY category ORDER BY category ASC")
    fun getCategoriesWithCounts(): Flow<List<CategoryCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongArtists(songArtists: List<SongArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongComposers(songComposers: List<SongComposerEntity>)

    @Query("UPDATE songs SET views = views + 1 WHERE id = :songId")
    suspend fun incrementViewCount(songId: Long)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Transaction
    @Query("""
        SELECT id, title as text, views, slug, 'SONG' as type FROM songs
        UNION ALL
        SELECT id, name as text, 0 as views, slug, 'ARTIST' as type FROM artists
        UNION ALL
        SELECT id, name as text, 0 as views, slug, 'COMPOSER' as type FROM composers
    """)
    suspend fun getSearchCandidates(): List<SearchCandidateRaw>

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("DELETE FROM song_artists")
    suspend fun deleteAllSongArtists()

    @Query("DELETE FROM song_composers")
    suspend fun deleteAllSongComposers()

    @Query("DELETE FROM song_artists WHERE song_id = :songId")
    suspend fun deleteSongArtistsBySongId(songId: Long)

    @Query("DELETE FROM song_composers WHERE song_id = :songId")
    suspend fun deleteSongComposersBySongId(songId: Long)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)
}

data class SearchCandidateRaw(
    val id: Long,
    val text: String,
    val views: Int,
    val slug: String,
    val type: String
)

data class SongWithArtistAndComposer(
    @Embedded val song: SongEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SongArtistEntity::class,
            parentColumn = "song_id",
            entityColumn = "artist_id"
        )
    )
    val artists: List<ArtistEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SongComposerEntity::class,
            parentColumn = "song_id",
            entityColumn = "composer_id"
        )
    )
    val composers: List<ComposerEntity> = emptyList(),
    @Relation(
        parentColumn = "copyright_owner_id",
        entityColumn = "id"
    )
    val copyrightOwner: CopyrightOwnerEntity? = null
)

@Dao
interface CopyrightOwnerDao {
    @Query("SELECT * FROM copyright_owners WHERE id = :id")
    suspend fun getById(id: Long): CopyrightOwnerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(owners: List<CopyrightOwnerEntity>)

    @Query("DELETE FROM copyright_owners")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM copyright_owners")
    suspend fun getCount(): Int
}

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getArtistById(id: Long): ArtistEntity?

    @Query("SELECT * FROM artists WHERE slug = :slug")
    suspend fun getArtistBySlug(slug: String): ArtistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Query("SELECT COUNT(*) FROM artists")
    suspend fun getArtistCount(): Int

    @Query("DELETE FROM artists")
    suspend fun deleteAll()
}

@Dao
interface ComposerDao {
    @Query("SELECT * FROM composers ORDER BY name ASC")
    fun getComposers(): Flow<List<ComposerEntity>>

    @Query("SELECT * FROM composers WHERE id = :id")
    suspend fun getComposerById(id: Long): ComposerEntity?

    @Query("SELECT * FROM composers WHERE slug = :slug")
    suspend fun getComposerBySlug(slug: String): ComposerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComposers(composers: List<ComposerEntity>)

    @Query("SELECT COUNT(*) FROM composers")
    suspend fun getComposerCount(): Int

    @Query("DELETE FROM composers")
    suspend fun deleteAll()
}

@Dao
interface FavoriteDao {
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE song_id = :songId)")
    suspend fun isFavorite(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE song_id = :songId")
    suspend fun removeFavorite(songId: Long)

    @Query("SELECT song_id FROM favorites")
    fun getAllFavoriteSongIds(): Flow<List<Long>>

    @Query("SELECT song_id FROM favorites")
    suspend fun getFavoriteIdsList(): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorites(favorites: List<FavoriteEntity>)

    @Query("SELECT COUNT(*) FROM favorites")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT s.artist_id) FROM favorites f JOIN songs s ON f.song_id = s.id")
    fun getFavoriteArtistCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT s.composer_id) FROM favorites f JOIN songs s ON f.song_id = s.id")
    fun getFavoriteComposerCount(): Flow<Int>

    @Query("SELECT DISTINCT s.category FROM favorites f JOIN songs s ON f.song_id = s.id WHERE s.category IS NOT NULL AND s.category != ''")
    fun getFavoriteCategories(): Flow<List<String>>
}

@Dao
interface RecentViewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentView(recentView: RecentViewEntity)

    @Query("DELETE FROM recent_views WHERE song_id = :songId")
    suspend fun deleteRecentView(songId: Long)

    @Query("""
        DELETE FROM recent_views WHERE id NOT IN (
            SELECT id FROM recent_views ORDER BY viewed_at DESC LIMIT 50
        )
    """)
    suspend fun trimRecentViews()
}

@Dao
interface FeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity)

    @Query("SELECT * FROM feedback WHERE is_synced = 0")
    suspend fun getUnsyncedFeedback(): List<FeedbackEntity>

    @Query("UPDATE feedback SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM feedback WHERE is_synced = 1")
    suspend fun deleteSyncedFeedback()
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE `key` = :key")
    suspend fun getMetadata(key: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: SyncMetadataEntity)
}

data class CategoryCount(
    val key: String,
    val songCount: Int
)
