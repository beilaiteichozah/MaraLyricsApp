package com.maralyrics.laitei.presentation.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountWebTest {

    private val token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOjEyfQ.c2lnbmF0dXJl"

    @Test
    fun `only https pages on the website itself load inside the app`() {
        assertTrue(AccountWeb.isAllowedPage("https://maralyrics.com/account"))
        assertTrue(AccountWeb.isAllowedPage("https://www.maralyrics.com/account?verify=abc"))
        assertTrue(AccountWeb.isAllowedPage("HTTPS://MaraLyrics.com/"))

        assertFalse(AccountWeb.isAllowedPage("http://maralyrics.com/account"))
        assertFalse(AccountWeb.isAllowedPage("https://accounts.google.com/o/oauth2/auth"))
        assertFalse(AccountWeb.isAllowedPage("https://maralyrics.com.evil.example/account"))
        assertFalse(AccountWeb.isAllowedPage("https://evilmaralyrics.com/account"))
        assertFalse(AccountWeb.isAllowedPage("https://maralyrics.com@evil.example/account"))
        assertFalse(AccountWeb.isAllowedPage("intent://maralyrics.com/#Intent;scheme=https;end"))
        assertFalse(AccountWeb.isAllowedPage("javascript:alert(1)"))
        assertFalse(AccountWeb.isAllowedPage("file:///sdcard/x.html"))
        assertFalse(AccountWeb.isAllowedPage("not a url"))
        assertFalse(AccountWeb.isAllowedPage(null))
    }

    @Test
    fun `the page the app opens first is on the website, so its storage is the account page's storage`() {
        assertTrue(AccountWeb.isAllowedPage(AccountWeb.PREPARE_URL))
        assertTrue(AccountWeb.isAllowedPage(AccountWeb.ACCOUNT_URL))
    }

    @Test
    fun `the preparation marks both first-visit pop-ups as seen`() {
        val script = AccountWeb.prepareScript(null)

        assertTrue(script.contains("localStorage.setItem('ml_calendar_pref_prompted','1')"))
        assertTrue(script.contains("localStorage.setItem('ml_app_promo_seen','1')"))
        assertTrue("no token: the page starts signed out", script.endsWith("})(null)"))
    }

    @Test
    fun `the preparation can also sign the page in with a real token`() {
        val script = AccountWeb.prepareScript(token)

        assertTrue(script.contains("localStorage.setItem('ml_account_token',t)"))
        assertTrue(script.endsWith("})('$token')"))
    }

    @Test
    fun `text that is not shaped like a token is never put into a page script`() {
        val bad = listOf(
            "",
            "abc",
            "a.b.c",
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOjEyfQ.c2ln');alert(1);('",
            "$token\nalert(1)",
            "$token;"
        )
        for (candidate in bad) {
            val script = AccountWeb.prepareScript(candidate)
            assertTrue("accepted: $candidate", script.endsWith("})(null)"))
            assertFalse("leaked: $candidate", script.contains("alert"))
        }
    }

    @Test
    fun `the Google sign-in button is hidden because Google blocks it inside an app window`() {
        assertTrue(AccountWeb.hideGoogleSignInScript.contains("#googleBlock{display:none!important}"))
    }

    @Test
    fun `a script result comes back as the text inside the JSON string`() {
        assertEquals("eyJ.abc.def", AccountWeb.parseScriptResult("\"eyJ.abc.def\""))
        assertEquals("ok", AccountWeb.parseScriptResult("\"ok\""))
        assertEquals("", AccountWeb.parseScriptResult("\"\""))
    }

    @Test
    fun `a missing or odd script result is nothing, not a crash`() {
        assertNull(AccountWeb.parseScriptResult(null))
        assertNull(AccountWeb.parseScriptResult(""))
        assertNull(AccountWeb.parseScriptResult("null"))
        assertNull(AccountWeb.parseScriptResult("42"))
        assertNull(AccountWeb.parseScriptResult("true"))
        assertNull(AccountWeb.parseScriptResult("{\"a\":1}"))
        assertNull(AccountWeb.parseScriptResult("not json {"))
    }
}
