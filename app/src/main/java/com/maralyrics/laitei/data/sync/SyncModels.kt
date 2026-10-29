package com.maralyrics.laitei.data.sync

import com.maralyrics.laitei.domain.model.AppLanguage
import com.maralyrics.laitei.domain.model.PrefMapping
import com.maralyrics.laitei.domain.model.AppTheme
import kotlinx.serialization.Serializable

/**
 * The account sync protocol (POST /account/sync) in the terms the app uses. Every synced thing is
 * a (kind, key) pair with the time it last changed and whether it was removed; the newest change
 * wins, and a removal is kept as a "tombstone" so other devices learn of it.
 */
object SyncKinds {
    const val FAVORITE = "favorite"
    const val PREF = "pref"

    /** Preference keys the app shares with the website. */
    const val PREF_THEME = PrefMapping.THEME_KEY
    const val PREF_LANGUAGE = PrefMapping.LANGUAGE_KEY

    fun id(kind: String, key: String) = "$kind:$key"
}

/** A change made on this device that the server hasn't seen yet. */
@Serializable
data class PendingOp(
    val kind: String,
    val key: String,
    val at: Long,
    val deleted: Boolean = false,
    val value: String? = null
) {
    val id: String get() = SyncKinds.id(kind, key)
}

/** The server's current word on one (kind, key). */
@Serializable
data class SyncedItem(
    val at: Long,
    val deleted: Boolean = false,
    val value: String? = null
)

/**
 * Everything the app remembers between syncs, for one account on this device.
 *
 * [items] is the server's copy of favorites and preferences (so a favorite for a song that hasn't
 * been downloaded yet is kept until it has); [queue] is what still has to be sent.
 */
@Serializable
data class SyncState(
    val accountId: Long,
    /** The server's revision this device has caught up to; asked for "everything after" next time. */
    val rev: Long = 0,
    val items: Map<String, SyncedItem> = emptyMap(),
    val queue: Map<String, PendingOp> = emptyMap(),
    /** When each preference was last changed on this device, so an older server value never undoes it. */
    val prefChangedAt: Map<String, Long> = emptyMap(),
    /** Whether the favorites this device already had were put in the queue (done once per account). */
    val seeded: Boolean = false,
    val lastSyncedAt: Long? = null
)

/** Where [SyncState] is kept. Not part of the session, so signing out can forget it independently. */
interface SyncStateStore {
    suspend fun read(): SyncState?
    suspend fun write(state: SyncState)
    suspend fun clear()
}

/** A favorite on this device, with when it was added. */
data class LocalFavorite(val slug: String, val at: Long)

/** The favorites and the song library on this device, as the sync sees them. */
interface FavoriteStore {
    suspend fun favorites(): List<LocalFavorite>

    /** Slugs of every downloaded song; a favorite can only be applied for a song that is here. */
    suspend fun library(): Set<String>

    suspend fun add(slug: String, at: Long)
    suspend fun remove(slug: String)
}

/** The two settings that follow the reader between devices. */
interface PreferenceStore {
    suspend fun theme(): AppTheme
    suspend fun language(): AppLanguage
    suspend fun setTheme(theme: AppTheme)
    suspend fun setLanguage(language: AppLanguage)
}
