package com.maralyrics.laitei.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Upgrades a database from every earlier version to the current one, through the same builder the
 * app uses, and checks that what the reader owns — favorites, recently viewed songs and queued
 * feedback — comes through intact. Opening runs Room's own check that the migrated tables, columns,
 * indices and foreign keys match the entities, so a migration that leaves anything different fails
 * here instead of crashing a reader's app.
 */
@RunWith(Parameterized::class)
class MaraLyricsMigrationTest(private val startVersion: Int) {

    companion object {
        private val CURRENT = MaraLyricsDatabase.ALL_MIGRATIONS.last().endVersion

        @JvmStatic
        @Parameterized.Parameters(name = "from v{0}")
        fun startVersions() = (1 until CURRENT).toList()
    }

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "migration-test-from-v$startVersion"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun upgradeKeepsEverythingTheReaderOwns() {
        LegacyDatabase.create(context, dbName, startVersion)

        val db = MaraLyricsDatabase.builder(context, dbName).build()
        try {
            val sql = db.openHelper.writableDatabase // runs the migrations and validates the schema

            assertEquals(CURRENT, sql.version)
            assertEquals(
                listOf("1|Alpha song|alpha lyrics", "2|Beta song|beta lyrics", "3|Gamma song|gamma lyrics"),
                rows(sql, "SELECT id, title, lyrics FROM songs ORDER BY id")
            )
            assertEquals(
                "favorites, with the time they were added",
                listOf("1|111", "3|333"),
                rows(sql, "SELECT song_id, favorited_at FROM favorites ORDER BY song_id")
            )
            assertEquals(
                listOf("2|222"),
                rows(sql, "SELECT song_id, viewed_at FROM recent_views")
            )
            assertEquals(
                "feedback that hadn't been sent yet",
                if (startVersion >= 2) listOf("Tester|typo in verse 2|0|") else emptyList(),
                rows(sql, "SELECT name, message, is_synced, song_slug FROM feedback")
            )
            assertEquals(
                "every song needs its own slug (the column is unique)",
                listOf("3"),
                rows(sql, "SELECT COUNT(DISTINCT slug) FROM songs")
            )
            assertEquals(
                "search still finds the song",
                listOf("2"),
                rows(sql, "SELECT rowid FROM songs_search_index WHERE songs_search_index MATCH 'beta'")
            )
            assertEquals(
                "no half-built tables left behind by a table rebuild",
                emptyList<String>(),
                rows(sql, "SELECT name FROM sqlite_master WHERE type = 'table' AND name LIKE '%\\_new' ESCAPE '\\'")
            )
            assertEquals(emptyList<String>(), rows(sql, "PRAGMA foreign_key_check"))
            assertEquals(listOf("ok"), rows(sql, "PRAGMA integrity_check"))
        } finally {
            db.close()
        }
    }

    private fun rows(db: SupportSQLiteDatabase, sql: String): List<String> =
        db.query(sql).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add((0 until cursor.columnCount).joinToString("|") { cursor.getString(it) ?: "" })
                }
            }
        }
}
