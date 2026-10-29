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
    private const val CALENDAR_PROMPTED_KEY = "ml_calendar_pref_prompted"
    private const val APP_PROMO_SEEN_KEY = "ml_app_promo_seen"

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
     * A tiny file on the same site. The page is opened through it so the app can write into the
     * site's storage first (see [prepareScript]) before the account page's own scripts read it.
     */
    const val PREPARE_URL = "https://maralyrics.com/icon.svg"

    /**
     * Run on [PREPARE_URL], before the account page loads:
     *  - marks the website's first-visit pop-ups ("pick your calendar", "get the Android app") as
     *    already seen. They are pointless inside the app that is the Android app, and the first one
     *    covers the sign-in form;
     *  - if [token] is given, makes it the page's own sign-in so the account page opens signed in.
     * The token is only ever put into the script if it is shaped like a token, so nothing else can be.
     */
    fun prepareScript(token: String?): String {
        val literal = token?.takeIf { TOKEN_SHAPE.matches(it) }?.let { "'$it'" } ?: "null"
        return "(function(t){try{" +
            "localStorage.setItem('$CALENDAR_PROMPTED_KEY','1');" +
            "localStorage.setItem('$APP_PROMO_SEEN_KEY','1');" +
            "if(t){localStorage.setItem('$TOKEN_KEY',t);}return 'ok';}" +
            "catch(e){return 'error';}})($literal)"
    }

    /**
     * Google refuses to sign anyone in from inside an app's own browser window, so the page's
     * "Continue with Google" button (and the "or use your email" line that goes with it) would only
     * lead nowhere. Hidden here until the app has its own Google sign-in.
     */
    val hideGoogleSignInScript: String =
        "(function(){var s=document.createElement('style');" +
            "s.textContent='#googleBlock{display:none!important}';" +
            "document.head.appendChild(s);return 'ok';})()"

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
