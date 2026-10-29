package com.maralyrics.laitei.domain.repository

import com.maralyrics.laitei.domain.model.Account
import kotlinx.coroutines.flow.StateFlow

interface AccountRepository {
    /** The signed-in reader, or null when signed out. Loaded from the device on first use. */
    val account: StateFlow<Account?>

    /** Reads the saved session from the device. Safe to call more than once. */
    suspend fun load()

    /** Signs in with a username or email and a password. Fails with an [com.maralyrics.laitei.domain.model.AuthException]. */
    suspend fun signIn(identifier: String, password: String): Result<Account>

    /** The Google client id the website uses, which an ID token has to be issued for; null if it can't be fetched. */
    suspend fun googleClientId(): String?

    /** Signs in with the ID token Google gave the app. */
    suspend fun signInWithGoogle(idToken: String): Result<Account>

    /**
     * Signs in with a token the website's own sign-in page produced (used when the website must
     * show its security check, or the reader is creating an account there). Checks it with the
     * server before trusting it.
     */
    suspend fun adoptToken(token: String): Result<Account>

    /** Re-reads the account; signs out if the server no longer accepts the session. Offline is a no-op. */
    suspend fun refresh()

    /** Ends the account's other sessions; this device stays signed in. */
    suspend fun signOutEverywhere(): Result<Unit>

    /** The token for the website's own pages (the account page opened inside the app), or null. */
    fun tokenForWebsite(): String?

    suspend fun signOut()
}
