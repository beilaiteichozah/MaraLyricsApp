package com.maralyrics.laitei.presentation.navigation

/**
 * The song view's route. Besides the song to show it can carry the ids of the list it was opened
 * from, in the order that list was showing them, so Previous/Next walk that list (the Home sort or
 * search results, Favorites, a category, an artist's songs) instead of just song numbers.
 *
 * The order travels in the route so it survives process death along with the rest of the back
 * stack; a route without it (widgets, notifications, a restored session) pages by song number.
 */
object SongRoute {
    const val ARG_SONG_ID = "songId"
    const val ARG_ORDER = "order"
    const val PATTERN = "song_detail/{$ARG_SONG_ID}?$ARG_ORDER={$ARG_ORDER}"

    // A route is stored and matched as one string; beyond this the list is long enough that
    // paging by song number is the better deal than a multi-kilobyte route.
    private const val MAX_ORDER_IDS = 1500

    fun create(songId: Long, order: List<Long> = emptyList()): String {
        val route = "song_detail/$songId"
        if (order.isEmpty() || order.size > MAX_ORDER_IDS) return route
        return "$route?$ARG_ORDER=${order.joinToString(",")}"
    }

    /** The ids from [create]'s order, each once; anything unreadable is skipped. */
    fun parseOrder(raw: String?): List<Long> =
        raw.orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }.distinct()
}
