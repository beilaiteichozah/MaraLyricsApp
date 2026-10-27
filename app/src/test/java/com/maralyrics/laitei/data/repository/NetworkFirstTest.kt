package com.maralyrics.laitei.data.repository

import android.content.Context
import com.maralyrics.laitei.data.local.ContentCache
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import retrofit2.Response
import java.io.IOException

class NetworkFirstTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Serializable
    private data class Payload(val value: String)

    private lateinit var cache: ContentCache

    @Before
    fun setUp() {
        val context = mock<Context> { on { cacheDir } doReturn tempFolder.root }
        cache = ContentCache(context, Json { ignoreUnknownKeys = true })
    }

    @Test
    fun `fresh response is returned and saved for next time`() = runBlocking {
        val result = cache.networkFirst("key") { Response.success(Payload("fresh")) }

        assertEquals(Payload("fresh"), result.getOrThrow().data)
        assertFalse(result.getOrThrow().isFromCache)
        assertEquals(Payload("fresh"), cache.readJson<Payload>("key"))
    }

    @Test
    fun `newer response replaces the saved copy`() = runBlocking {
        cache.networkFirst("key") { Response.success(Payload("old")) }
        cache.networkFirst("key") { Response.success(Payload("new")) }

        assertEquals(Payload("new"), cache.readJson<Payload>("key"))
    }

    @Test
    fun `io failure falls back to the saved copy and says so`() = runBlocking {
        cache.networkFirst("key") { Response.success(Payload("saved")) }

        val result = cache.networkFirst<Payload>("key") { throw IOException("offline") }

        assertEquals(Payload("saved"), result.getOrThrow().data)
        assertTrue(result.getOrThrow().isFromCache)
    }

    @Test
    fun `http error falls back to the saved copy`() = runBlocking {
        cache.networkFirst("key") { Response.success(Payload("saved")) }

        val result = cache.networkFirst<Payload>("key") {
            Response.error(500, "boom".toResponseBody())
        }

        assertEquals(Payload("saved"), result.getOrThrow().data)
        assertTrue(result.getOrThrow().isFromCache)
    }

    @Test
    fun `malformed response body falls back to the saved copy`() = runBlocking {
        cache.networkFirst("key") { Response.success(Payload("saved")) }

        val result = cache.networkFirst<Payload>("key") {
            throw kotlinx.serialization.SerializationException("bad json")
        }

        assertEquals(Payload("saved"), result.getOrThrow().data)
    }

    @Test
    fun `offline with nothing saved is a failure`() = runBlocking {
        val result = cache.networkFirst<Payload>("never-saved") { throw IOException("offline") }

        assertTrue(result.isFailure)
    }

    @Test
    fun `a corrupt saved copy counts as nothing saved`() = runBlocking {
        cache.write("key", "{ this is not json")

        val result = cache.networkFirst<Payload>("key") { throw IOException("offline") }

        assertTrue(result.isFailure)
        assertNull(cache.readJson<Payload>("key"))
    }

    @Test
    fun `cache keys with unsafe characters do not collide or escape the cache folder`() = runBlocking {
        cache.writeJson("a/b", Payload("slash"))
        cache.writeJson("../../evil", Payload("dots"))

        assertEquals(Payload("slash"), cache.readJson<Payload>("a/b"))
        assertEquals(Payload("dots"), cache.readJson<Payload>("../../evil"))
        // Everything stayed inside the cache directory.
        assertTrue(tempFolder.root.walkTopDown().filter { it.isFile }.all { it.absolutePath.startsWith(tempFolder.root.absolutePath) })
    }

    @Test
    fun `cancellation is not swallowed as a network failure`(): Unit = runBlocking {
        var cancelled = false
        try {
            cache.networkFirst<Payload>("key") { throw kotlinx.coroutines.CancellationException("cancelled") }
        } catch (e: kotlinx.coroutines.CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }
}
