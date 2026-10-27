package com.maralyrics.laitei.data.mapper

import com.maralyrics.laitei.data.remote.dto.ArticleDto
import com.maralyrics.laitei.data.remote.dto.ArticlesResponse
import com.maralyrics.laitei.data.remote.dto.CalendarEventsResponse
import com.maralyrics.laitei.data.remote.dto.IdolIndexResponse
import com.maralyrics.laitei.data.remote.dto.IdolSeasonDto
import com.maralyrics.laitei.domain.model.EventMoment
import com.maralyrics.laitei.domain.model.IdolResult
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Payloads below are trimmed copies of what api.maralyrics.com and calendar-api.marareih.org
 * actually return, decoded with the same Json settings the app uses (NetworkModule).
 */
class ContentMappersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // --- Articles ---------------------------------------------------------------------------

    @Test
    fun `articles list decodes and maps, including the server timestamp`() {
        val body = """
            {"articles":[{"id":2,"title":"📢 Updated Terms & Privacy Policy","slug":"updated-terms-privacy-policy",
            "author_name":"Mara Lyrics Team","summary":"Terms & Conditions and Privacy Policy of MaraLyrics has been updated",
            "published_at":"2026-09-21 08:32:49","created_at":"2026-09-21 08:32:41"}],
            "total":1,"page":1,"totalPages":1,"sort":"published_desc"}
        """.trimIndent()

        val page = json.decodeFromString<ArticlesResponse>(body).toDomain()

        assertEquals(1, page.articles.size)
        val article = page.articles.single()
        assertEquals("updated-terms-privacy-policy", article.slug)
        assertEquals("Mara Lyrics Team", article.authorName)
        assertEquals(1789979569000L, article.publishedAt) // 2026-09-21 08:32:49 UTC
        assertNull("list endpoint carries no body", article.contentHtml)
        assertFalse(page.hasMore)
    }

    @Test
    fun `article without published_at falls back to created_at, and blanks become null`() {
        val body = """{"slug":"s","title":"T","author_name":"  ","summary":"","content":"<p>Hi</p>",
            "published_at":null,"created_at":"2026-09-21 08:32:41"}"""

        val article = json.decodeFromString<ArticleDto>(body).toDomain()

        assertNull(article.authorName)
        assertNull(article.summary)
        assertEquals("<p>Hi</p>", article.contentHtml)
        assertNotNull(article.publishedAt)
    }

    @Test
    fun `article page reports more pages while page is below totalPages`() {
        val body = """{"articles":[],"total":30,"page":1,"totalPages":3}"""
        assertTrue(json.decodeFromString<ArticlesResponse>(body).toDomain().hasMore)
    }

    // --- Mara Idol --------------------------------------------------------------------------

    @Test
    fun `idol index ignores the duplicated idols list and maps season fields`() {
        val body = """
            {"seasons":[{"title":"Mara Idol Season 7","slug":"mara-idol-season-7","year":2024,"venue":"Siaha",
            "start_date":"2024-10-23","end_date":"2024-12-03","cover_url":"data:image/jpeg;base64,AAAA","photo_url":null,
            "contestant_count":3,"summary":"A summary",
            "winners":[{"name":"HC Teresa Ngiano","slug":"x","photo_url":"data:image/jpeg;base64,BBBB"}]}],
            "idols":[{"name":"HC Teresa Ngiano","result":"winner","season_slug":"mara-idol-season-7"}],"total":3}
        """.trimIndent()

        val season = json.decodeFromString<IdolIndexResponse>(body).seasons.single().toDomain()

        assertEquals("Mara Idol Season 7", season.title)
        assertEquals(2024, season.year)
        assertEquals("Siaha", season.venue)
        assertEquals("data:image/jpeg;base64,AAAA", season.coverUrl)
        assertEquals(3, season.contestantCount)
        assertEquals(listOf("HC Teresa Ngiano"), season.winners.map { it.name })
        assertTrue(season.contestants.isEmpty())
        assertTrue(season.videos.isEmpty())
    }

    @Test
    fun `season detail maps videos and contestants with results`() {
        val body = """
            {"title":"Season","slug":"s","year":2024,"description":"Longer text","cover_url":null,"photo_url":"https://x/p.jpg",
            "videos":[{"title":"Round 1","url":"https://youtu.be/a"},{"url":"https://youtu.be/b"}],
            "contestants":[
              {"name":"A","slug":"a","photo_url":null,"result":"winner","placement":null,"artist_slug":"a-artist","summary":"S"},
              {"name":"B","slug":"b","result":"runner_up","artist_slug":""},
              {"name":"C","slug":"c","result":"something_new"},
              {"name":"D","slug":"d","result":null}
            ]}
        """.trimIndent()

        val season = json.decodeFromString<IdolSeasonDto>(body).toDomain()

        assertEquals("cover falls back to photo_url", "https://x/p.jpg", season.coverUrl)
        assertEquals("Longer text", season.description)
        assertEquals(listOf("Round 1", null), season.videos.map { it.title })
        assertEquals(
            listOf(IdolResult.WINNER, IdolResult.RUNNER_UP, IdolResult.CONTESTANT, IdolResult.CONTESTANT),
            season.contestants.map { it.result }
        )
        assertEquals("a-artist", season.contestants[0].artistSlug)
        assertNull("blank artist_slug means no profile link", season.contestants[1].artistSlug)
    }

    @Test
    fun `every documented result string maps to its enum value`() {
        assertEquals(IdolResult.WINNER, IdolResult.fromApi("winner"))
        assertEquals(IdolResult.RUNNER_UP, IdolResult.fromApi("runner_up"))
        assertEquals(IdolResult.SECOND_RUNNER_UP, IdolResult.fromApi("second_runner_up"))
        assertEquals(IdolResult.FINALIST, IdolResult.fromApi("finalist"))
        assertEquals(IdolResult.SEMI_FINALIST, IdolResult.fromApi("semi_finalist"))
        assertEquals(IdolResult.CONTESTANT, IdolResult.fromApi("contestant"))
        assertEquals(IdolResult.WINNER, IdolResult.fromApi(" Winner "))
    }

    // --- Calendar ---------------------------------------------------------------------------

    @Test
    fun `calendar events decode and map`() {
        val body = """
            {"year":2026,"events":[
              {"id":"5253950a-a159","start":"2026-01-01","end":"2026-01-03","title":"New Year Holiday","description":"ECM Assembly Office Holiday","location":""},
              {"id":"2112f78e-4202","start":"2026-01-01","end":"2026-01-01","title":"Kô Thiehpa Noh","description":"Pohbiehnoh","location":"Mararâh"}
            ]}
        """.trimIndent()

        val events = json.decodeFromString<CalendarEventsResponse>(body).events.mapNotNull { it.toDomain() }

        assertEquals(2, events.size)
        assertEquals("5253950a-a159", events[0].id)
        assertEquals(EventMoment(2026, 1, 1), events[0].start)
        assertEquals(EventMoment(2026, 1, 3), events[0].end)
        assertNull("empty location becomes null", events[0].location)
        assertEquals("Mararâh", events[1].location)
        assertEquals("Kô Thiehpa Noh", events[1].title)
    }

    @Test
    fun `numeric event ids from the third-party service do not break decoding`() {
        val body = """{"events":[{"id":42,"start":"2026-05-05","title":"Numeric id"}]}"""
        val event = json.decodeFromString<CalendarEventsResponse>(body).events.single().toDomain()
        assertEquals("42", event?.id)
    }

    @Test
    fun `event with an unreadable start is dropped rather than failing the list`() {
        val body = """{"events":[
            {"id":"a","start":"garbage","title":"Bad"},
            {"id":"b","start":"2026-05-05","title":"Good"}]}"""
        val events = json.decodeFromString<CalendarEventsResponse>(body).events.mapNotNull { it.toDomain() }
        assertEquals(listOf("Good"), events.map { it.title })
    }

    @Test
    fun `missing or inverted end date collapses to the start day`() {
        val body = """{"events":[
            {"id":"a","start":"2026-05-05","title":"No end"},
            {"id":"b","start":"2026-05-05","end":"2026-05-01","title":"Inverted"}]}"""
        val events = json.decodeFromString<CalendarEventsResponse>(body).events.mapNotNull { it.toDomain() }
        assertTrue(events.none { it.isMultiDay })
        assertEquals(EventMoment(2026, 5, 5), events[0].end)
        assertEquals(EventMoment(2026, 5, 5), events[1].end)
    }
}
