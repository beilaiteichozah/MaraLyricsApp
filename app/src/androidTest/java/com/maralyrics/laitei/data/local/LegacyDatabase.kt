package com.maralyrics.laitei.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

/**
 * Builds databases as earlier app versions left them, for the migration tests.
 *
 * Only version 1 is written out by hand (it is the original `Song`/`Favorite`/`RecentView` schema).
 * Later versions are made by running the shipped migrations forward from it — which is how a real
 * device got there — because the exported schema files for some versions (2, 4, 7, 8) were written
 * at the wrong moment and don't describe what those versions really were, and 3 was never kept.
 */
internal object LegacyDatabase {

    /** Songs, favorites and recently viewed songs as the reader might have them. */
    fun create(context: Context, name: String, version: Int) {
        context.deleteDatabase(name)
        val db = open(context, name)
        db.execSQL(
            """INSERT INTO songs (id, title, lyrics, category, song_number, created_at, updated_at, view_count)
               VALUES (1, 'Alpha song', 'alpha lyrics', 'Gospel', 1, 1000, 1000, 0),
                      (2, 'Beta song', 'beta lyrics', 'Love', 2, 2000, 2000, 3),
                      (3, 'Gamma song', 'gamma lyrics', 'Gospel', 3, 3000, 3000, 0)"""
        )
        db.execSQL("INSERT INTO favorites (song_id, added_at) VALUES (1, 111), (3, 333)")
        db.execSQL("INSERT INTO recent_views (song_id, viewed_at) VALUES (2, 222)")

        for (step in MaraLyricsDatabase.ALL_MIGRATIONS) {
            if (step.endVersion > version) break
            step.migrate(db)
            if (step.endVersion == 2) {
                // The feedback table first exists in v2, in its original shape.
                db.execSQL(
                    """INSERT INTO feedback (song_id, name, email, message, is_synced, created_at)
                       VALUES (2, 'Tester', 't@example.com', 'typo in verse 2', 0, 444)"""
                )
            }
        }
        db.version = version
        db.close()
    }

    /** A version 1 database, from the schema the app first shipped with. */
    private fun open(context: Context, name: String): SupportSQLiteDatabase {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `songs` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                        "`lyrics` TEXT NOT NULL, `category` TEXT NOT NULL, `song_number` INTEGER NOT NULL, " +
                        "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                        "`view_count` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`song_id` INTEGER NOT NULL, `added_at` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`song_id`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_song_id` ON `favorites` (`song_id`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recent_views` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`song_id` INTEGER NOT NULL, `viewed_at` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`song_id`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recent_views_song_id` ON `recent_views` (`song_id`)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        return FrameworkSQLiteOpenHelperFactory()
            .create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(callback).build())
            .writableDatabase
    }
}
