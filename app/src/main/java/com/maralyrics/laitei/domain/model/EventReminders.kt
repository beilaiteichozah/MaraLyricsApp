package com.maralyrics.laitei.domain.model

import java.util.Calendar
import java.util.TimeZone

/** The rules behind the "event today" reminder, kept free of Android so they can be tested. */
object EventReminders {

    /** Hour of the day (device time) the reminder is due. */
    const val MORNING_HOUR = 8

    /**
     * The events on [today] from all calendars, each once even if a calendar lists it twice, with
     * all-day events first and timed ones in the order they start.
     */
    fun happeningOn(today: EventMoment, events: List<CommunityEvent>): List<CommunityEvent> =
        events
            .filter { it.isOn(today) }
            .distinctBy { it.id }
            .sortedWith(compareBy<CommunityEvent> { it.start.hour ?: -1 }.thenBy { it.start.minute ?: 0 }.thenBy { it.title })

    /** Identifies "this event, announced on this day"; a multi-day event is announced afresh each day. */
    fun announcementKey(today: EventMoment, event: CommunityEvent): String = "${today.dayKey}:${event.id}"

    /** What is still to announce: events on [today] whose key isn't in [announced]. */
    fun unannounced(today: EventMoment, events: List<CommunityEvent>, announced: Set<String>): List<CommunityEvent> =
        happeningOn(today, events).filter { announcementKey(today, it) !in announced }

    /** Keeps only today's keys, so the saved list never grows past one day of events. */
    fun pruned(today: EventMoment, announced: Set<String>): Set<String> =
        announced.filterTo(mutableSetOf()) { it.startsWith("${today.dayKey}:") }

    /**
     * Milliseconds from [nowMillis] until the next [hour]:00 in [timeZone] — today's if it hasn't
     * come yet, otherwise tomorrow's — so the first reminder arrives in the morning, not whenever
     * the user happened to switch it on.
     */
    fun delayUntilNextMorningMillis(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        hour: Int = MORNING_HOUR
    ): Long {
        val next = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_MONTH, 1)
        }
        return next.timeInMillis - nowMillis
    }
}
