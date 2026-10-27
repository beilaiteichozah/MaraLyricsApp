package com.maralyrics.laitei.domain.model

/** Where a song sits in the list being paged through, and the songs either side of it. */
data class SongNeighbors(
    val previousId: Long?,
    val nextId: Long?,
    /** 1-based place in the list. */
    val position: Int,
    val total: Int
) {
    companion object {
        /** Null when [currentId] isn't in [order], i.e. there is nothing sensible to page through. */
        fun of(order: List<Long>, currentId: Long): SongNeighbors? {
            val index = order.indexOf(currentId)
            if (index < 0) return null
            return SongNeighbors(
                previousId = order.getOrNull(index - 1),
                nextId = order.getOrNull(index + 1),
                position = index + 1,
                total = order.size
            )
        }
    }
}
