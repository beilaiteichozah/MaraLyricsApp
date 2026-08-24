package com.maralyrics.laitei.presentation.song_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.Feedback
import com.maralyrics.laitei.domain.model.Song
import com.maralyrics.laitei.domain.repository.SettingsRepository
import com.maralyrics.laitei.domain.usecase.GetSettingsUseCase
import com.maralyrics.laitei.domain.usecase.GetSongDetailUseCase
import com.maralyrics.laitei.domain.usecase.SubmitFeedbackUseCase
import com.maralyrics.laitei.domain.usecase.ToggleFavoriteUseCase
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
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
    private val settingsRepository: SettingsRepository,
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

    private val _initialScrollPosition = MutableStateFlow(0)
    val initialScrollPosition: StateFlow<Int> = _initialScrollPosition.asStateFlow()

    private val _feedbackStatus = MutableStateFlow<FeedbackStatus?>(null)
    val feedbackStatus: StateFlow<FeedbackStatus?> = _feedbackStatus.asStateFlow()

    init {
        loadSong()
        loadSettings()
        loadSessionState()
    }

    private fun loadSessionState() {
        viewModelScope.launch {
            val settings = getSettingsUseCase().first()
            if (settings.resumeSessionEnabled) {
                val lastSongId = settingsRepository.getLastSongId().first()
                if (lastSongId == songId) {
                    _initialScrollPosition.value = settingsRepository.getSongScrollPosition().first()
                }
            }
            // Save this song as last viewed
            settingsRepository.saveLastSongId(songId)
        }
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
                _uiState.value = SongDetailUiState.Error
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value
        if (currentState is SongDetailUiState.Success) {
            viewModelScope.launch {
                val isFavorite = toggleFavoriteUseCase(songId)
                if (isFavorite) {
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
                notificationManager.showFeedbackSuccess()
            } else {
                notificationManager.showFeedbackSaved()
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

    fun onScrollChanged(position: Int) {
        viewModelScope.launch {
            settingsRepository.saveSongScrollPosition(position)
        }
    }
}

sealed interface SongDetailUiState {
    object Loading : SongDetailUiState
    data class Success(val song: Song) : SongDetailUiState
    object Error : SongDetailUiState
}

enum class FeedbackStatus {
    Success, SavedOffline
}
