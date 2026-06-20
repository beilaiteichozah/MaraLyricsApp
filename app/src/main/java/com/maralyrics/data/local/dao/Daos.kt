package com.maralyrics.data.local.dao

import androidx.room.*
import com.maralyrics.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Transaction
    @Query("SELECT * FROM songs WHERE (:category IS NULL OR category = :category) ORDER BY id ASC")
    fun getSongsByCategory(category: String?): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT s.* FROM songs s INNER JOIN recent_views r ON s.id = r.song_id ORDER BY r.viewed_at DESC LIMIT :limit")
    fun getRecentlyViewed(limit: Int): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT s.* FROM songs s INNER JOIN favorites f ON s.id = f.song_id ORDER BY f.added_at DESC")
    fun getFavoriteSongs(): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:category IS NULL OR category = :category) ORDER BY views DESC LIMIT :limit")
    fun getPopularSongs(category: String?, limit: Int): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:category IS NULL OR category = :category) ORDER BY RANDOM() LIMIT :limit")
    fun getRandomSongs(category: String?, limit: Int): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongWithArtistAndComposer?

    @Transaction
    @Query("SELECT * FROM songs WHERE (:category IS NULL OR category = :category) ORDER BY id ASC")
    fun getAllSongsByNumberAsc(category: String?): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:category IS NULL OR category = :category) ORDER BY id DESC")
    fun getAllSongsByNumberDesc(category: String?): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:category IS NULL OR category = :category) ORDER BY title ASC")
    fun getAllSongsByTitleAsc(category: String?): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:category IS NULL OR category = :category) ORDER BY title DESC")
    fun getAllSongsByTitleDesc(category: String?): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:category IS NULL OR category = :category) ORDER BY created_at DESC")
    fun getAllSongsByDateDesc(category: String?): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE (:category IS NULL OR category = :category) ORDER BY created_at ASC")
    fun getAllSongsByDateAsc(category: String?): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE slug = :slug")
    suspend fun getSongBySlug(slug: String): SongWithArtistAndComposer?

    @Transaction
    @Query("SELECT * FROM songs WHERE artist_id = :artistId ORDER BY title ASC")
    fun getSongsByArtist(artistId: Long): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("SELECT * FROM songs WHERE composer_id = :composerId ORDER BY title ASC")
    fun getSongsByComposer(composerId: Long): Flow<List<SongWithArtistAndComposer>>

    @Query("SELECT COUNT(*) FROM songs WHERE artist_id = :artistId")
    suspend fun getSongCountByArtist(artistId: Long): Int

    @Query("SELECT COUNT(*) FROM songs WHERE composer_id = :composerId")
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
        AND (:category IS NULL OR s.category = :category)
        ORDER BY 
            CASE WHEN s.id = :query THEN 0 ELSE 1 END,
            s.id ASC
    """)
    fun searchSongs(query: String, ftsQuery: String, category: String?): Flow<List<SongWithArtistAndComposer>>

    @Transaction
    @Query("""
        SELECT s.* FROM songs s
        LEFT JOIN artists a ON s.artist_id = a.id
        LEFT JOIN composers c ON s.composer_id = c.id
        WHERE (:category IS NULL OR s.category = :category) AND (
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
    fun searchSongsFallback(query: String, category: String?): Flow<List<SongWithArtistAndComposer>>

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
        parentColumn = "artist_id",
        entityColumn = "id"
    )
    val artist: ArtistEntity? = null,
    @Relation(
        parentColumn = "composer_id",
        entityColumn = "id"
    )
    val composer: ComposerEntity? = null,
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
