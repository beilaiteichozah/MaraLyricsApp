package com.maralyrics.laitei.data.sync

import com.maralyrics.laitei.data.mapper.isTruthy
import com.maralyrics.laitei.data.remote.dto.SyncResponse
import kotlinx.serialization.json.JsonNull

/** What to do to this device's favorites so they match the server's. */
data class FavoriteChanges(val add: List<String>, val remove: List<String>)

/** The merging rules of the sync, free of networking and storage so they can be tested exhaustively. */
object SyncLogic {

    /** The server accepts at most this many changes per request. */
    const val MAX_OPS = 200

    /**
     * Records a change made on this device. If a newer change to the same thing is already waiting
     * (two quick taps), the newer one stays.
     */
    fun enqueue(state: SyncState, op: PendingOp): SyncState {
        val existing = state.queue[op.id]
        if (existing != null && existing.at > op.at) return state
        return state.copy(
            queue = state.queue + (op.id to op),
            prefChangedAt = if (op.kind == SyncKinds.PREF) state.prefChangedAt + (op.key to op.at) else state.prefChangedAt
        )
    }

    /**
     * The first time an account syncs on this device, everything the reader already favorited here
     * joins the queue, so signing in adds to the account rather than being overwritten by it.
     */
    fun seed(state: SyncState, favorites: List<LocalFavorite>): SyncState {
        var next = state
        for (favorite in favorites) {
            next = enqueue(next, PendingOp(SyncKinds.FAVORITE, favorite.slug, at = favorite.at.coerceAtLeast(1)))
        }
        return next.copy(seeded = true)
    }

    /**
     * Folds the server's answer in: what it says now about each favorite and setting replaces what
     * was known, the changes that were just sent leave the queue (unless changed again since), and
     * the revision moves on.
     */
    fun applyResponse(state: SyncState, sent: List<PendingOp>, response: SyncResponse): SyncState {
        val items = state.items.toMutableMap()
        for (item in response.items) {
            if (item.kind != SyncKinds.FAVORITE && item.kind != SyncKinds.PREF) continue
            if (item.key.isBlank()) continue
            val id = SyncKinds.id(item.kind, item.key)
            if (items[id]?.let { it.at > item.at } == true) continue
            items[id] = SyncedItem(
                at = item.at,
                deleted = item.deleted.isTruthy(),
                value = item.value?.takeIf { it !is JsonNull }?.content
            )
        }
        val stillWaiting = state.queue.filter { (id, op) -> sent.none { it.id == id && it.at == op.at } }
        return state.copy(items = items, queue = stillWaiting, rev = maxOf(state.rev, response.rev))
    }

    /**
     * Which favorites to add or remove here. A change that is still waiting to be sent is the
     * newest word on that song, so the server's older answer doesn't override it; a favorite for a
     * song that isn't downloaded yet is left for a later sync.
     */
    fun planFavorites(state: SyncState, local: Set<String>, library: Set<String>): FavoriteChanges {
        val waiting = state.queue.values.filter { it.kind == SyncKinds.FAVORITE }.map { it.key }.toSet()
        val add = mutableListOf<String>()
        val remove = mutableListOf<String>()
        for ((id, item) in state.items) {
            if (!id.startsWith("${SyncKinds.FAVORITE}:")) continue
            val slug = id.removePrefix("${SyncKinds.FAVORITE}:")
            if (slug in waiting) continue
            when {
                !item.deleted && slug !in local && slug in library -> add += slug
                item.deleted && slug in local -> remove += slug
            }
        }
        return FavoriteChanges(add.sorted(), remove.sorted())
    }

    /** A server setting applies only if it is newer than the last time it was changed here. */
    fun shouldApplyPreference(state: SyncState, key: String): SyncedItem? {
        val item = state.items[SyncKinds.id(SyncKinds.PREF, key)] ?: return null
        if (item.deleted || item.value == null) return null
        return item.takeIf { it.at > (state.prefChangedAt[key] ?: 0L) }
    }
}
