package com.maralyrics.laitei.presentation.account

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.net.URI

/**
 * The rules for showing the website's own account page inside the app. That page does what a phone
 * can't do natively: it shows Cloudflare's security check (needed to create an account, and after
 * repeated wrong passwords), and it holds the profile photo, password, data download and account
 * deletion screens.
 */
internal object AccountWeb {

    const val ACCOUNT_URL = "https://maralyrics.com/account"

    /** Where the website keeps the session token once someone is signed in on that page. */
    private const val TOKEN_KEY = "ml_account_token"

    private val HOSTS = setOf("maralyrics.com", "www.maralyrics.com")
    private val TOKEN_SHAPE = Regex("""[A-Za-z0-9_-]{4,}\.[A-Za-z0-9_-]{4,}\.[A-Za-z0-9_-]{4,}""")

    /** Only the website itself may load in the app's window; every other link opens in the browser. */
    fun isAllowedPage(url: String?): Boolean {
        val uri = try { URI(url ?: return false) } catch (e: Exception) { return false }
        return uri.scheme.equals("https", ignoreCase = true) && uri.host?.lowercase() in HOSTS
    }

    /** Reads the token the page saved, or "" if there isn't one. */
    val readTokenScript: String =
        "(function(){try{return localStorage.getItem('$TOKEN_KEY')||'';}catch(e){return '';}})()"

    /**
     * Saves [token] as the page's own sign-in, so the account page opens already signed in. Null if
     * [token] isn't shaped like a token, so nothing but a token is ever written into a page script.
     */
    fun signInScript(token: String): String? {
        if (!TOKEN_SHAPE.matches(token)) return null
        return "(function(t){try{if(localStorage.getItem('$TOKEN_KEY')!==t){" +
            "localStorage.setItem('$TOKEN_KEY',t);return 'changed';}return 'same';}" +
            "catch(e){return 'error';}})('$token')"
    }

    /** `evaluateJavascript` hands back the result as JSON text ("\"abc\"" or "null"); this is the string inside. */
    fun parseScriptResult(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "null") return null
        return try {
            (Json.parseToJsonElement(raw) as? JsonPrimitive)?.takeIf { it !is JsonNull && it.isString }?.content
        } catch (e: Exception) {
            null
        }
    }
}
