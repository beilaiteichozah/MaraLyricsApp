package com.maralyrics.laitei.domain.model

import kotlinx.coroutines.flow.StateFlow

/** Where syncing a signed-in reader's favorites and settings stands. */
data class AccountSyncStatus(
    val isSyncing: Boolean = false,
    /** Epoch millis of the last sync that finished, or null if none has yet. */
    val lastSyncedAt: Long? = null,
    /** True when the latest attempt couldn't reach the server; it is retried by the next change or visit. */
    val lastAttemptFailed: Boolean = false
)

/** What the rest of the app calls when something that syncs has changed on this device. */
interface SyncRecorder {
    fun favoriteChanged(slug: String, isFavorite: Boolean)
    fun favoritesAdded(slugs: Collection<String>)

    /** [key] is "theme" or "lang", [value] is the website's wording for it. */
    fun preferenceChanged(key: String, value: String)
}

interface AccountSync {
    val status: StateFlow<AccountSyncStatus>

    fun syncNow()

    /** Called when the app comes to the front; syncs unless it just did. */
    fun onAppResumed()
}

/** The website's spelling of the two settings that sync. */
object PrefMapping {
    /** The keys the website stores them under. */
    const val THEME_KEY = "theme"
    const val LANGUAGE_KEY = "lang"

    fun themeToWire(theme: AppTheme): String = when (theme) {
        AppTheme.LIGHT -> "light"
        AppTheme.DARK -> "dark"
        AppTheme.SYSTEM -> "system"
    }

    /** "time" (dark at night) is a website-only mode; the app follows the system for it. */
    fun themeFromWire(value: String?): AppTheme? = when (value?.trim()?.lowercase()) {
        "light" -> AppTheme.LIGHT
        "dark" -> AppTheme.DARK
        "system", "time" -> AppTheme.SYSTEM
        else -> null
    }

    fun languageToWire(language: AppLanguage): String = language.code

    fun languageFromWire(value: String?): AppLanguage? =
        AppLanguage.entries.firstOrNull { it.code == value?.trim()?.lowercase() }
}
