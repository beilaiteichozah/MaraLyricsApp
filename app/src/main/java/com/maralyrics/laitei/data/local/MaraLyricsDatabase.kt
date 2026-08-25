package com.maralyrics.laitei.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.maralyrics.laitei.data.local.dao.*
import com.maralyrics.laitei.data.local.entity.*

@Database(
    entities = [
        SongEntity::class,
        ArtistEntity::class,
        ComposerEntity::class,
        CopyrightOwnerEntity::class,
        FavoriteEntity::class,
        RecentViewEntity::class,
        FeedbackEntity::class,
        SyncMetadataEntity::class,
        SongArtistEntity::class,
        SongComposerEntity::class,
        SongFtsEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class MaraLyricsDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun artistDao(): ArtistDao
    abstract fun composerDao(): ComposerDao
    abstract fun copyrightOwnerDao(): CopyrightOwnerDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentViewDao(): RecentViewDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        const val DATABASE_NAME = "mara_lyrics.db"

        // Must match the @Database(version = ...) above — used to reject an
        // imported database file from an incompatible app version before it
        // silently gets wiped by fallbackToDestructiveMigration().
        const val SCHEMA_VERSION = 10

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN copyright_owner_name TEXT")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create song_artists table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `song_artists` (
                        `song_id` INTEGER NOT NULL, 
                        `artist_id` INTEGER NOT NULL, 
                        `position` INTEGER NOT NULL DEFAULT 0, 
                        PRIMARY KEY(`song_id`, `artist_id`), 
                        FOREIGN KEY(`song_id`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`artist_id`) REFERENCES `artists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artists_artist_id` ON `song_artists` (`artist_id`)")

                // Create song_composers table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `song_composers` (
                        `song_id` INTEGER NOT NULL, 
                        `composer_id` INTEGER NOT NULL, 
                        `position` INTEGER NOT NULL DEFAULT 0, 
                        PRIMARY KEY(`song_id`, `composer_id`), 
                        FOREIGN KEY(`song_id`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`composer_id`) REFERENCES `composers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_composers_composer_id` ON `song_composers` (`composer_id`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN artist_name TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN artist_slug TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN composer_name TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN composer_slug TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename added_at to favorited_at in favorites table
                // Room migrations for column renames usually involve recreating the table if it's not SQLite 3.25.0+ 
                // but let's see if ALTER TABLE RENAME COLUMN works or use traditional way
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `favorites_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `song_id` INTEGER NOT NULL, 
                        `favorited_at` INTEGER NOT NULL,
                        FOREIGN KEY(`song_id`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
                db.execSQL("INSERT INTO favorites_new (id, song_id, favorited_at) SELECT id, song_id, added_at FROM favorites")
                db.execSQL("DROP TABLE favorites")
                db.execSQL("ALTER TABLE favorites_new RENAME TO favorites")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_song_id` ON `favorites` (`song_id`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create FTS table
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `songs_search_index` USING FTS4(
                        `title`, `lyrics`, content=`songs`
                    )
                """)
                // Populate FTS table
                db.execSQL("INSERT INTO songs_search_index(rowid, title, lyrics) SELECT id, title, lyrics FROM songs")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Update feedback table to include song_slug, song_title, song_artist
                // and allow email to be null
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `feedback_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `song_id` INTEGER NOT NULL, 
                        `song_slug` TEXT NOT NULL DEFAULT '', 
                        `song_title` TEXT NOT NULL DEFAULT '', 
                        `song_artist` TEXT, 
                        `name` TEXT NOT NULL, 
                        `email` TEXT, 
                        `message` TEXT NOT NULL, 
                        `is_synced` INTEGER NOT NULL, 
                        `created_at` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO feedback_new (id, song_id, name, email, message, is_synced, created_at)
                    SELECT id, song_id, name, email, message, is_synced, created_at FROM feedback
                """)
                db.execSQL("DROP TABLE feedback")
                db.execSQL("ALTER TABLE feedback_new RENAME TO feedback")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove song_number, updated_at, view_count by recreating the table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `songs_new` (
                        `id` INTEGER NOT NULL, 
                        `slug` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `lyrics` TEXT NOT NULL, 
                        `category` TEXT, 
                        `created_at` INTEGER NOT NULL, 
                        `views` INTEGER NOT NULL DEFAULT 0, 
                        `artist_id` INTEGER, 
                        `composer_id` INTEGER, 
                        `copyright_owner_id` INTEGER, 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("""
                    INSERT INTO songs_new (id, slug, title, lyrics, category, created_at, views, artist_id, composer_id, copyright_owner_id)
                    SELECT id, slug, title, lyrics, category, created_at, views, artist_id, composer_id, copyright_owner_id FROM songs
                """)
                db.execSQL("DROP TABLE songs")
                db.execSQL("ALTER TABLE songs_new RENAME TO songs")
                
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_songs_slug` ON `songs` (`slug`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_artist_id` ON `songs` (`artist_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_composer_id` ON `songs` (`composer_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_copyright_owner_id` ON `songs` (`copyright_owner_id`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add copyright_owner_id to songs table
                db.execSQL("ALTER TABLE songs ADD COLUMN copyright_owner_id INTEGER")
                
                // Create copyright_owners table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `copyright_owners` (
                        `id` INTEGER NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `slug` TEXT NOT NULL, 
                        `full_legal_name` TEXT, 
                        `organization` TEXT, 
                        `territory` TEXT, 
                        `email` TEXT, 
                        `website` TEXT, 
                        `address` TEXT, 
                        `ipi_number` TEXT, 
                        `isrc_prefix` TEXT, 
                        `pro_affiliation` TEXT, 
                        `notes` TEXT, 
                        `created_at` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_copyright_owners_slug` ON `copyright_owners` (`slug`)")
                
                // Add index to songs table
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_copyright_owner_id` ON `songs` (`copyright_owner_id`)")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Update songs table
                db.execSQL("ALTER TABLE songs ADD COLUMN slug TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE songs ADD COLUMN views INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN artist_id INTEGER")
                db.execSQL("ALTER TABLE songs ADD COLUMN composer_id INTEGER")
                
                // Create artists table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `artists` (
                        `id` INTEGER NOT NULL, 
                        `slug` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `bio` TEXT, 
                        `image_url` TEXT, 
                        `social_links` TEXT, 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_artists_slug` ON `artists` (`slug`)")

                // Create composers table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `composers` (
                        `id` INTEGER NOT NULL, 
                        `slug` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `bio` TEXT, 
                        `image_url` TEXT, 
                        `social_links` TEXT, 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_composers_slug` ON `composers` (`slug`)")

                // Create feedback table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `feedback` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `song_id` INTEGER NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `email` TEXT NOT NULL, 
                        `message` TEXT NOT NULL, 
                        `is_synced` INTEGER NOT NULL, 
                        `created_at` INTEGER NOT NULL
                    )
                """)

                // Create sync_metadata table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_metadata` (
                        `key` TEXT NOT NULL, 
                        `version` INTEGER NOT NULL, 
                        `last_sync` INTEGER NOT NULL, 
                        PRIMARY KEY(`key`)
                    )
                """)
                
                // Add indices to songs table
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_songs_slug` ON `songs` (`slug`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_artist_id` ON `songs` (`artist_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_composer_id` ON `songs` (`composer_id`)")
            }
        }
    }
}
