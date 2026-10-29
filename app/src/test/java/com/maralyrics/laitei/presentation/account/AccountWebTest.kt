package com.maralyrics.laitei.presentation.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountWebTest {

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
    fun `the sign-in script embeds a real token and nothing else`() {
        val token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOjEyfQ.c2lnbmF0dXJl"

        val script = AccountWeb.signInScript(token)!!

        assertTrue(script.contains("'$token'"))
        assertTrue(script.contains("localStorage.setItem('ml_account_token',t)"))
    }

    @Test
    fun `text that is not shaped like a token is never put into a page script`() {
        assertNull(AccountWeb.signInScript(""))
        assertNull(AccountWeb.signInScript("abc"))
        assertNull(AccountWeb.signInScript("a.b.c"))
        assertNull(AccountWeb.signInScript("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOjEyfQ.c2ln');alert(1);('"))
        assertNull(AccountWeb.signInScript("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOjEyfQ.c2lnbmF0dXJl\nalert(1)"))
        assertNull(AccountWeb.signInScript("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOjEyfQ.c2lnbmF0dXJl;"))
    }

    @Test
    fun `a script result comes back as the text inside the JSON string`() {
        assertEquals("eyJ.abc.def", AccountWeb.parseScriptResult("\"eyJ.abc.def\""))
        assertEquals("changed", AccountWeb.parseScriptResult("\"changed\""))
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
