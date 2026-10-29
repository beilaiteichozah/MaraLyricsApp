package com.maralyrics.laitei.data.sync

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.maralyrics.laitei.data.local.MaraLyricsDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The favorites queries the account sync relies on, run against real SQLite: slugs, not ids, in and out. */
class RoomFavoriteStoreTest {

    private lateinit var db: MaraLyricsDatabase
    private lateinit var store: RoomFavoriteStore

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, MaraLyricsDatabase::class.java).build()
        db.openHelper.writableDatabase.execSQL(
            """INSERT INTO songs (id, slug, title, lyrics, category, created_at, views) VALUES
               (1, 'alpha', 'Alpha', 'a', 'Gospel', 1, 0),
               (2, 'beta', 'Beta', 'b', 'Love', 2, 0),
               (3, 'gamma', 'Gamma', 'c', 'Gospel', 3, 0)"""
        )
        store = RoomFavoriteStore(db.favoriteDao(), db.songDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun theLibraryIsEverySongBySlug() = runBlocking {
        assertEquals(setOf("alpha", "beta", "gamma"), store.library())
    }

    @Test
    fun addingByNameFavoritesTheSongWithThatTime() = runBlocking {
        store.add("beta", at = 12_345)

        assertEquals(listOf(LocalFavorite("beta", 12_345)), store.favorites())
    }

    @Test
    fun removingByNameUnfavoritesIt() = runBlocking {
        store.add("alpha", 1)
        store.add("beta", 2)

        store.remove("alpha")

        assertEquals(listOf("beta"), store.favorites().map { it.slug })
    }

    @Test
    fun aSongThatIsNotDownloadedIsIgnoredNotACrash() = runBlocking {
        store.add("not-in-the-library", 5)
        store.remove("not-in-the-library")

        assertEquals(emptyList<LocalFavorite>(), store.favorites())
    }

    @Test
    fun favoritingTwiceStaysOneFavorite() = runBlocking {
        store.add("gamma", 1)
        store.add("gamma", 2)

        assertEquals(1, store.favorites().size)
    }

    @Test
    fun theDatabaseLooksUpSongsByIdAndSlug() = runBlocking {
        assertEquals(2L, db.songDao().getIdBySlug("beta"))
        assertEquals("gamma", db.songDao().getSlugById(3))
        assertEquals(null, db.songDao().getIdBySlug("nope"))
    }
}
