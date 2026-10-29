package com.maralyrics.laitei.data.sync


import com.maralyrics.laitei.data.remote.AccountApiService
import com.maralyrics.laitei.data.remote.dto.AuthResponse
import com.maralyrics.laitei.data.remote.dto.LoginRequest
import com.maralyrics.laitei.data.remote.dto.MeResponse
import com.maralyrics.laitei.data.remote.dto.SyncItemDto
import com.maralyrics.laitei.data.remote.dto.SyncRequest
import com.maralyrics.laitei.data.remote.dto.SyncResponse
import com.maralyrics.laitei.data.remote.dto.TokenResponse
import com.maralyrics.laitei.data.repository.SessionTokens
import com.maralyrics.laitei.domain.model.AppLanguage
import com.maralyrics.laitei.domain.model.AppTheme
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Runs the sync engine of two phones against a stand-in for the website's sync endpoint that keeps
 * its rules: the newest change wins, a removal is a tombstone, every change moves a revision, and
 * a request asks for "everything after" a revision, a page at a time.
 */
class AccountSyncEngineTest {

    // --- The website's side -------------------------------------------------------------------

    private class Row(var value: String?, var deleted: Boolean, var at: Long, var rev: Long)

    private class FakeServer(private val pageSize: Int = 1000) : AccountApiService {
        val rows = linkedMapOf<String, Row>()
        var rev = 0L
        var failWith: (() -> Nothing)? = null
        var unauthorized = false
        val requests = mutableListOf<SyncRequest>()

        override suspend fun sync(authorization: String, body: SyncRequest): Response<SyncResponse> {
            failWith?.invoke()
            if (unauthorized) return Response.error(401, "{\"error\":\"Unauthorized\"}".toResponseBody("application/json".toMediaType()))
            requests += body

            val lost = mutableSetOf<String>()
            // Within one request the newest change per item is kept.
            for (op in body.ops.groupBy { "${it.kind}:${it.key}" }.values.map { group -> group.maxByOrNull { it.at }!! }) {
                val id = "${op.kind}:${op.key}"
                val existing = rows[id]
                when {
                    existing != null && existing.at > op.at -> lost += id
                    existing != null && existing.deleted == op.deleted && existing.value == op.value?.content -> Unit
                    existing == null && op.deleted -> Unit
                    else -> rows[id] = Row(op.value?.content, op.deleted, op.at, ++rev)
                }
            }

            val newer = rows.entries.filter { it.value.rev > body.since }.sortedBy { it.value.rev }
            val page = newer.take(pageSize)
            val more = newer.size > pageSize
            val returned = (page.map { it.key to it.value } + lost.mapNotNull { id -> rows[id]?.let { id to it } })
                .distinctBy { it.first }
            return Response.success(
                SyncResponse(
                    rev = if (more) page.last().value.rev - 1 else rev,
                    more = JsonPrimitive(more),
                    items = returned.map { (id, row) ->
                        SyncItemDto(
                            kind = id.substringBefore(':'),
                            key = id.substringAfter(':'),
                            value = row.value?.let { JsonPrimitive(it) },
                            deleted = JsonPrimitive(row.deleted),
                            at = row.at
                        )
                    }
                )
            )
        }

        override suspend fun login(body: LoginRequest): Response<AuthResponse> = error("not expected")
        override suspend fun me(authorization: String): Response<MeResponse> = error("not expected")
        override suspend fun logoutAll(authorization: String): Response<TokenResponse> = error("not expected")
    }

    // --- A phone's side -----------------------------------------------------------------------

    private class FakeSession(var accountId: Long? = 7) : SessionTokens {
        var invalidated = false
        override fun authorization() = if (accountId != null && !invalidated) "Bearer x.y.z" else null
        override fun accountId() = if (invalidated) null else accountId
        override suspend fun invalidate() { invalidated = true }
    }

    private class MemoryState : SyncStateStore {
        var state: SyncState? = null
        override suspend fun read() = state
        override suspend fun write(state: SyncState) { this.state = state }
        override suspend fun clear() { state = null }
    }

    private class Phone(val server: FakeServer, val clock: Clock, library: Set<String>, val accountId: Long = 7) {
        val local = LocalFavorites(library.toMutableSet())
        val prefs = LocalPrefs()
        val stateStore = MemoryState()
        val session = FakeSession(accountId)
        val engine = AccountSyncEngine(server, session, stateStore, local, prefs, clock::now)

        fun favoriteSlugs() = local.favorites.keys.toSet()
        suspend fun favorite(slug: String) {
            local.favorites[slug] = clock.now()
            engine.recordFavorite(slug, true)
        }
        suspend fun unfavorite(slug: String) {
            local.favorites.remove(slug)
            engine.recordFavorite(slug, false)
        }
    }

    private class Clock(var t: Long = 1_000) {
        fun now() = t
        fun tick(by: Long = 10) = apply { t += by }
    }

    private class LocalFavorites(val library: MutableSet<String>) : FavoriteStore {
        val favorites = linkedMapOf<String, Long>()
        override suspend fun favorites() = favorites.map { LocalFavorite(it.key, it.value) }
        override suspend fun library(): Set<String> = library
        override suspend fun add(slug: String, at: Long) { if (slug in library) favorites[slug] = at }
        override suspend fun remove(slug: String) { favorites.remove(slug) }
    }

    private class LocalPrefs : PreferenceStore {
        var theme = AppTheme.SYSTEM
        var language = AppLanguage.ENGLISH
        override suspend fun theme() = theme
        override suspend fun language() = language
        override suspend fun setTheme(theme: AppTheme) { this.theme = theme }
        override suspend fun setLanguage(language: AppLanguage) { this.language = language }
    }

    private val songs = setOf("alpha", "beta", "gamma", "delta")
    private val clock = Clock()
    private val server = FakeServer()
    private fun phone(library: Set<String> = songs, accountId: Long = 7) = Phone(server, clock, library, accountId)

    private fun success(outcome: SyncOutcome) = outcome as SyncOutcome.Success

    // --- Favorites ----------------------------------------------------------------------------

    @Test
    fun `signing in uploads the favorites this phone already has`() = runBlocking {
        val a = phone()
        a.local.favorites["alpha"] = 500
        a.local.favorites["beta"] = 600

        success(a.engine.sync())

        assertEquals(setOf("favorite:alpha", "favorite:beta"), server.rows.keys)
        assertEquals(500L, server.rows["favorite:alpha"]!!.at)
    }

    @Test
    fun `a second phone receives the favorites`() = runBlocking {
        val a = phone()
        a.local.favorites["alpha"] = 500
        a.local.favorites["beta"] = 600
        a.engine.sync()

        val b = phone()
        val outcome = success(b.engine.sync())

        assertEquals(setOf("alpha", "beta"), b.favoriteSlugs())
        assertEquals(2, outcome.favoritesAdded)
    }

    @Test
    fun `removing a favorite on one phone removes it on the other`() = runBlocking {
        val a = phone(); val b = phone()
        a.favorite("alpha"); a.favorite("beta"); a.engine.sync()
        b.engine.sync()
        assertEquals(setOf("alpha", "beta"), b.favoriteSlugs())

        clock.tick()
        a.unfavorite("alpha")
        a.engine.sync()
        val outcome = success(b.engine.sync())

        assertEquals(setOf("beta"), b.favoriteSlugs())
        assertEquals(1, outcome.favoritesRemoved)
        assertTrue("kept as a tombstone", server.rows["favorite:alpha"]!!.deleted)
    }

    @Test
    fun `favoriting again after removing it brings it back everywhere`() = runBlocking {
        val a = phone(); val b = phone()
        a.favorite("alpha"); a.engine.sync(); b.engine.sync()
        clock.tick(); a.unfavorite("alpha"); a.engine.sync(); b.engine.sync()
        assertEquals(emptySet<String>(), b.favoriteSlugs())

        clock.tick(); a.favorite("alpha"); a.engine.sync()
        b.engine.sync()

        assertEquals(setOf("alpha"), b.favoriteSlugs())
    }

    @Test
    fun `a favorite for a song that is not downloaded yet waits for it`() = runBlocking {
        val a = phone()
        a.favorite("alpha"); a.favorite("delta"); a.engine.sync()

        val b = phone(library = setOf("alpha", "beta"))
        b.engine.sync()
        assertEquals(setOf("alpha"), b.favoriteSlugs())

        b.local.library += "delta" // the song arrives with a library update
        b.engine.sync()

        assertEquals(setOf("alpha", "delta"), b.favoriteSlugs())
    }

    @Test
    fun `a newer favorite beats an older removal made on another phone`() = runBlocking {
        val a = phone(); val b = phone()
        clock.t = 1_000; a.favorite("alpha"); a.engine.sync(); b.engine.sync()

        // A removes it and syncs. B, which has been offline, removes it and favorites it again later.
        clock.t = 1_010; a.unfavorite("alpha"); a.engine.sync()
        clock.t = 1_015; b.unfavorite("alpha")
        clock.t = 1_020; b.favorite("alpha")
        b.engine.sync() // 1020 is newer than the removal at 1010

        a.engine.sync()

        assertEquals(setOf("alpha"), b.favoriteSlugs())
        assertEquals(setOf("alpha"), a.favoriteSlugs())
        assertFalse(server.rows["favorite:alpha"]!!.deleted)
    }

    @Test
    fun `an older favorite loses to a newer removal made on another phone`() = runBlocking {
        val a = phone(); val b = phone()
        clock.t = 1_000; a.favorite("alpha"); a.engine.sync(); b.engine.sync()

        // B favorites it again at 1006, offline; A removes it later, at 1010, and syncs first.
        clock.t = 1_005; b.unfavorite("alpha")
        clock.t = 1_006; b.favorite("alpha")
        clock.t = 1_010; a.unfavorite("alpha"); a.engine.sync()

        b.engine.sync() // B's change at 1006 loses to the removal at 1010

        assertEquals(emptySet<String>(), b.favoriteSlugs())
        assertEquals(emptySet<String>(), a.favoriteSlugs())
        assertTrue(server.rows["favorite:alpha"]!!.deleted)
    }

    @Test
    fun `a change made while offline is kept and sent later`() = runBlocking {
        val a = phone()
        a.favorite("alpha"); a.engine.sync()

        clock.tick(); a.favorite("beta")
        server.failWith = { throw IOException("offline") }
        assertEquals(SyncOutcome.Offline, a.engine.sync())
        assertFalse("not on the server yet", "favorite:beta" in server.rows)

        server.failWith = null
        success(a.engine.sync())

        assertTrue("favorite:beta" in server.rows)
    }

    @Test
    fun `changes recorded before the first sync are still sent by it`() = runBlocking {
        val a = phone()
        a.favorite("alpha") // signed in, nothing synced yet

        success(a.engine.sync())

        assertTrue("favorite:alpha" in server.rows)
    }

    @Test
    fun `a server error leaves everything as it was`() = runBlocking {
        val a = phone()
        a.favorite("alpha")
        server.failWith = { error("boom") }

        assertEquals(SyncOutcome.Failed, a.engine.sync())

        server.failWith = null
        success(a.engine.sync())
        assertTrue("favorite:alpha" in server.rows)
    }

    @Test
    fun `a session the server turns down signs this phone out`() = runBlocking {
        val a = phone()
        a.favorite("alpha")
        server.unauthorized = true

        assertEquals(SyncOutcome.SignedOut, a.engine.sync())
        assertTrue(a.session.invalidated)
        assertEquals("the favorites on the phone are untouched", setOf("alpha"), a.favoriteSlugs())
    }

    @Test
    fun `nobody signed in means nothing to do`() = runBlocking {
        val a = phone()
        a.session.accountId = null

        assertEquals(SyncOutcome.SignedOut, a.engine.sync())
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun `a different account does not inherit the previous one's sync state`() = runBlocking {
        val a = phone(accountId = 7)
        a.favorite("alpha"); a.engine.sync()
        val staleState = a.stateStore.state!!
        assertEquals(7L, staleState.accountId)

        // The same phone, now signed in as account 8.
        val second = FakeServer()
        val asEight = Phone(second, clock, songs, accountId = 8)
        asEight.stateStore.state = staleState
        asEight.local.favorites["beta"] = 700
        asEight.engine.sync()

        assertEquals(setOf("favorite:beta"), second.rows.keys)
        assertEquals(8L, asEight.stateStore.state!!.accountId)
    }

    @Test
    fun `resetting forgets the sync state but not the favorites`() = runBlocking {
        val a = phone()
        a.favorite("alpha"); a.engine.sync()

        a.engine.reset()

        assertNull(a.stateStore.state)
        assertEquals(setOf("alpha"), a.favoriteSlugs())
    }

    // --- Size ---------------------------------------------------------------------------------

    @Test
    fun `a long queue goes out in batches the server accepts`() = runBlocking {
        val many = (1..450).map { "song-$it" }.toSet()
        val a = phone(library = many)
        many.forEach { a.local.favorites[it] = 100L }

        success(a.engine.sync())

        assertEquals(450, server.rows.size)
        assertTrue(server.requests.all { it.ops.size <= SyncLogic.MAX_OPS })
        assertEquals(listOf(200, 200, 50), server.requests.map { it.ops.size })
    }

    @Test
    fun `a big answer is read page by page until it is all in`() = runBlocking {
        val paged = FakeServer(pageSize = 2)
        val a = Phone(paged, clock, songs)
        songs.forEach { a.local.favorites[it] = 100L }
        a.engine.sync()

        val b = Phone(paged, clock, songs)
        b.engine.sync()

        assertEquals(songs, b.favoriteSlugs())
    }

    // --- Theme and language -------------------------------------------------------------------

    @Test
    fun `a theme chosen on one phone follows to the other`() = runBlocking {
        val a = phone(); val b = phone()
        a.engine.sync(); b.engine.sync()

        clock.tick()
        a.prefs.theme = AppTheme.DARK
        a.engine.recordPreference(SyncKinds.PREF_THEME, "dark")
        a.engine.sync()
        b.engine.sync()

        assertEquals(AppTheme.DARK, b.prefs.theme)
    }

    @Test
    fun `language follows too`() = runBlocking {
        val a = phone(); val b = phone()
        a.engine.sync(); b.engine.sync()

        clock.tick()
        a.engine.recordPreference(SyncKinds.PREF_LANGUAGE, "mrh")
        a.engine.sync()
        b.engine.sync()

        assertEquals(AppLanguage.MARA, b.prefs.language)
    }

    @Test
    fun `an older setting from the server never undoes a newer choice made here`() = runBlocking {
        val a = phone(); val b = phone()
        a.engine.sync(); b.engine.sync()
        clock.tick(); a.engine.recordPreference(SyncKinds.PREF_THEME, "dark"); a.engine.sync()

        // B chooses light after A's change, then syncs.
        clock.tick()
        b.prefs.theme = AppTheme.LIGHT
        b.engine.recordPreference(SyncKinds.PREF_THEME, "light")
        b.engine.sync()
        a.engine.sync()

        assertEquals(AppTheme.LIGHT, b.prefs.theme)
        assertEquals(AppTheme.LIGHT, a.prefs.theme)
    }

    @Test
    fun `a theme the app does not have is left alone`() = runBlocking {
        server.rows["pref:theme"] = Row("time", false, 5_000, ++server.rev)
        val a = phone()
        a.prefs.theme = AppTheme.DARK

        a.engine.sync()

        assertEquals("'time' is website-only; it maps to the system theme", AppTheme.SYSTEM, a.prefs.theme)

        server.rows["pref:theme"] = Row("sepia", false, 9_000, ++server.rev)
        a.prefs.theme = AppTheme.DARK
        a.engine.sync()
        assertEquals("an unknown value changes nothing", AppTheme.DARK, a.prefs.theme)
    }

    @Test
    fun `things the app does not use, like pins and downloads, are ignored and never overwritten`() = runBlocking {
        server.rows["pin:alpha"] = Row("0", false, 100, ++server.rev)
        server.rows["download:alpha"] = Row(null, false, 100, ++server.rev)
        val a = phone()
        a.favorite("beta")

        a.engine.sync()

        assertEquals(setOf("beta"), a.favoriteSlugs())
        assertEquals("0", server.rows["pin:alpha"]!!.value)
        assertTrue(a.stateStore.state!!.items.keys.none { it.startsWith("pin:") || it.startsWith("download:") })
    }
}
