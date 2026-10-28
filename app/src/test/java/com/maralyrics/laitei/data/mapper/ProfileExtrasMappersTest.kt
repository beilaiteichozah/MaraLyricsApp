package com.maralyrics.laitei.data.mapper

import com.maralyrics.laitei.data.remote.dto.PeopleRecognitionResponse
import com.maralyrics.laitei.data.remote.dto.ProfileExtrasDto
import com.maralyrics.laitei.domain.model.BadgePeriod
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Payloads are trimmed copies of GET /artists/{slug} and GET /artists from api.maralyrics.com. */
class ProfileExtrasMappersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `an artist with a badge and no Green mark`() {
        val body = """
            {"id":7,"name":"HC Teresa Ngiano","slug":"hc-teresa-ngiano","bio":"Winner.",
            "image_url":"data:image/jpeg;base64,AAAA","social_links":"[]","created_at":"2026-02-25 10:44:27",
            "badges":[{"period":"year","period_value":"2024","title":"2nd Runner-up of Mara Idol","created_at":"2026-10-10 09:29:20"}],
            "green":false,"bio_html":"<p>Winner.</p>","claimed":false,"songs":[]}
        """.trimIndent()

        val extras = json.decodeFromString<ProfileExtrasDto>(body).toDomain()

        assertFalse(extras.isGreen)
        assertEquals("<p>Winner.</p>", extras.bioHtml)
        val badge = extras.badges.single()
        assertEquals(BadgePeriod.YEAR, badge.period)
        assertEquals("2024", badge.periodValue)
        assertEquals("2nd Runner-up of Mara Idol", badge.title)
    }

    @Test
    fun `Green mark is read from a boolean, 1 or the word true`() {
        fun green(value: String) =
            json.decodeFromString<ProfileExtrasDto>("""{"slug":"a","green":$value}""").toDomain().isGreen

        assertTrue(green("true"))
        assertTrue(green("1"))
        assertTrue(green("\"true\""))
        assertFalse(green("false"))
        assertFalse(green("0"))
        assertFalse(green("null"))
        assertFalse(json.decodeFromString<ProfileExtrasDto>("""{"slug":"a"}""").toDomain().isGreen)
    }

    @Test
    fun `a badge date that arrives as a number is kept as text`() {
        val badge = json.decodeFromString<ProfileExtrasDto>(
            """{"slug":"a","badges":[{"period":"year","period_value":2025}]}"""
        ).toDomain().badges.single()

        assertEquals("2025", badge.periodValue)
        assertNull("no title sent, so the screen words it from the period", badge.title)
    }

    @Test
    fun `lifetime awards have no date and unknown periods are kept`() {
        val badges = json.decodeFromString<ProfileExtrasDto>(
            """{"slug":"a","badges":[{"period":"lifetime","period_value":null,"title":"  "},{"period":"decade"}]}"""
        ).toDomain().badges

        assertEquals(BadgePeriod.LIFETIME, badges[0].period)
        assertNull(badges[0].periodValue)
        assertNull("a blank title counts as none", badges[0].title)
        assertEquals(BadgePeriod.OTHER, badges[1].period)
    }

    @Test
    fun `a blank rich bio counts as no rich bio`() {
        val extras = json.decodeFromString<ProfileExtrasDto>("""{"slug":"a","bio_html":"   "}""").toDomain()

        assertNull(extras.bioHtml)
    }

    @Test
    fun `the list response is read for artists and for composers`() {
        val artists = json.decodeFromString<PeopleRecognitionResponse>(
            """{"artists":[{"slug":"a-chhualai","badges":[],"green":false,"song_count":9},{"slug":"x","green":true}],"total":2}"""
        )
        val composers = json.decodeFromString<PeopleRecognitionResponse>(
            """{"composers":[{"slug":"l-vabeiha","badges":[{"period":"lifetime"}]}],"total":1}"""
        )

        assertEquals(listOf("a-chhualai", "x"), artists.artists.map { it.slug })
        assertTrue(artists.artists[1].toDomain().isGreen)
        assertTrue(artists.composers.isEmpty())
        assertEquals(BadgePeriod.LIFETIME, composers.composers.single().toDomain().badges.single().period)
    }
}
