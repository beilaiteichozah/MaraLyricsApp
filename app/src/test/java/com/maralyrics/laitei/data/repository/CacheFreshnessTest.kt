package com.maralyrics.laitei.data.repository

import android.content.Context
import com.maralyrics.laitei.data.local.ContentCache
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File

class CacheFreshnessTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val hour = 60L * 60 * 1000

    @Test
    fun `a copy younger than the limit is fresh, an older or missing one is not`() {
        assertTrue(isFresh(0, 12 * hour))
        assertTrue(isFresh(12 * hour - 1, 12 * hour))
        assertFalse(isFresh(12 * hour, 12 * hour))
        assertFalse(isFresh(13 * hour, 12 * hour))
        assertFalse("never saved", isFresh(null, 12 * hour))
        assertFalse("a clock set back must not make a copy look brand new forever", isFresh(-1, 12 * hour))
    }

    @Test
    fun `the cache reports how old a saved entry is`() = runBlocking {
        val context = mock<Context> { on { cacheDir } doReturn tempFolder.root }
        val cache = ContentCache(context, Json { ignoreUnknownKeys = true })

        assertNull(cache.ageMillis("recognition_artist"))

        cache.write("recognition_artist", "{}")
        val justWritten = cache.ageMillis("recognition_artist")
        assertNotNull(justWritten)
        assertTrue("was $justWritten", justWritten!! in 0..10_000)

        File(tempFolder.root, "content/recognition_artist.json").setLastModified(System.currentTimeMillis() - 5 * hour)
        val aged = cache.ageMillis("recognition_artist")!!
        assertTrue("was $aged", aged >= 5 * hour - 1000)
        assertEquals(true, isFresh(aged, 12 * hour))
        assertEquals(false, isFresh(aged, 4 * hour))
    }
}
