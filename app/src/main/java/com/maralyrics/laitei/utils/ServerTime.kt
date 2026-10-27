package com.maralyrics.laitei.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** The API sends "yyyy-MM-dd HH:mm:ss" in server (UTC) time with no offset. Null if unparsable. */
fun parseServerTimestamp(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return try {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(value.trim())?.time
    } catch (e: Exception) {
        null
    }
}
