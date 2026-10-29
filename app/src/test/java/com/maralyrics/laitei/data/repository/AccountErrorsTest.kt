package com.maralyrics.laitei.data.repository

import com.maralyrics.laitei.domain.model.AuthFailure
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** The bodies are the ones POST /account/login really sends. */
class AccountErrorsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun classify(status: Int, body: String?) = AccountErrors.classify(status, body, json)

    @Test
    fun `a wrong password or unknown account is just invalid credentials`() {
        assertEquals(AuthFailure.InvalidCredentials, classify(401, """{"error":"Invalid email or password"}"""))
    }

    @Test
    fun `a wrong password that also starts the security check is still a wrong password`() {
        // After the 2nd failure the 401 carries the code too; the next attempt is what needs the website.
        assertEquals(
            AuthFailure.InvalidCredentials,
            classify(401, """{"error":"Invalid email or password","code":"challenge_required"}""")
        )
    }

    @Test
    fun `the security check is recognised by its code`() {
        assertEquals(
            AuthFailure.ChallengeRequired,
            classify(403, """{"error":"Please complete the security check.","code":"challenge_required"}""")
        )
    }

    @Test
    fun `a 403 without that code is not mistaken for the security check`() {
        assertEquals(AuthFailure.Server("Forbidden"), classify(403, """{"error":"Forbidden"}"""))
    }

    @Test
    fun `too many attempts reads the wait from the message`() {
        assertEquals(
            AuthFailure.TooManyAttempts(15),
            classify(429, """{"error":"Too many failed attempts. Try again in 15 minutes."}""")
        )
        assertEquals(
            AuthFailure.TooManyAttempts(3),
            classify(429, """{"error":"Too many failed attempts. Try again in 3 minutes."}""")
        )
        assertEquals(AuthFailure.TooManyAttempts(null), classify(429, """{"error":"Slow down"}"""))
        assertEquals(AuthFailure.TooManyAttempts(null), classify(429, null))
    }

    @Test
    fun `anything else is a server problem and keeps the message`() {
        assertEquals(AuthFailure.Server("Accounts are not configured"), classify(500, """{"error":"Accounts are not configured"}"""))
        assertEquals(AuthFailure.Server(null), classify(502, "<html>Bad gateway</html>"))
        assertEquals(AuthFailure.Server(null), classify(500, ""))
        assertEquals(AuthFailure.Server(null), classify(500, null))
    }
}
