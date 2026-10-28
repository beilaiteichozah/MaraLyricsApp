package com.maralyrics.laitei.domain.model

import com.maralyrics.laitei.utils.MaraAlphabetUtils
import com.maralyrics.laitei.utils.SearchUtils

/** One row of the Artists & Composers directory: a person in one role, with what the website knows of them. */
data class DirectoryPerson(
    val slug: String,
    val name: String,
    val imageUrl: String?,
    val type: ProfileType,
    val songCount: Int,
    val badges: List<ProfileBadge> = emptyList(),
    val isGreen: Boolean = false
)

enum class PeopleRole { ALL, ARTISTS, COMPOSERS }

enum class PeopleSort { NAME_ASC, NAME_DESC, SONGS_DESC, SONGS_ASC, BADGES_DESC }

object PeopleDirectory {

    /**
     * Applies the directory's search box, role filter and sort. Search ignores case and accents
     * ("ngiano" finds "Ngianô") and matches any word of the name; names sort in Mara alphabet
     * order, as the song lists do.
     */
    fun filterAndSort(
        people: List<DirectoryPerson>,
        query: String,
        role: PeopleRole,
        sort: PeopleSort
    ): List<DirectoryPerson> {
        val wanted = SearchUtils.normalize(query)
        val byName = Comparator<DirectoryPerson> { a, b -> MaraAlphabetUtils.compare(a.name, b.name) }

        val sorted = when (sort) {
            PeopleSort.NAME_ASC -> byName
            PeopleSort.NAME_DESC -> byName.reversed()
            PeopleSort.SONGS_DESC -> compareByDescending<DirectoryPerson> { it.songCount }.then(byName)
            PeopleSort.SONGS_ASC -> compareBy<DirectoryPerson> { it.songCount }.then(byName)
            PeopleSort.BADGES_DESC -> compareByDescending<DirectoryPerson> { it.badges.size }
                .thenByDescending { it.isGreen }
                .then(byName)
        }

        return people
            .filter { person ->
                when (role) {
                    PeopleRole.ALL -> true
                    PeopleRole.ARTISTS -> person.type == ProfileType.ARTIST
                    PeopleRole.COMPOSERS -> person.type == ProfileType.COMPOSER
                }
            }
            .filter { wanted.isEmpty() || SearchUtils.normalize(it.name).contains(wanted) }
            .sortedWith(sorted)
    }
}
