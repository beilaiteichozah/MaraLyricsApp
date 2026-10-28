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

/** A sponsor or partner of MaraLyrics. */
data class Supporter(
    val name: String,
    val description: String?,
    /** An inline `data:image` logo or an https URL. */
    val logoUrl: String?,
    /** Only ever an http(s) link, so it is safe to open. */
    val websiteUrl: String?
)

enum class TeamRole {
    EDITOR, MANAGER, SUPER_ADMIN, OTHER;

    companion object {
        fun fromApi(value: String?): TeamRole = when (value?.trim()?.lowercase()) {
            "editor" -> EDITOR
            "manager" -> MANAGER
            "super_admin" -> SUPER_ADMIN
            else -> OTHER
        }
    }
}

data class TeamMember(
    val name: String,
    val role: TeamRole,
    val photoUrl: String?
)

/** The website-sourced part of the Credits screen; the contributors list is still bundled. */
data class CreditsExtras(
    val sponsors: List<Supporter> = emptyList(),
    val partners: List<Supporter> = emptyList(),
    val team: List<TeamMember> = emptyList()
) {
    val isEmpty: Boolean get() = sponsors.isEmpty() && partners.isEmpty() && team.isEmpty()
}

enum class BadgePeriod {
    MONTH, YEAR, LIFETIME, OTHER;

    companion object {
        fun fromApi(value: String?): BadgePeriod = when (value?.trim()?.lowercase()) {
            "month" -> MONTH
            "year" -> YEAR
            "lifetime" -> LIFETIME
            else -> OTHER
        }
    }
}

/** A recognition awarded to an artist or composer, e.g. "Artist of the Year · 2024". */
data class ProfileBadge(
    val period: BadgePeriod,
    /** "2024" or "2026-09"; null for a lifetime award. */
    val periodValue: String?,
    /** The award's own name when the server sent one; the screen words it from [period] otherwise. */
    val title: String?
)

/** What the website knows about a profile beyond what is stored in the offline library. */
data class ProfileExtras(
    val badges: List<ProfileBadge> = emptyList(),
    /** The green tick shown beside the name of a paid profile. */
    val isGreen: Boolean = false,
    /** The bio as sanitized HTML (bold, links, lists...); null when there is only plain text. */
    val bioHtml: String? = null
)

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
