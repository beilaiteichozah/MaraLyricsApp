package com.maralyrics.laitei.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleDirectoryTest {

    private fun person(
        name: String,
        type: ProfileType = ProfileType.ARTIST,
        songs: Int = 0,
        badges: Int = 0,
        green: Boolean = false
    ) = DirectoryPerson(
        slug = name.lowercase().replace(' ', '-'),
        name = name,
        imageUrl = null,
        type = type,
        songCount = songs,
        badges = List(badges) { ProfileBadge(BadgePeriod.YEAR, "2024", null) },
        isGreen = green
    )

    private val people = listOf(
        person("L Vabeiha", ProfileType.ARTIST, songs = 5),
        person("L Vabeiha", ProfileType.COMPOSER, songs = 12),
        person("HC Teresa Ngianô", ProfileType.ARTIST, songs = 0, badges = 2),
        person("A Chhualai", ProfileType.ARTIST, songs = 9, badges = 1, green = true),
        person("Zakhu", ProfileType.COMPOSER, songs = 3)
    )

    private fun names(list: List<DirectoryPerson>) = list.map { "${it.name}/${it.type.name.first()}" }

    @Test
    fun `everyone is listed A to Z by default`() {
        val result = PeopleDirectory.filterAndSort(people, "", PeopleRole.ALL, PeopleSort.NAME_ASC)

        assertEquals(
            listOf("A Chhualai/A", "HC Teresa Ngianô/A", "L Vabeiha/A", "L Vabeiha/C", "Zakhu/C"),
            names(result)
        )
    }

    @Test
    fun `Z to A reverses the names`() {
        val result = PeopleDirectory.filterAndSort(people, "", PeopleRole.ALL, PeopleSort.NAME_DESC)

        assertEquals("Zakhu", result.first().name)
        assertEquals("A Chhualai", result.last().name)
    }

    @Test
    fun `role filter keeps only artists or only composers`() {
        val artists = PeopleDirectory.filterAndSort(people, "", PeopleRole.ARTISTS, PeopleSort.NAME_ASC)
        val composers = PeopleDirectory.filterAndSort(people, "", PeopleRole.COMPOSERS, PeopleSort.NAME_ASC)

        assertTrue(artists.all { it.type == ProfileType.ARTIST })
        assertEquals(3, artists.size)
        assertTrue(composers.all { it.type == ProfileType.COMPOSER })
        assertEquals(2, composers.size)
    }

    @Test
    fun `search ignores case and accents and matches any word`() {
        assertEquals(listOf("HC Teresa Ngianô"), PeopleDirectory.filterAndSort(people, "ngiano", PeopleRole.ALL, PeopleSort.NAME_ASC).map { it.name })
        assertEquals(listOf("HC Teresa Ngianô"), PeopleDirectory.filterAndSort(people, "  TERESA ", PeopleRole.ALL, PeopleSort.NAME_ASC).map { it.name })
        assertEquals(2, PeopleDirectory.filterAndSort(people, "vabeiha", PeopleRole.ALL, PeopleSort.NAME_ASC).size)
        assertTrue(PeopleDirectory.filterAndSort(people, "nobody", PeopleRole.ALL, PeopleSort.NAME_ASC).isEmpty())
    }

    @Test
    fun `search and role filter combine`() {
        val result = PeopleDirectory.filterAndSort(people, "vabeiha", PeopleRole.COMPOSERS, PeopleSort.NAME_ASC)

        assertEquals(listOf("L Vabeiha/C"), names(result))
    }

    @Test
    fun `most songs first, ties broken by name`() {
        val result = PeopleDirectory.filterAndSort(people, "", PeopleRole.ALL, PeopleSort.SONGS_DESC)

        assertEquals(listOf(12, 9, 5, 3, 0), result.map { it.songCount })
    }

    @Test
    fun `fewest songs first`() {
        val result = PeopleDirectory.filterAndSort(people, "", PeopleRole.ALL, PeopleSort.SONGS_ASC)

        assertEquals(listOf(0, 3, 5, 9, 12), result.map { it.songCount })
    }

    @Test
    fun `most badges first, then Green marks, then name`() {
        val result = PeopleDirectory.filterAndSort(people, "", PeopleRole.ALL, PeopleSort.BADGES_DESC)

        assertEquals("HC Teresa Ngianô", result[0].name)
        assertEquals("A Chhualai", result[1].name)
        assertEquals(listOf(2, 1, 0, 0, 0), result.map { it.badges.size })
    }

    @Test
    fun `an empty directory stays empty`() {
        assertTrue(PeopleDirectory.filterAndSort(emptyList(), "x", PeopleRole.ALL, PeopleSort.NAME_ASC).isEmpty())
    }
}
