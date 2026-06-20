package com.maralyrics.presentation.song_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.Feedback
import com.maralyrics.domain.model.Song
import com.maralyrics.domain.usecase.GetSettingsUseCase
import com.maralyrics.domain.usecase.GetSongDetailUseCase
import com.maralyrics.domain.usecase.SubmitFeedbackUseCase
import com.maralyrics.domain.usecase.ToggleFavoriteUseCase
import com.maralyrics.presentation.common.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongDetailViewModel @Inject constructor(
    private val getSongDetailUseCase: GetSongDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val submitFeedbackUseCase: SubmitFeedbackUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val notificationManager: NotificationManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val songId: Long = checkNotNull(savedStateHandle["songId"])

    private val _uiState = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Loading)
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    private val _fontSize = MutableStateFlow(18)
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    private val _lineSpacing = MutableStateFlow(1.5f)
    val lineSpacing: StateFlow<Float> = _lineSpacing.asStateFlow()

    private val _feedbackStatus = MutableStateFlow<FeedbackStatus?>(null)
    val feedbackStatus: StateFlow<FeedbackStatus?> = _feedbackStatus.asStateFlow()

    init {
        loadSong()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collectLatest { settings ->
                _fontSize.value = settings.defaultFontSize
                _lineSpacing.value = settings.lineSpacing
            }
        }
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
                val isFav = !currentState.song.isFavorite
                if (isFav) {
                    notificationManager.showFavoriteAdded()
                } else {
                    notificationManager.showFavoriteRemoved()
                }
                // Reload to get updated favorite status
                loadSong()
            }
        }
    }

    fun submitFeedback(name: String, email: String, message: String) {
        val song = (uiState.value as? SongDetailUiState.Success)?.song ?: return
        viewModelScope.launch {
            val feedback = Feedback(
                songId = song.id,
                songSlug = song.slug,
                songTitle = song.title,
                artistName = song.artistName,
                name = name,
                email = email,
                message = message
            )
            val result = submitFeedbackUseCase(feedback)
            if (result.isSuccess) {
                notificationManager.showSuccess("Report submitted successfully.")
            } else {
                notificationManager.showInfo("Report saved. It will be sent automatically when you're online.")
            }
        }
    }

    fun clearFeedbackStatus() {
        _feedbackStatus.value = null
    }

    fun onLyricsCopied() {
        viewModelScope.launch {
            notificationManager.showCopied()
        }
    }
}

sealed interface SongDetailUiState {
    object Loading : SongDetailUiState
    data class Success(val song: Song) : SongDetailUiState
    data class Error(val message: String) : SongDetailUiState
}

enum class FeedbackStatus {
    Success, SavedOffline
}
