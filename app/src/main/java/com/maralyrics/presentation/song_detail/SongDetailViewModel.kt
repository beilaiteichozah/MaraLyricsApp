package com.maralyrics.presentation.song_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.Song
import com.maralyrics.domain.usecase.GetSongDetailUseCase
import com.maralyrics.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongDetailViewModel @Inject constructor(
    private val getSongDetailUseCase: GetSongDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val songId: Long = checkNotNull(savedStateHandle["songId"])

    private val _uiState = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Loading)
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    private val _fontSize = MutableStateFlow(18)
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    init {
        loadSong()
    }

    private fun loadSong() {
        viewModelScope.launch {
            _uiState.value = SongDetailUiState.Loading
            val song = getSongDetailUseCase(songId)
            if (song != null) {
                _uiState.value = SongDetailUiState.Success(song)
            } else {
                _uiState.value = SongDetailUiState.Error("Song not found")
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value
        if (currentState is SongDetailUiState.Success) {
            viewModelScope.launch {
                toggleFavoriteUseCase(songId)
                // Reload to get updated favorite status
                loadSong()
            }
        }
    }

    fun increaseFontSize() {
        if (_fontSize.value < 40) {
            _fontSize.value += 2
        }
    }

    fun decreaseFontSize() {
        if (_fontSize.value > 12) {
            _fontSize.value -= 2
        }
    }
}

sealed interface SongDetailUiState {
    object Loading : SongDetailUiState
    data class Success(val song: Song) : SongDetailUiState
    data class Error(val message: String) : SongDetailUiState
}
