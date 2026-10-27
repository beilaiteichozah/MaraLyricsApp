package com.maralyrics.laitei.presentation.song_detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsZoomTest {

    private val delta = 0.0001f

    @Test
    fun `starts at the default size`() {
        val zoom = LyricsZoom()

        assertEquals(LyricsZoom.DEFAULT, zoom.factor.value, delta)
        assertTrue(LyricsZoom.isDefault(zoom.factor.value))
    }

    @Test
    fun `each press is a fifth bigger or smaller`() {
        val zoom = LyricsZoom()

        zoom.zoomIn()
        assertEquals(1.2f, zoom.factor.value, delta)
        zoom.zoomIn()
        assertEquals(1.44f, zoom.factor.value, delta)
        zoom.zoomOut()
        assertEquals(1.2f, zoom.factor.value, delta)
    }

    @Test
    fun `zooming in stops at the maximum`() {
        val zoom = LyricsZoom()

        repeat(20) { zoom.zoomIn() }

        assertEquals(LyricsZoom.MAX, zoom.factor.value, delta)
        assertFalse(LyricsZoom.canZoomIn(zoom.factor.value))
        assertTrue(LyricsZoom.canZoomOut(zoom.factor.value))
    }

    @Test
    fun `zooming out stops at the minimum`() {
        val zoom = LyricsZoom()

        repeat(20) { zoom.zoomOut() }

        assertEquals(LyricsZoom.MIN, zoom.factor.value, delta)
        assertFalse(LyricsZoom.canZoomOut(zoom.factor.value))
        assertTrue(LyricsZoom.canZoomIn(zoom.factor.value))
    }

    @Test
    fun `reset returns to the default from anywhere`() {
        val zoom = LyricsZoom()
        zoom.zoomIn()
        zoom.zoomIn()
        zoom.reset()
        assertEquals(LyricsZoom.DEFAULT, zoom.factor.value, delta)

        repeat(20) { zoom.zoomOut() }
        zoom.reset()
        assertEquals(LyricsZoom.DEFAULT, zoom.factor.value, delta)
    }

    @Test
    fun `a pinch applies its ratio and is clamped to the limits`() {
        val zoom = LyricsZoom()

        zoom.zoomBy(1.5f)
        assertEquals(1.5f, zoom.factor.value, delta)

        zoom.zoomBy(10f)
        assertEquals(LyricsZoom.MAX, zoom.factor.value, delta)

        zoom.zoomBy(0.001f)
        assertEquals(LyricsZoom.MIN, zoom.factor.value, delta)
    }

    @Test
    fun `many small pinch steps add up like one big one`() {
        val zoom = LyricsZoom()

        repeat(10) { zoom.zoomBy(1.05f) }

        assertEquals(Math.pow(1.05, 10.0).toFloat(), zoom.factor.value, 0.001f)
    }

    @Test
    fun `nonsense ratios are ignored rather than corrupting the size`() {
        val zoom = LyricsZoom()
        zoom.zoomBy(1.5f)

        zoom.zoomBy(0f)
        zoom.zoomBy(-2f)
        zoom.zoomBy(Float.NaN)

        assertEquals(1.5f, zoom.factor.value, delta)
    }

    @Test
    fun `default is recognised despite float drift from zooming in and back out`() {
        val zoom = LyricsZoom()

        repeat(5) { zoom.zoomIn() }
        repeat(5) { zoom.zoomOut() }

        assertTrue("drifted to ${zoom.factor.value}", LyricsZoom.isDefault(zoom.factor.value))
    }

    @Test
    fun `percentage is rounded for display`() {
        assertEquals(100, LyricsZoom.percent(1f))
        assertEquals(120, LyricsZoom.percent(1.2f))
        assertEquals(144, LyricsZoom.percent(1.44f))
        assertEquals(60, LyricsZoom.percent(LyricsZoom.MIN))
        assertEquals(300, LyricsZoom.percent(LyricsZoom.MAX))
        assertEquals(167, LyricsZoom.percent(1.6667f))
    }

    @Test
    fun `the state is shared across everyone holding the same instance`() {
        // The app keeps one LyricsZoom, so a size chosen on one song carries to the next.
        val zoom = LyricsZoom()
        val seenByNextSong = zoom

        zoom.zoomIn()

        assertEquals(1.2f, seenByNextSong.factor.value, delta)
    }
}
