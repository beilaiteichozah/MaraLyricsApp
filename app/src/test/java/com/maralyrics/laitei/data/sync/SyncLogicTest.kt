package com.maralyrics.laitei.data.sync

import com.maralyrics.laitei.data.remote.dto.SyncItemDto
import com.maralyrics.laitei.data.remote.dto.SyncResponse
import com.maralyrics.laitei.domain.model.AppLanguage
import com.maralyrics.laitei.domain.model.AppTheme
import com.maralyrics.laitei.domain.model.PrefMapping
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncLogicTest {

    private val empty = SyncState(accountId = 1)

    private fun fav(slug: String, at: Long, deleted: Boolean = false) =
        PendingOp(SyncKinds.FAVORITE, slug, at, deleted)

    // --- The queue ----------------------------------------------------------------------------

    @Test
    fun `a newer change to the same song replaces the waiting one`() {
        val state = SyncLogic.enqueue(SyncLogic.enqueue(empty, fav("a", 10)), fav("a", 20, deleted = true))

        assertEquals(listOf(fav("a", 20, deleted = true)), state.queue.values.toList())
    }

    @Test
    fun `an older change never replaces a newer waiting one`() {
        val state = SyncLogic.enqueue(SyncLogic.enqueue(empty, fav("a", 20)), fav("a", 10, deleted = true))

        assertEquals(20L, state.queue.getValue("favorite:a").at)
        assertEquals(false, state.queue.getValue("favorite:a").deleted)
    }

    @Test
    fun `changing a setting remembers when, so an older server value can't undo it`() {
        val state = SyncLogic.enqueue(empty, PendingOp(SyncKinds.PREF, "theme", 50, value = "dark"))

        assertEquals(50L, state.prefChangedAt["theme"])
    }

    @Test
    fun `seeding queues every favorite already on the device, once`() {
        val seeded = SyncLogic.seed(empty, listOf(LocalFavorite("a", 5), LocalFavorite("b", 0)))

        assertTrue(seeded.seeded)
        assertEquals(setOf("favorite:a", "favorite:b"), seeded.queue.keys)
        assertEquals("time zero is bumped so the server accepts it", 1L, seeded.queue.getValue("favorite:b").at)
    }

    // --- The server's answer ------------------------------------------------------------------

    private fun response(rev: Long, vararg items: SyncItemDto) = SyncResponse(rev = rev, items = items.toList())

    private fun item(kind: String, key: String, at: Long, deleted: Boolean = false, value: String? = null) =
        SyncItemDto(kind, key, value?.let { JsonPrimitive(it) }, JsonPrimitive(deleted), at)

    @Test
    fun `what was sent leaves the queue and the server's items are kept`() {
        val queued = SyncLogic.enqueue(empty, fav("a", 10))
        val sent = queued.queue.values.toList()

        val next = SyncLogic.applyResponse(queued, sent, response(3, item("favorite", "a", 10)))

        assertTrue(next.queue.isEmpty())
        assertEquals(3L, next.rev)
        assertEquals(SyncedItem(at = 10, deleted = false), next.items["favorite:a"])
    }

    @Test
    fun `a change made while the request was in flight stays queued`() {
        val queued = SyncLogic.enqueue(empty, fav("a", 10))
        val sent = queued.queue.values.toList()
        val changedAgain = SyncLogic.enqueue(queued, fav("a", 30, deleted = true))

        val next = SyncLogic.applyResponse(changedAgain, sent, response(1, item("favorite", "a", 10)))

        assertEquals(30L, next.queue.getValue("favorite:a").at)
    }

    @Test
    fun `the revision never moves backwards`() {
        val next = SyncLogic.applyResponse(empty.copy(rev = 9), emptyList(), response(4))

        assertEquals(9L, next.rev)
    }

    @Test
    fun `only favorites and the two settings are kept from the server`() {
        val next = SyncLogic.applyResponse(
            empty, emptyList(),
            response(
                1,
                item("favorite", "a", 1),
                item("pin", "a", 1, value = "0"),
                item("download", "a", 1),
                item("pref", "theme", 1, value = "dark"),
                item("favorite", "", 1)
            )
        )

        assertEquals(setOf("favorite:a", "pref:theme"), next.items.keys)
    }

    @Test
    fun `an older row never replaces a newer one already known`() {
        val known = empty.copy(items = mapOf("favorite:a" to SyncedItem(at = 50)))

        val next = SyncLogic.applyResponse(known, emptyList(), response(2, item("favorite", "a", 20, deleted = true)))

        assertEquals(SyncedItem(at = 50), next.items["favorite:a"])
    }

    @Test
    fun `a removal is read from 1 or true as well as from a boolean`() {
        val next = SyncLogic.applyResponse(
            empty, emptyList(),
            response(
                1,
                SyncItemDto("favorite", "a", null, JsonPrimitive(1), 5),
                SyncItemDto("favorite", "b", null, JsonPrimitive("true"), 5),
                SyncItemDto("favorite", "c", null, JsonPrimitive(0), 5)
            )
        )

        assertEquals(listOf(true, true, false), listOf("a", "b", "c").map { next.items.getValue("favorite:$it").deleted })
    }

    // --- Deciding what to change here ---------------------------------------------------------

    private fun stateWith(vararg items: Pair<String, SyncedItem>, queue: List<PendingOp> = emptyList()) =
        empty.copy(items = items.toMap(), queue = queue.associateBy { it.id })

    @Test
    fun `favorites on the server that are missing here are added, if the song is downloaded`() {
        val state = stateWith("favorite:a" to SyncedItem(1), "favorite:b" to SyncedItem(1))

        val changes = SyncLogic.planFavorites(state, local = emptySet(), library = setOf("a"))

        assertEquals(listOf("a"), changes.add)
        assertTrue(changes.remove.isEmpty())
    }

    @Test
    fun `removals on the server are applied to favorites held here`() {
        val state = stateWith("favorite:a" to SyncedItem(1, deleted = true))

        val changes = SyncLogic.planFavorites(state, local = setOf("a", "b"), library = setOf("a", "b"))

        assertEquals(listOf("a"), changes.remove)
    }

    @Test
    fun `a favorite here that the server has never heard of is left alone`() {
        val changes = SyncLogic.planFavorites(stateWith(), local = setOf("a"), library = setOf("a"))

        assertTrue(changes.add.isEmpty() && changes.remove.isEmpty())
    }

    @Test
    fun `a change still waiting to be sent beats the server's older word`() {
        val state = stateWith(
            "favorite:a" to SyncedItem(1, deleted = true),
            "favorite:b" to SyncedItem(1),
            queue = listOf(fav("a", 9), fav("b", 9, deleted = true))
        )

        val changes = SyncLogic.planFavorites(state, local = setOf("a"), library = setOf("a", "b"))

        assertTrue("a was re-added here and is about to be sent", changes.remove.isEmpty())
        assertTrue("b was removed here and is about to be sent", changes.add.isEmpty())
    }

    // --- Settings -----------------------------------------------------------------------------

    @Test
    fun `a server setting applies only if newer than the last change here`() {
        val state = empty.copy(
            items = mapOf("pref:theme" to SyncedItem(at = 100, value = "dark"), "pref:lang" to SyncedItem(at = 100, value = "my")),
            prefChangedAt = mapOf("theme" to 100L, "lang" to 40L)
        )

        assertNull("same moment: it was ours", SyncLogic.shouldApplyPreference(state, "theme"))
        assertEquals("my", SyncLogic.shouldApplyPreference(state, "lang")?.value)
        assertNull(SyncLogic.shouldApplyPreference(state, "calendar"))
    }

    @Test
    fun `a removed or empty setting is never applied`() {
        val state = empty.copy(
            items = mapOf("pref:theme" to SyncedItem(at = 100, deleted = true, value = "dark"), "pref:lang" to SyncedItem(at = 100))
        )

        assertNull(SyncLogic.shouldApplyPreference(state, "theme"))
        assertNull(SyncLogic.shouldApplyPreference(state, "lang"))
    }

    @Test
    fun `the website's words for theme and language map to the app's`() {
        assertEquals(AppTheme.DARK, PrefMapping.themeFromWire("dark"))
        assertEquals(AppTheme.LIGHT, PrefMapping.themeFromWire(" Light "))
        assertEquals(AppTheme.SYSTEM, PrefMapping.themeFromWire("system"))
        assertEquals(AppTheme.SYSTEM, PrefMapping.themeFromWire("time"))
        assertNull(PrefMapping.themeFromWire("sepia"))
        assertNull(PrefMapping.themeFromWire(null))

        assertEquals(AppLanguage.MARA, PrefMapping.languageFromWire("mrh"))
        assertEquals(AppLanguage.BURMESE, PrefMapping.languageFromWire("MY"))
        assertNull(PrefMapping.languageFromWire("fr"))

        assertEquals("dark", PrefMapping.themeToWire(AppTheme.DARK))
        assertEquals("mrh", PrefMapping.languageToWire(AppLanguage.MARA))
    }
}
