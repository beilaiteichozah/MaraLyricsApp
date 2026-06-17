package com.maralyrics.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.maralyrics.data.local.dao.FavoriteDao
import com.maralyrics.data.local.dao.RecentViewDao
import com.maralyrics.data.local.dao.SongDao
import com.maralyrics.data.local.entity.FavoriteEntity
import com.maralyrics.data.local.entity.RecentViewEntity
import com.maralyrics.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        FavoriteEntity::class,
        RecentViewEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MaraLyricsDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentViewDao(): RecentViewDao

    companion object {
        const val DATABASE_NAME = "mara_lyrics.db"
    }
}