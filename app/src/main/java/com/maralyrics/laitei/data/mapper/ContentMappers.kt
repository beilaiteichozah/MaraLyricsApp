package com.maralyrics.laitei.data.mapper

import com.maralyrics.laitei.data.remote.dto.*
import com.maralyrics.laitei.domain.model.*
import com.maralyrics.laitei.utils.parseServerTimestamp

private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

fun ArticleDto.toDomain() = Article(
    slug = slug,
    title = title,
    authorName = authorName.nullIfBlank(),
    summary = summary.nullIfBlank(),
    contentHtml = content.nullIfBlank(),
    publishedAt = parseServerTimestamp(publishedAt) ?: parseServerTimestamp(createdAt)
)

fun ArticlesResponse.toDomain() = ArticlePage(
    articles = articles.map { it.toDomain() },
    page = page,
    totalPages = totalPages
)

fun IdolContestantDto.toDomain() = IdolContestant(
    name = name,
    photoUrl = photoUrl.nullIfBlank(),
    result = IdolResult.fromApi(result),
    artistSlug = artistSlug.nullIfBlank(),
    summary = summary.nullIfBlank()
)

fun IdolSeasonDto.toDomain() = IdolSeason(
    slug = slug,
    title = title,
    year = year,
    venue = venue.nullIfBlank(),
    startDate = startDate.nullIfBlank(),
    endDate = endDate.nullIfBlank(),
    // The season banner is cover_url; photo_url is the fallback some seasons use instead.
    coverUrl = coverUrl.nullIfBlank() ?: photoUrl.nullIfBlank(),
    summary = summary.nullIfBlank(),
    description = description.nullIfBlank(),
    contestantCount = contestantCount,
    winners = winners.map { IdolWinner(name = it.name, photoUrl = it.photoUrl.nullIfBlank()) },
    videos = videos.map { IdolVideo(title = it.title.nullIfBlank(), url = it.url) },
    contestants = contestants.map { it.toDomain() }
)

fun CalendarDto.toDomain() = CommunityCalendar(
    slug = slug,
    name = name,
    description = description.nullIfBlank(),
    eventCount = eventCount
)

/** Null for an event whose start date can't be read — better to drop it than the whole year. */
fun CalendarEventDto.toDomain(): CommunityEvent? {
    val startMoment = EventMoment.parse(start) ?: return null
    val endMoment = EventMoment.parse(end)?.takeIf { it >= startMoment } ?: startMoment
    return CommunityEvent(
        id = id?.content ?: "$start|$title",
        title = title.trim(),
        description = description.nullIfBlank(),
        location = location.nullIfBlank(),
        start = startMoment,
        end = endMoment
    )
}
