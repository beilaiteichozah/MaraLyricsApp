package com.maralyrics.laitei.data.sync

import com.maralyrics.laitei.data.remote.AccountApiService
import com.maralyrics.laitei.data.remote.dto.SyncOpDto
import com.maralyrics.laitei.data.remote.dto.SyncRequest
import com.maralyrics.laitei.data.repository.SessionTokens
import com.maralyrics.laitei.data.mapper.isTruthy
import com.maralyrics.laitei.domain.model.PrefMapping
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException
import javax.inject.Inject

sealed interface SyncOutcome {
    data class Success(val favoritesAdded: Int, val favoritesRemoved: Int) : SyncOutcome

    /** Nobody is signed in, or the server turned the session down (and the app signed out). */
    data object SignedOut : SyncOutcome
    data object Offline : SyncOutcome
    data object Failed : SyncOutcome
}

/**
 * Keeps one signed-in reader's favorites, theme and language the same on every device they use.
 * Sends what changed here, takes what changed elsewhere, and applies it. One run at a time.
 */
class AccountSyncEngine internal constructor(
    private val api: AccountApiService,
    private val session: SessionTokens,
    private val stateStore: SyncStateStore,
    private val favorites: FavoriteStore,
    private val preferences: PreferenceStore,
    private val clock: () -> Long
) {
    @Inject
    constructor(
        api: AccountApiService,
        session: SessionTokens,
        stateStore: SyncStateStore,
        favorites: FavoriteStore,
        preferences: PreferenceStore
    ) : this(api, session, stateStore, favorites, preferences, System::currentTimeMillis)

    private val lock = Mutex()

    /** Remembers a favorite added or removed here, to be sent on the next sync. */
    suspend fun recordFavorite(slug: String, isFavorite: Boolean) {
        record(PendingOp(SyncKinds.FAVORITE, slug, at = clock(), deleted = !isFavorite))
    }

    suspend fun recordPreference(key: String, value: String) {
        record(PendingOp(SyncKinds.PREF, key, at = clock(), value = value))
    }

    private suspend fun record(op: PendingOp) = lock.withLock {
        val accountId = session.accountId() ?: return
        // Before the first sync there is nothing to add to: that sync reads the device's own state.
        val state = stateStore.read()?.takeIf { it.accountId == accountId && it.seeded } ?: return
        stateStore.write(SyncLogic.enqueue(state, op))
    }

    /** Forgets what was synced (on sign-out). The device's own favorites are left alone. */
    suspend fun reset() = lock.withLock { stateStore.clear() }

    suspend fun sync(): SyncOutcome = lock.withLock {
        val authorization = session.authorization() ?: return SyncOutcome.SignedOut
        val accountId = session.accountId() ?: return SyncOutcome.SignedOut

        var state = stateStore.read()?.takeIf { it.accountId == accountId } ?: SyncState(accountId)
        if (!state.seeded) state = SyncLogic.seed(state, favorites.favorites())

        do {
            val batch = state.queue.values.sortedBy { it.at }.take(SyncLogic.MAX_OPS)
            val response = try {
                api.sync(authorization, SyncRequest(since = state.rev, ops = batch.map { it.toDto() }))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                stateStore.write(state)
                return SyncOutcome.Offline
            } catch (e: Exception) {
                stateStore.write(state)
                return SyncOutcome.Failed
            }

            if (response.code() == 401) {
                session.invalidate()
                return SyncOutcome.SignedOut
            }
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                stateStore.write(state)
                return SyncOutcome.Failed
            }

            state = SyncLogic.applyResponse(state, batch, body)
            stateStore.write(state)
            val morePages = body.more.isTruthy()
        } while (morePages || state.queue.isNotEmpty())

        val changes = applyFavorites(state)
        applyPreferences(state)
        stateStore.write(state.copy(lastSyncedAt = clock()))
        SyncOutcome.Success(changes.add.size, changes.remove.size)
    }

    private suspend fun applyFavorites(state: SyncState): FavoriteChanges {
        val local = favorites.favorites().map { it.slug }.toSet()
        val changes = SyncLogic.planFavorites(state, local, favorites.library())
        for (slug in changes.add) {
            val at = state.items[SyncKinds.id(SyncKinds.FAVORITE, slug)]?.at ?: clock()
            favorites.add(slug, at)
        }
        for (slug in changes.remove) favorites.remove(slug)
        return changes
    }

    private suspend fun applyPreferences(state: SyncState) {
        SyncLogic.shouldApplyPreference(state, SyncKinds.PREF_THEME)?.let { item ->
            PrefMapping.themeFromWire(item.value)?.takeIf { it != preferences.theme() }?.let { preferences.setTheme(it) }
        }
        SyncLogic.shouldApplyPreference(state, SyncKinds.PREF_LANGUAGE)?.let { item ->
            PrefMapping.languageFromWire(item.value)?.takeIf { it != preferences.language() }?.let { preferences.setLanguage(it) }
        }
    }

    private fun PendingOp.toDto() = SyncOpDto(
        kind = kind,
        key = key,
        at = at,
        deleted = deleted,
        value = value?.let { JsonPrimitive(it) }
    )
}
