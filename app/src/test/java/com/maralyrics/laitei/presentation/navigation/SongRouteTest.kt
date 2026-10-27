package com.maralyrics.laitei.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongRouteTest {

    @Test
    fun `route without a list is just the song`() {
        assertEquals("song_detail/5", SongRoute.create(5L))
        assertEquals("song_detail/5", SongRoute.create(5L, emptyList()))
    }

    @Test
    fun `route carries the list order after the song`() {
        assertEquals("song_detail/5?order=3,5,9", SongRoute.create(5L, listOf(3L, 5L, 9L)))
    }

    @Test
    fun `order survives a round trip through the route`() {
        val order = listOf(121L, 4L, 88L, 7L)
        val route = SongRoute.create(88L, order)

        assertEquals(order, SongRoute.parseOrder(route.substringAfter("order=")))
    }

    @Test
    fun `a very long list is dropped so the route stays short`() {
        val huge = (1L..5000L).toList()
        val route = SongRoute.create(1L, huge)

        assertEquals("song_detail/1", route)
    }

    @Test
    fun `a list at the cap is still carried`() {
        val atCap = (1L..1500L).toList()
        assertTrue(SongRoute.create(1L, atCap).contains("?order="))
    }

    @Test
    fun `parsing tolerates missing, blank, padded and repeated entries`() {
        assertEquals(emptyList<Long>(), SongRoute.parseOrder(null))
        assertEquals(emptyList<Long>(), SongRoute.parseOrder(""))
        assertEquals(listOf(1L, 2L), SongRoute.parseOrder("1, x, 2,2,,"))
    }

    @Test
    fun `pattern matches the arguments the nav graph declares`() {
        assertEquals("song_detail/{songId}?order={order}", SongRoute.PATTERN)
        assertEquals("songId", SongRoute.ARG_SONG_ID)
        assertEquals("order", SongRoute.ARG_ORDER)
    }
}
