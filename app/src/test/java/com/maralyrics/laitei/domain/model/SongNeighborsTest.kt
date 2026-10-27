package com.maralyrics.laitei.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongNeighborsTest {

    // Deliberately not sorted by id: the order is whatever the list on screen was showing.
    private val order = listOf(30L, 10L, 20L, 40L)

    @Test
    fun `middle song has both neighbours in list order, not id order`() {
        val neighbors = SongNeighbors.of(order, 10L)

        assertEquals(30L, neighbors?.previousId)
        assertEquals(20L, neighbors?.nextId)
        assertEquals(2, neighbors?.position)
        assertEquals(4, neighbors?.total)
    }

    @Test
    fun `first song has no previous`() {
        val neighbors = SongNeighbors.of(order, 30L)

        assertNull(neighbors?.previousId)
        assertEquals(10L, neighbors?.nextId)
        assertEquals(1, neighbors?.position)
    }

    @Test
    fun `last song has no next`() {
        val neighbors = SongNeighbors.of(order, 40L)

        assertEquals(20L, neighbors?.previousId)
        assertNull(neighbors?.nextId)
        assertEquals(4, neighbors?.position)
    }

    @Test
    fun `only song in the list has neither`() {
        val neighbors = SongNeighbors.of(listOf(7L), 7L)

        assertNull(neighbors?.previousId)
        assertNull(neighbors?.nextId)
        assertEquals(1, neighbors?.total)
    }

    @Test
    fun `song missing from the list gives nothing to page through`() {
        assertNull(SongNeighbors.of(order, 99L))
        assertNull(SongNeighbors.of(emptyList(), 1L))
    }
}
