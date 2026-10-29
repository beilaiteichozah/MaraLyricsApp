package com.maralyrics.laitei.domain.model

import kotlinx.serialization.Serializable

/** A reader's MaraLyrics account, as the website knows it. */
@Serializable
data class Account(
    val id: Long,
    val username: String,
    val displayName: String? = null,
    val email: String? = null,
    val emailVerified: Boolean = false,
    /** False for an account that only signs in with Google. */
    val hasPassword: Boolean = true,
    val hasGoogle: Boolean = false
) {
    /** What to call the reader on screen: their display name if they set one, else their username. */
    val label: String get() = displayName?.takeIf { it.isNotBlank() } ?: username
}

/** Why a sign-in didn't work, in terms the sign-in screen can act on. */
sealed interface AuthFailure {
    /** Wrong username/email or password. */
    data object InvalidCredentials : AuthFailure

    /** The website wants its security check, which only the website can show. */
    data object ChallengeRequired : AuthFailure

    /** Too many wrong attempts; [minutes] is how long to wait, when the server said. */
    data class TooManyAttempts(val minutes: Int?) : AuthFailure

    data object Offline : AuthFailure

    /**
     * A Google account that isn't registered on MaraLyrics yet. Creating an account needs the
     * website's security check, which only the website can show.
     */
    data object NewGoogleAccount : AuthFailure

    /** There is no Google account on this phone to choose. */
    data object GoogleNoAccount : AuthFailure

    /** Google turned the app down: this build isn't registered with Google for sign-in yet. */
    data object GoogleNotSetUp : AuthFailure

    data object GoogleFailed : AuthFailure

    data class Server(val message: String?) : AuthFailure
}

class AuthException(val failure: AuthFailure) : Exception(failure.toString())
