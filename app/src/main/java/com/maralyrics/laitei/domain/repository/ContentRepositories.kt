package com.maralyrics.laitei.domain.repository

import com.maralyrics.laitei.domain.model.*

/**
 * Today's featured song, as chosen by the website — resolved against the downloaded library so
 * it opens offline. Returns null only when there are no songs on the device at all.
 */
interface SongOfTheDayRepository {
    suspend fun getSongOfTheDay(): Song?
}

interface ArticleRepository {
    /** Page 1 falls back to the last saved copy offline; later pages need the network. */
    suspend fun getArticles(page: Int): Result<ContentResult<ArticlePage>>
    suspend fun getArticle(slug: String): Result<ContentResult<Article>>
}

interface MaraIdolRepository {
    suspend fun getSeasons(): Result<ContentResult<List<IdolSeason>>>
    suspend fun getSeason(slug: String): Result<ContentResult<IdolSeason>>
}

interface CalendarRepository {
    suspend fun getCalendars(): Result<ContentResult<List<CommunityCalendar>>>
    suspend fun getEvents(calendarSlug: String, year: Int): Result<ContentResult<List<CommunityEvent>>>

    /** The calendar the user last looked at, so the screen reopens where they left off. */
    fun getSelectedCalendarSlug(): String?
    fun setSelectedCalendarSlug(slug: String)
}
