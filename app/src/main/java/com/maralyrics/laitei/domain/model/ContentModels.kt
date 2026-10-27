package com.maralyrics.laitei.domain.model

/** [isFromCache] is true when the network failed and [data] is the last saved copy. */
data class ContentResult<out T>(val data: T, val isFromCache: Boolean = false)

data class Article(
    val slug: String,
    val title: String,
    val authorName: String?,
    val summary: String?,
    /** HTML body — only loaded for the detail screen, null in list results. */
    val contentHtml: String?,
    /** Epoch millis, or null if the server sent no usable date. */
    val publishedAt: Long?
)

data class ArticlePage(
    val articles: List<Article>,
    val page: Int,
    val totalPages: Int
) {
    val hasMore: Boolean get() = page < totalPages
}

enum class IdolResult {
    WINNER, RUNNER_UP, SECOND_RUNNER_UP, FINALIST, SEMI_FINALIST, CONTESTANT;

    companion object {
        fun fromApi(value: String?): IdolResult = when (value?.trim()?.lowercase()) {
            "winner" -> WINNER
            "runner_up" -> RUNNER_UP
            "second_runner_up" -> SECOND_RUNNER_UP
            "finalist" -> FINALIST
            "semi_finalist" -> SEMI_FINALIST
            else -> CONTESTANT
        }
    }
}

data class IdolWinner(
    val name: String,
    val photoUrl: String?
)

data class IdolContestant(
    val name: String,
    val photoUrl: String?,
    val result: IdolResult,
    /** Slug of the matching artist profile, when the contestant also has songs on MaraLyrics. */
    val artistSlug: String?,
    val summary: String?
)

data class IdolVideo(
    val title: String?,
    val url: String
)

/** Season summary in the index; [description], [videos] and [contestants] only in the detail. */
data class IdolSeason(
    val slug: String,
    val title: String,
    val year: Int?,
    val venue: String?,
    /** ISO yyyy-MM-dd, kept as sent by the server. */
    val startDate: String?,
    val endDate: String?,
    val coverUrl: String?,
    val summary: String?,
    val description: String?,
    val contestantCount: Int,
    val winners: List<IdolWinner>,
    val videos: List<IdolVideo>,
    val contestants: List<IdolContestant>
)
