package com.maralyrics.laitei.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

// DTOs for the website "extras" — articles, Mara Idol, and the community calendar. They are also
// what ContentCache stores, so every field the app reads has a default: a payload cached by one
// app version must still decode after the DTO grows a field.

@Serializable
data class ArticleDto(
    @SerialName("slug") val slug: String,
    @SerialName("title") val title: String,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("summary") val summary: String? = null,
    // Only present on GET /articles/{slug}; the list endpoint omits the body.
    @SerialName("content") val content: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ArticlesResponse(
    @SerialName("articles") val articles: List<ArticleDto> = emptyList(),
    @SerialName("total") val total: Int = 0,
    @SerialName("page") val page: Int = 1,
    @SerialName("totalPages") val totalPages: Int = 1
)

@Serializable
data class IdolWinnerDto(
    @SerialName("name") val name: String,
    @SerialName("photo_url") val photoUrl: String? = null
)

@Serializable
data class IdolVideoDto(
    @SerialName("title") val title: String? = null,
    @SerialName("url") val url: String
)

@Serializable
data class IdolContestantDto(
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String = "",
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("result") val result: String? = null,
    @SerialName("artist_slug") val artistSlug: String? = null,
    @SerialName("summary") val summary: String? = null
)

// One shape for both GET /mara-idol (seasons only) and GET /mara-idol/{season}, which adds
// description, videos and contestants. The index also carries a flat "idols" list that
// duplicates every contestant photo as base64 — it is deliberately not modelled, which keeps
// the cached copy about half the size of the response.
@Serializable
data class IdolSeasonDto(
    @SerialName("slug") val slug: String,
    @SerialName("title") val title: String,
    @SerialName("year") val year: Int? = null,
    @SerialName("venue") val venue: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("contestant_count") val contestantCount: Int = 0,
    @SerialName("summary") val summary: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("winners") val winners: List<IdolWinnerDto> = emptyList(),
    @SerialName("videos") val videos: List<IdolVideoDto> = emptyList(),
    @SerialName("contestants") val contestants: List<IdolContestantDto> = emptyList()
)

@Serializable
data class IdolIndexResponse(
    @SerialName("seasons") val seasons: List<IdolSeasonDto> = emptyList()
)

@Serializable
data class CalendarDto(
    @SerialName("slug") val slug: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("event_count") val eventCount: Int = 0
)

@Serializable
data class CalendarsResponse(
    @SerialName("calendars") val calendars: List<CalendarDto> = emptyList()
)

@Serializable
data class CalendarEventDto(
    // A UUID today, but this is a third-party service — JsonPrimitive accepts a number or a
    // string instead of failing the whole year's events if that ever changes.
    @SerialName("id") val id: JsonPrimitive? = null,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String? = null,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("location") val location: String? = null
)

@Serializable
data class CalendarEventsResponse(
    @SerialName("year") val year: Int = 0,
    @SerialName("events") val events: List<CalendarEventDto> = emptyList()
)

// Recognition shown on artist and composer profiles. GET /artists/{slug} and the /artists list
// carry far more (the photo as base64, the full bio, every song), so this models only what the
// app reads, which also keeps the cached copy small. "green" and "period_value" are read as raw
// JSON values so a boolean-or-number or string-or-number change on the server can't fail a
// whole list.
@Serializable
data class BadgeDto(
    @SerialName("period") val period: String? = null,
    @SerialName("period_value") val periodValue: JsonPrimitive? = null,
    @SerialName("title") val title: String? = null
)

@Serializable
data class ProfileExtrasDto(
    @SerialName("slug") val slug: String = "",
    @SerialName("badges") val badges: List<BadgeDto> = emptyList(),
    @SerialName("green") val green: JsonPrimitive? = null,
    // Only on the single-profile endpoint; the list omits it.
    @SerialName("bio_html") val bioHtml: String? = null
)

// GET /artists answers with "artists", GET /composers with "composers"; one class reads both.
@Serializable
data class PeopleRecognitionResponse(
    @SerialName("artists") val artists: List<ProfileExtrasDto> = emptyList(),
    @SerialName("composers") val composers: List<ProfileExtrasDto> = emptyList(),
    @SerialName("total") val total: Int = 0
)
