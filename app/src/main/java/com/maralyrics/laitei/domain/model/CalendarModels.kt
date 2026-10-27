package com.maralyrics.laitei.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class CommunityCalendar(
    val slug: String,
    val name: String,
    val description: String?,
    val eventCount: Int
)

/**
 * A calendar date, plus a wall-clock time for timed events ([hour] == null means all-day).
 * Ordered by date only, so "is this event on day X" never depends on the time of day.
 */
data class EventMoment(
    val year: Int,
    val month: Int, // 1..12
    val day: Int,
    val hour: Int? = null,
    val minute: Int? = null
) : Comparable<EventMoment> {

    val isAllDay: Boolean get() = hour == null

    /** yyyyMMdd as an Int — compares chronologically and is cheap to group and hash by. */
    val dayKey: Int get() = year * 10_000 + month * 100 + day

    override fun compareTo(other: EventMoment): Int = dayKey.compareTo(other.dayKey)

    /** Whole days from this date to [other] (negative if [other] is earlier). */
    fun daysUntil(other: EventMoment): Int {
        val a = toUtcMidnightMillis()
        val b = other.toUtcMidnightMillis()
        return Math.round((b - a) / MILLIS_PER_DAY.toDouble()).toInt()
    }

    private fun toUtcMidnightMillis(): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis

    companion object {
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
        private val DATE_ONLY = Regex("""^\d{4}-\d{2}-\d{2}$""")
        private val FRACTION = Regex("""\.\d+""")
        private val ZONE_SUFFIX = Regex("""(Z|[+-]\d{2}:?\d{2})$""")
        private val ZONE_NO_COLON = Regex("""([+-]\d{2})(\d{2})$""")

        fun today(): EventMoment = of(Calendar.getInstance())

        private fun of(c: Calendar, withTime: Boolean = false) = EventMoment(
            year = c.get(Calendar.YEAR),
            month = c.get(Calendar.MONTH) + 1,
            day = c.get(Calendar.DAY_OF_MONTH),
            hour = if (withTime) c.get(Calendar.HOUR_OF_DAY) else null,
            minute = if (withTime) c.get(Calendar.MINUTE) else null
        )

        /**
         * Parses what the calendar service sends: `yyyy-MM-dd` for all-day events, or an ISO
         * date-time for timed ones. A date-time with an offset or `Z` is converted to the
         * device's time zone; one without is read as local wall-clock time (same as the website).
         * Returns null for anything unrecognised so one bad event can't break the list.
         */
        fun parse(raw: String?): EventMoment? {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty()) return null
            return try {
                if (DATE_ONLY.matches(text)) {
                    val (y, m, d) = text.split('-').map { it.toInt() }
                    if (m !in 1..12 || d !in 1..31) null else EventMoment(y, m, d)
                } else {
                    parseDateTime(text)
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun parseDateTime(raw: String): EventMoment? {
            // "+0630" -> "+06:30" so a single XXX pattern covers Z, +06:30 and +0630.
            val text = raw.replace(' ', 'T').replace(FRACTION, "").replace(ZONE_NO_COLON, "$1:$2")
            val zone = ZONE_SUFFIX.find(text)?.value
            val clock = text.substringAfter('T').removeSuffix(zone.orEmpty())
            val pattern = buildString {
                append("yyyy-MM-dd'T'HH:mm")
                if (clock.count { it == ':' } >= 2) append(":ss")
                if (zone != null) append("XXX")
            }
            val format = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val date = format.parse(text) ?: return null
            return of(Calendar.getInstance().apply { time = date }, withTime = true)
        }
    }
}

data class CommunityEvent(
    val id: String,
    val title: String,
    val description: String?,
    val location: String?,
    val start: EventMoment,
    val end: EventMoment
) {
    val isMultiDay: Boolean get() = end > start

    fun isOn(day: EventMoment): Boolean = day >= start && day <= end

    fun isOver(today: EventMoment): Boolean = end < today
}
