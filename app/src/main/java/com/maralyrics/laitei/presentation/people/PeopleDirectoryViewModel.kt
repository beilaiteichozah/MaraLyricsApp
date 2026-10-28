package com.maralyrics.laitei.presentation.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.DirectoryPerson
import com.maralyrics.laitei.domain.model.PeopleDirectory
import com.maralyrics.laitei.domain.model.PeopleRole
import com.maralyrics.laitei.domain.model.PeopleSort
import com.maralyrics.laitei.domain.model.ProfileExtras
import com.maralyrics.laitei.domain.model.ProfileType
import com.maralyrics.laitei.domain.repository.ArtistRepository
import com.maralyrics.laitei.domain.repository.ComposerRepository
import com.maralyrics.laitei.domain.repository.ProfileExtrasRepository
import com.maralyrics.laitei.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PeopleUiState(
    val people: List<DirectoryPerson> = emptyList(),
    val query: String = "",
    val role: PeopleRole = PeopleRole.ALL,
    val sort: PeopleSort = PeopleSort.NAME_ASC,
    /** True until the downloaded library has been read. */
    val isLoading: Boolean = true,
    /** Whether anyone has a badge, so "Most badges" is only offered when it would do something. */
    val hasBadges: Boolean = false
)

@HiltViewModel
class PeopleDirectoryViewModel @Inject constructor(
    artistRepository: ArtistRepository,
    composerRepository: ComposerRepository,
    songRepository: SongRepository,
    private val profileExtrasRepository: ProfileExtrasRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val role = MutableStateFlow(PeopleRole.ALL)
    private val sort = MutableStateFlow(PeopleSort.NAME_ASC)

    // Badges and Green marks come from the website, so they arrive after the list itself, and
    // not at all when offline with nothing saved; the directory works without them.
    private val recognition = MutableStateFlow<Map<ProfileType, Map<String, ProfileExtras>>>(emptyMap())

    private val everyone = combine(
        artistRepository.getArtists(),
        composerRepository.getComposers(),
        songRepository.getArtistSongCounts(),
        songRepository.getComposerSongCounts(),
        recognition
    ) { artists, composers, artistCounts, composerCounts, extras ->
        val artistExtras = extras[ProfileType.ARTIST].orEmpty()
        val composerExtras = extras[ProfileType.COMPOSER].orEmpty()
        artists.map {
            DirectoryPerson(
                slug = it.slug,
                name = it.name,
                imageUrl = it.imageUrl,
                type = ProfileType.ARTIST,
                songCount = artistCounts[it.id] ?: 0,
                badges = artistExtras[it.slug]?.badges.orEmpty(),
                isGreen = artistExtras[it.slug]?.isGreen == true
            )
        } + composers.map {
            DirectoryPerson(
                slug = it.slug,
                name = it.name,
                imageUrl = it.imageUrl,
                type = ProfileType.COMPOSER,
                songCount = composerCounts[it.id] ?: 0,
                badges = composerExtras[it.slug]?.badges.orEmpty(),
                isGreen = composerExtras[it.slug]?.isGreen == true
            )
        }
    }

    val uiState: StateFlow<PeopleUiState> = combine(everyone, query, role, sort) { people, q, r, s ->
        PeopleUiState(
            people = PeopleDirectory.filterAndSort(people, q, r, s),
            query = q,
            role = r,
            sort = s,
            isLoading = false,
            hasBadges = people.any { it.badges.isNotEmpty() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleUiState())

    init {
        viewModelScope.launch {
            val artists = async { profileExtrasRepository.getRecognition(ProfileType.ARTIST) }
            val composers = async { profileExtrasRepository.getRecognition(ProfileType.COMPOSER) }
            recognition.value = mapOf(
                ProfileType.ARTIST to artists.await(),
                ProfileType.COMPOSER to composers.await()
            )
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onRoleChange(value: PeopleRole) {
        role.value = value
    }

    fun onSortChange(value: PeopleSort) {
        sort.value = value
    }

    fun resetFilters() {
        query.value = ""
        role.value = PeopleRole.ALL
    }
}
