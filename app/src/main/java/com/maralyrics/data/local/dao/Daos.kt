package com.maralyrics.data.local.dao

import androidx.room.*
import com.maralyrics.data.local.entity.FavoriteEntity
import com.maralyrics.data.local.entity.RecentViewEntity
import com.maralyrics.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs WHERE category = :category ORDER BY song_number ASC")
    fun getSongsByCategory(category: String): Flow<List<SongEntity>>

    @Query("SELECT s.* FROM songs s INNER JOIN recent_views r ON s.id = r.song_id ORDER BY r.viewed_at DESC LIMIT :limit")
    fun getRecentlyViewed(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT s.* FROM songs s INNER JOIN favorites f ON s.id = f.song_id ORDER BY f.added_at DESC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE category = :category ORDER BY view_count DESC LIMIT :limit")
    fun getPopularSongs(category: String, limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE category = :category ORDER BY RANDOM() LIMIT :limit")
    fun getRandomSongs(category: String, limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("""
        SELECT * FROM songs 
        WHERE category = :category AND (
            title LIKE '%' || :query || '%' OR 
            lyrics LIKE '%' || :query || '%' OR
            song_number LIKE '%' || :query || '%'
        )
        ORDER BY 
            CASE WHEN title LIKE :query || '%' THEN 0
                 WHEN title LIKE '%' || :query || '%' THEN 1
                 ELSE 2 END,
            song_number ASC
    """)
    fun searchSongs(query: String, category: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET view_count = view_count + 1 WHERE id = :songId")
    suspend fun incrementViewCount(songId: Long)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()
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