package com.maralyrics.laitei.domain.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class EventMomentTest {

    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
        // Myanmar time (UTC+06:30): a half-hour offset catches off-by-one-hour conversion bugs.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+06:30"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun `date-only string is an all-day moment`() {
        val moment = EventMoment.parse("2026-11-01")
        assertEquals(EventMoment(2026, 11, 1), moment)
        assertTrue(moment!!.isAllDay)
    }

    @Test
    fun `unreadable input parses to null instead of throwing`() {
        assertNull(EventMoment.parse(null))
        assertNull(EventMoment.parse(""))
        assertNull(EventMoment.parse("   "))
        assertNull(EventMoment.parse("next tuesday"))
        assertNull(EventMoment.parse("2026-13-01"))
        assertNull(EventMoment.parse("2026-00-10"))
    }

    @Test
    fun `date-time without an offset is read as local wall-clock time`() {
        assertEquals(EventMoment(2026, 3, 5, 14, 30), EventMoment.parse("2026-03-05T14:30:00"))
        assertEquals(EventMoment(2026, 3, 5, 14, 30), EventMoment.parse("2026-03-05T14:30"))
        assertEquals(EventMoment(2026, 3, 5, 14, 30), EventMoment.parse("2026-03-05 14:30"))
    }

    @Test
    fun `UTC date-time is converted to the device time zone and can roll into the next day`() {
        // 23:30Z is 06:00 the next morning at UTC+06:30.
        assertEquals(EventMoment(2026, 3, 6, 6, 0), EventMoment.parse("2026-03-05T23:30:00Z"))
    }

    @Test
    fun `offset forms and fractional seconds are understood`() {
        val expected = EventMoment(2026, 3, 5, 10, 0)
        assertEquals(expected, EventMoment.parse("2026-03-05T10:00:00+06:30"))
        assertEquals(expected, EventMoment.parse("2026-03-05T10:00:00+0630"))
        assertEquals(expected, EventMoment.parse("2026-03-05T10:00:00.000+06:30"))
        assertEquals(EventMoment(2026, 3, 5, 10, 30), EventMoment.parse("2026-03-05T04:00:00.000Z"))
    }

    @Test
    fun `midnight UTC all-day style timestamp still resolves to a valid moment`() {
        // The calendars endpoint sends "2026-11-01T00:00:00.000Z" for all-day next_events.
        assertEquals(EventMoment(2026, 11, 1, 6, 30), EventMoment.parse("2026-11-01T00:00:00.000Z"))
    }

    @Test
    fun `daysUntil counts whole calendar days across month and year ends`() {
        assertEquals(0, EventMoment(2026, 5, 5).daysUntil(EventMoment(2026, 5, 5)))
        assertEquals(1, EventMoment(2026, 12, 31).daysUntil(EventMoment(2027, 1, 1)))
        assertEquals(2, EventMoment(2026, 3, 28).daysUntil(EventMoment(2026, 3, 30)))
        assertEquals(-1, EventMoment(2026, 3, 1).daysUntil(EventMoment(2026, 2, 28)))
        // 2028 is a leap year.
        assertEquals(2, EventMoment(2028, 2, 28).daysUntil(EventMoment(2028, 3, 1)))
    }

    @Test
    fun `daysUntil is not thrown off by daylight saving changes in the device zone`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        // Clocks go forward on 2026-03-08 in New York.
        assertEquals(2, EventMoment(2026, 3, 7).daysUntil(EventMoment(2026, 3, 9)))
    }

    @Test
    fun `moments compare by date only, ignoring time of day`() {
        val morning = EventMoment(2026, 1, 1, 9, 0)
        val evening = EventMoment(2026, 1, 1, 17, 0)
        assertEquals(0, morning.compareTo(evening))
        assertTrue(EventMoment(2026, 1, 2) > evening)
        assertTrue(EventMoment(2025, 12, 31, 23, 59) < morning)
    }

    @Test
    fun `multi-day event covers every day in its range and is over only after it ends`() {
        val event = CommunityEvent(
            id = "1", title = "Retreat", description = null, location = null,
            start = EventMoment(2026, 1, 1), end = EventMoment(2026, 1, 3)
        )
        assertTrue(event.isMultiDay)
        assertFalse(event.isOn(EventMoment(2025, 12, 31)))
        assertTrue(event.isOn(EventMoment(2026, 1, 1)))
        assertTrue(event.isOn(EventMoment(2026, 1, 2)))
        assertTrue(event.isOn(EventMoment(2026, 1, 3)))
        assertFalse(event.isOn(EventMoment(2026, 1, 4)))

        assertFalse(event.isOver(EventMoment(2026, 1, 3)))
        assertTrue(event.isOver(EventMoment(2026, 1, 4)))
    }

    @Test
    fun `single-day event is not multi-day`() {
        val event = CommunityEvent(
            id = "1", title = "Sunday", description = null, location = null,
            start = EventMoment(2026, 10, 25), end = EventMoment(2026, 10, 25)
        )
        assertFalse(event.isMultiDay)
        assertTrue(event.isOn(EventMoment(2026, 10, 25)))
    }
}
