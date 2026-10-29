package com.maralyrics.laitei.data.repository

import com.maralyrics.laitei.data.remote.dto.ApiErrorDto
import com.maralyrics.laitei.domain.model.AuthFailure
import kotlinx.serialization.json.Json

/** Turns the account API's error answers into something the sign-in screen can act on. */
internal object AccountErrors {

    /** The code the server sends when it wants its Cloudflare security check completed. */
    const val CHALLENGE_REQUIRED = "challenge_required"

    private val MINUTES = Regex("""(\d+)\s*minute""", RegexOption.IGNORE_CASE)

    fun classify(status: Int, body: String?, json: Json): AuthFailure {
        val error = body?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<ApiErrorDto>(it) }.getOrNull() }
        val message = error?.error ?: error?.message

        return when {
            status == 403 && error?.code == CHALLENGE_REQUIRED -> AuthFailure.ChallengeRequired
            // "Too many failed attempts. Try again in 15 minutes."
            status == 429 -> AuthFailure.TooManyAttempts(
                message?.let { MINUTES.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            )
            // A wrong answer is a 401 whether or not the account exists, by design.
            status == 401 -> AuthFailure.InvalidCredentials
            else -> AuthFailure.Server(message)
        }
    }
}
