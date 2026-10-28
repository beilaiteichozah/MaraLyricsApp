package com.maralyrics.laitei.data.mapper

import com.maralyrics.laitei.data.remote.dto.SupportersResponse
import com.maralyrics.laitei.data.remote.dto.TeamResponse
import com.maralyrics.laitei.domain.model.TeamRole
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Payloads are trimmed copies of GET /supporters and GET /team from api.maralyrics.com. */
class CreditsExtrasMappersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `a partner decodes with its logo and link`() {
        val body = """
            {"sponsors":[],"partners":[{"kind":"partner","name":"Mara Language Preservation",
            "description":"MLP preserves and promotes the Mara language.","logo_url":"data:image/jpeg;base64,AAAA",
            "website_url":"https://example.org/mlp","sort_order":1}]}
        """.trimIndent()

        val response = json.decodeFromString<SupportersResponse>(body)

        assertEquals(emptyList<Any>(), response.sponsors)
        val partner = response.partners.single().toDomain()!!
        assertEquals("Mara Language Preservation", partner.name)
        assertEquals("MLP preserves and promotes the Mara language.", partner.description)
        assertEquals("data:image/jpeg;base64,AAAA", partner.logoUrl)
        assertEquals("https://example.org/mlp", partner.websiteUrl)
    }

    @Test
    fun `only plain web links are kept, since they get opened`() {
        assertEquals("https://example.org", "https://example.org".asWebUrl())
        assertEquals("http://example.org/a?b=1", "  http://example.org/a?b=1 ".asWebUrl())
        assertEquals("HTTPS://EXAMPLE.ORG", "HTTPS://EXAMPLE.ORG".asWebUrl())

        assertNull("javascript:alert(1)".asWebUrl())
        assertNull("intent://scan/#Intent;scheme=zxing;end".asWebUrl())
        assertNull("file:///sdcard/x".asWebUrl())
        assertNull("content://contacts/people".asWebUrl())
        assertNull("//example.org".asWebUrl())
        assertNull("example.org".asWebUrl())
        assertNull("https://".asWebUrl())
        assertNull("   ".asWebUrl())
        assertNull(null.asWebUrl())
    }

    @Test
    fun `a supporter with an unsafe link is still listed, just not clickable`() {
        val supporter = json.decodeFromString<SupportersResponse>(
            """{"sponsors":[{"name":"Friend","website_url":"javascript:alert(1)"}]}"""
        ).sponsors.single().toDomain()!!

        assertEquals("Friend", supporter.name)
        assertNull(supporter.websiteUrl)
        assertNull(supporter.description)
        assertNull(supporter.logoUrl)
    }

    @Test
    fun `entries without a name are dropped`() {
        val response = json.decodeFromString<SupportersResponse>(
            """{"sponsors":[{"name":"  "},{"description":"no name"},{"name":"Real"}]}"""
        )

        assertEquals(listOf("Real"), response.sponsors.mapNotNull { it.toDomain()?.name })
    }

    @Test
    fun `team members are read with their role and photo`() {
        val body = """
            {"team":[{"username":"laitei","role":"super_admin","avatar":"⭐","photo":"data:image/jpeg;base64,BBBB"},
            {"username":"editor-one","role":"editor","photo":null},{"username":"someone","role":"translator"}]}
        """.trimIndent()

        val team = json.decodeFromString<TeamResponse>(body).team.mapNotNull { it.toDomain() }

        assertEquals(listOf("laitei", "editor-one", "someone"), team.map { it.name })
        assertEquals(listOf(TeamRole.SUPER_ADMIN, TeamRole.EDITOR, TeamRole.OTHER), team.map { it.role })
        assertEquals("data:image/jpeg;base64,BBBB", team[0].photoUrl)
        assertNull(team[1].photoUrl)
    }
}
