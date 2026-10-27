package com.maralyrics.laitei.utils

import com.maralyrics.laitei.domain.model.EventMoment
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** e.g. "Sep 21, 2026", in the app's current locale. */
fun formatMediumDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))

fun formatMediumDate(day: EventMoment): String =
    formatMediumDate(
        Calendar.getInstance().apply {
            clear()
            set(day.year, day.month - 1, day.day)
        }.timeInMillis
    )

/** e.g. "October 2026". */
fun formatMonthYear(year: Int, month: Int): String =
    SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(monthStart(year, month))

/** e.g. "Oct". */
fun formatMonthShort(year: Int, month: Int): String =
    SimpleDateFormat("LLL", Locale.getDefault()).format(monthStart(year, month))

private fun monthStart(year: Int, month: Int): Date =
    Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
    }.time

/** Local time of day, e.g. "2:30 PM"; empty for all-day moments. */
fun formatTimeOfDay(moment: EventMoment): String {
    val hour = moment.hour ?: return ""
    val calendar = Calendar.getInstance().apply {
        clear()
        set(moment.year, moment.month - 1, moment.day, hour, moment.minute ?: 0)
    }
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
}
