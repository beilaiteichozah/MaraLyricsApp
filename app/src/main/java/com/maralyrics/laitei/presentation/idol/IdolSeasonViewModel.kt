package com.maralyrics.laitei.presentation.idol

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.IdolSeason
import com.maralyrics.laitei.domain.repository.ArtistRepository
import com.maralyrics.laitei.domain.repository.MaraIdolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface IdolSeasonUiState {
    data object Loading : IdolSeasonUiState
    data class Success(
        val season: IdolSeason,
        val isFromCache: Boolean,
        /** Artist slugs of contestants whose profile exists in the downloaded library. */
        val linkableArtistSlugs: Set<String>
    ) : IdolSeasonUiState
    data object Error : IdolSeasonUiState
}

@HiltViewModel
class IdolSeasonViewModel @Inject constructor(
    private val maraIdolRepository: MaraIdolRepository,
    private val artistRepository: ArtistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val seasonSlug: String = checkNotNull(savedStateHandle["season"])

    private val _uiState = MutableStateFlow<IdolSeasonUiState>(IdolSeasonUiState.Loading)
    val uiState: StateFlow<IdolSeasonUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = IdolSeasonUiState.Loading
            maraIdolRepository.getSeason(seasonSlug)
                .onSuccess { result ->
                    // A contestant may not have an artist page in this copy of the library
                    // (not downloaded yet, or not an artist on MaraLyrics) — only link the ones that do.
                    val linkable = result.data.contestants
                        .mapNotNull { it.artistSlug }
                        .filter { artistRepository.getArtistBySlug(it) != null }
                        .toSet()
                    _uiState.value = IdolSeasonUiState.Success(result.data, result.isFromCache, linkable)
                }
                .onFailure { _uiState.value = IdolSeasonUiState.Error }
        }
    }
}
