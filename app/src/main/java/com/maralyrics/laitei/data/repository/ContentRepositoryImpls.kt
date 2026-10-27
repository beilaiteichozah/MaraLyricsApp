package com.maralyrics.laitei.data.repository

import android.content.Context
import com.maralyrics.laitei.data.local.ContentCache
import com.maralyrics.laitei.data.mapper.toDomain
import com.maralyrics.laitei.data.remote.CalendarApiService
import com.maralyrics.laitei.data.remote.MaraLyricsApiService
import com.maralyrics.laitei.domain.model.*
import com.maralyrics.laitei.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongOfTheDayRepositoryImpl @Inject constructor(
    private val api: MaraLyricsApiService,
    private val songRepository: SongRepository,
    private val cache: ContentCache
) : SongOfTheDayRepository {

    @Serializable
    private data class SavedPick(val dayKey: Int, val songId: Long)

    override suspend fun getSongOfTheDay(): Song? {
        val today = EventMoment.today()
        // The server's pick is stable for the day, so ask once per day and reuse it afterwards.
        val saved = cache.readJson<SavedPick>(PICK_KEY)?.takeIf { it.dayKey == today.dayKey }
        val songId = saved?.songId ?: fetchTodaysPickId(today)
        return songId?.let { songRepository.getSongById(it) } ?: pickLocally(today)
    }

    private suspend fun fetchTodaysPickId(today: EventMoment): Long? {
        val id = try {
            api.getSongOfTheDay().takeIf { it.isSuccessful }?.body()?.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (id != null) cache.writeJson(PICK_KEY, SavedPick(today.dayKey, id))
        return id
    }

    /**
     * Offline (or not yet in the local library): rotate through the downloaded songs by day.
     * Deliberately not saved, so the server's real pick wins as soon as it can be fetched.
     */
    private suspend fun pickLocally(today: EventMoment): Song? {
        val songs = songRepository.getAllSongs(null, SongSortOrder.NUMBER_ASC).first()
        if (songs.isEmpty()) return null
        val dayNumber = EventMoment(1970, 1, 1).daysUntil(today)
        return songs[Math.floorMod(dayNumber, songs.size)]
    }

    private companion object {
        const val PICK_KEY = "song_of_the_day"
    }
}

@Singleton
class ArticleRepositoryImpl @Inject constructor(
    private val api: MaraLyricsApiService,
    private val cache: ContentCache
) : ArticleRepository {

    override suspend fun getArticles(page: Int): Result<ContentResult<ArticlePage>> =
        cache.networkFirst("articles_page_$page") { api.getArticles(page = page, limit = PAGE_SIZE) }
            .map { ContentResult(it.data.toDomain(), it.isFromCache) }

    override suspend fun getArticle(slug: String): Result<ContentResult<Article>> =
        cache.networkFirst("article_$slug") { api.getArticle(slug) }
            .map { ContentResult(it.data.toDomain(), it.isFromCache) }

    private companion object {
        const val PAGE_SIZE = 12
    }
}

@Singleton
class MaraIdolRepositoryImpl @Inject constructor(
    private val api: MaraLyricsApiService,
    private val cache: ContentCache
) : MaraIdolRepository {

    override suspend fun getSeasons(): Result<ContentResult<List<IdolSeason>>> =
        cache.networkFirst("mara_idol_seasons") { api.getMaraIdol() }
            .map { result ->
                val seasons = result.data.seasons.map { it.toDomain() }
                    .sortedWith(compareByDescending<IdolSeason> { it.year ?: Int.MIN_VALUE }.thenByDescending { it.startDate.orEmpty() })
                ContentResult(seasons, result.isFromCache)
            }

    override suspend fun getSeason(slug: String): Result<ContentResult<IdolSeason>> =
        cache.networkFirst("mara_idol_season_$slug") { api.getMaraIdolSeason(slug) }
            .map { ContentResult(it.data.toDomain(), it.isFromCache) }
}

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val api: CalendarApiService,
    private val cache: ContentCache,
    @ApplicationContext context: Context
) : CalendarRepository {

    private val prefs = context.getSharedPreferences("community_calendar", Context.MODE_PRIVATE)

    override suspend fun getCalendars(): Result<ContentResult<List<CommunityCalendar>>> =
        cache.networkFirst("calendars") { api.getCalendars() }
            .map { result -> ContentResult(result.data.calendars.map { it.toDomain() }, result.isFromCache) }

    override suspend fun getEvents(calendarSlug: String, year: Int): Result<ContentResult<List<CommunityEvent>>> =
        cache.networkFirst("calendar_events_${calendarSlug}_$year") { api.getEvents(year, calendarSlug) }
            .map { result ->
                val events = result.data.events
                    .mapNotNull { it.toDomain() }
                    .sortedWith(compareBy<CommunityEvent> { it.start.dayKey }.thenBy { it.title })
                ContentResult(events, result.isFromCache)
            }

    override fun getSelectedCalendarSlug(): String? = prefs.getString(KEY_SELECTED, null)

    override fun setSelectedCalendarSlug(slug: String) {
        prefs.edit().putString(KEY_SELECTED, slug).apply()
    }

    private companion object {
        const val KEY_SELECTED = "selected_calendar_slug"
    }
}
