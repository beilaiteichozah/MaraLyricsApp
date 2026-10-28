package com.maralyrics.laitei.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class EventRemindersTest {

    private val today = EventMoment(2026, 11, 1)

    private fun event(
        id: String,
        title: String = id,
        start: EventMoment = today,
        end: EventMoment = start
    ) = CommunityEvent(id = id, title = title, description = null, location = null, start = start, end = end)

    // --- Which events -----------------------------------------------------------------------

    @Test
    fun `only events on today are picked, including multi-day events that span it`() {
        val events = listOf(
            event("today"),
            event("yesterday", start = EventMoment(2026, 10, 31)),
            event("tomorrow", start = EventMoment(2026, 11, 2)),
            event("spans", start = EventMoment(2026, 10, 30), end = EventMoment(2026, 11, 3)),
            event("ended", start = EventMoment(2026, 10, 28), end = EventMoment(2026, 10, 31))
        )

        assertEquals(listOf("spans", "today"), EventReminders.happeningOn(today, events).map { it.id })
    }

    @Test
    fun `an event listed by two calendars is announced once`() {
        val events = listOf(event("same"), event("same"), event("other"))

        assertEquals(listOf("other", "same"), EventReminders.happeningOn(today, events).map { it.id })
    }

    @Test
    fun `all-day events come first, then timed ones in start order`() {
        val events = listOf(
            event("evening", start = EventMoment(2026, 11, 1, 19, 30)),
            event("all-day"),
            event("morning", start = EventMoment(2026, 11, 1, 9, 0)),
            event("morning-later", start = EventMoment(2026, 11, 1, 9, 45))
        )

        assertEquals(
            listOf("all-day", "morning", "morning-later", "evening"),
            EventReminders.happeningOn(today, events).map { it.id }
        )
    }

    // --- Never twice ------------------------------------------------------------------------

    @Test
    fun `an announced event is not announced again the same day`() {
        val events = listOf(event("a"), event("b"))
        val announced = setOf(EventReminders.announcementKey(today, events[0]))

        assertEquals(listOf("b"), EventReminders.unannounced(today, events, announced).map { it.id })
    }

    @Test
    fun `a multi-day event is announced again on the next day`() {
        val multiDay = event("camp", start = EventMoment(2026, 11, 1), end = EventMoment(2026, 11, 3))
        val tomorrow = EventMoment(2026, 11, 2)
        val announcedToday = setOf(EventReminders.announcementKey(today, multiDay))

        assertTrue(EventReminders.unannounced(today, listOf(multiDay), announcedToday).isEmpty())
        assertEquals(listOf("camp"), EventReminders.unannounced(tomorrow, listOf(multiDay), announcedToday).map { it.id })
    }

    @Test
    fun `old keys are dropped so the saved list stays small`() {
        val keys = setOf("20261031:old", "20261101:a", "20261101:b", "20251101:last-year")

        assertEquals(setOf("20261101:a", "20261101:b"), EventReminders.pruned(today, keys))
    }

    // --- When -------------------------------------------------------------------------------

    private val yangon = TimeZone.getTimeZone("Asia/Yangon") // UTC+6:30, no daylight saving

    private fun millis(zone: TimeZone, y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        Calendar.getInstance(zone).apply { clear(); set(y, m - 1, d, h, min, 0) }.timeInMillis

    private val hour = 60L * 60 * 1000

    @Test
    fun `before the morning the reminder is due later today`() {
        val now = millis(yangon, 2026, 11, 1, 6, 30)

        assertEquals(90 * 60 * 1000L, EventReminders.delayUntilNextMorningMillis(now, yangon))
    }

    @Test
    fun `after the morning the reminder is due tomorrow morning`() {
        val now = millis(yangon, 2026, 11, 1, 14, 0)

        assertEquals(18 * hour, EventReminders.delayUntilNextMorningMillis(now, yangon))
    }

    @Test
    fun `at exactly the morning hour the next one is a day away`() {
        val now = millis(yangon, 2026, 11, 1, 8, 0)

        assertEquals(24 * hour, EventReminders.delayUntilNextMorningMillis(now, yangon))
    }

    @Test
    fun `a clock change keeps the reminder at eight o'clock local time`() {
        val newYork = TimeZone.getTimeZone("America/New_York")
        // The night US clocks go back an hour (2026-11-01 02:00 -> 01:00): that day is 25 hours long.
        val now = millis(newYork, 2026, 11, 1, 7, 0)
        val due = now + EventReminders.delayUntilNextMorningMillis(now, newYork)

        assertEquals(millis(newYork, 2026, 11, 1, 8, 0), due)

        val lateNight = millis(newYork, 2026, 10, 31, 23, 0)
        val dueAfterChange = lateNight + EventReminders.delayUntilNextMorningMillis(lateNight, newYork)
        assertEquals(millis(newYork, 2026, 11, 1, 8, 0), dueAfterChange)
    }
}
