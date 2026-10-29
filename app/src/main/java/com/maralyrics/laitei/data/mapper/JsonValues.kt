package com.maralyrics.laitei.data.mapper

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** A flag the server sends as a boolean today; `1` or the word "true" would mean the same thing. */
internal fun JsonPrimitive?.isTruthy(): Boolean = when {
    this == null || this is JsonNull -> false
    booleanOrNull != null -> booleanOrNull == true
    else -> content.trim().let { it == "1" || it.equals("true", ignoreCase = true) }
}
