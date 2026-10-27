package com.maralyrics.laitei.presentation.song_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.Feedback
import com.maralyrics.laitei.domain.model.Song
import com.maralyrics.laitei.domain.model.SongNeighbors
import com.maralyrics.laitei.domain.model.SongSortOrder
import com.maralyrics.laitei.domain.repository.SettingsRepository
import com.maralyrics.laitei.domain.usecase.GetAllSongsUseCase
import com.maralyrics.laitei.domain.usecase.GetSettingsUseCase
import com.maralyrics.laitei.domain.usecase.GetSongDetailUseCase
import com.maralyrics.laitei.domain.usecase.SubmitFeedbackUseCase
import com.maralyrics.laitei.domain.usecase.ToggleFavoriteUseCase
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
import com.maralyrics.laitei.presentation.navigation.SongRoute
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
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val settingsRepository: SettingsRepository,
    private val notificationManager: NotificationManager,
    private val lyricsZoom: LyricsZoom,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Multiplier on the Settings font size; 1.0 is the default size. */
    val zoom: StateFlow<Float> = lyricsZoom.factor

    fun zoomIn() = lyricsZoom.zoomIn()
    fun zoomOut() = lyricsZoom.zoomOut()
    fun zoomBy(ratio: Float) = lyricsZoom.zoomBy(ratio)
    fun resetZoom() = lyricsZoom.reset()

    private val openedSongId: Long = checkNotNull(savedStateHandle[SongRoute.ARG_SONG_ID])

    // The song on screen. It starts as the one the route opened and moves with Previous/Next
    // without pushing a back-stack entry per song (back should leave the song view, not retrace
    // the paging). Kept in the SavedStateHandle so a restored process comes back to the song
    // that was being read rather than the one first opened.
    private val currentSongId: StateFlow<Long> =
        savedStateHandle.getStateFlow(KEY_CURRENT_SONG_ID, openedSongId)

    // The ids Previous/Next walk, in the order of the list the song was opened from. Empty when
    // it was opened without one (widget, notification, restored session) until the fallback in
    // init fills it with every song by number.
    private val order = MutableStateFlow(SongRoute.parseOrder(savedStateHandle[SongRoute.ARG_ORDER]))

    val neighbors: StateFlow<SongNeighbors?> = combine(currentSongId, order) { id, ids ->
        SongNeighbors.of(ids, id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Loading)
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    private val _fontSize = MutableStateFlow(18)
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    private val _lineSpacing = MutableStateFlow(1.5f)
    val lineSpacing: StateFlow<Float> = _lineSpacing.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(false)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _initialScrollPosition = MutableStateFlow(0)
    val initialScrollPosition: StateFlow<Int> = _initialScrollPosition.asStateFlow()

    private val _feedbackStatus = MutableStateFlow<FeedbackStatus?>(null)
    val feedbackStatus: StateFlow<FeedbackStatus?> = _feedbackStatus.asStateFlow()

    init {
        loadSettings()
        loadSessionState()
        viewModelScope.launch {
            currentSongId.collectLatest { loadSong(it) }
        }
        viewModelScope.launch {
            if (SongNeighbors.of(order.value, currentSongId.value) == null) {
                order.value = getAllSongsUseCase(null, SongSortOrder.NUMBER_ASC).first().map { it.id }
            }
        }
    }

    private fun loadSessionState() {
        viewModelScope.launch {
            val songId = currentSongId.value
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
                _keepScreenOn.value = settings.keepScreenOnEnabled
            }
        }
    }

    // Paging deliberately keeps showing the previous song until the next one is ready instead of
    // flashing the loading spinner between every pair of songs.
    private suspend fun loadSong(songId: Long) {
        val song = getSongDetailUseCase(songId)
        _uiState.value = if (song != null) SongDetailUiState.Success(song) else SongDetailUiState.Error
    }

    fun openPrevious() {
        SongNeighbors.of(order.value, currentSongId.value)?.previousId?.let(::openSong)
    }

    fun openNext() {
        SongNeighbors.of(order.value, currentSongId.value)?.nextId?.let(::openSong)
    }

    private fun openSong(songId: Long) {
        savedStateHandle[KEY_CURRENT_SONG_ID] = songId
        // The resume offset was for the song first opened; re-applying it to this one (say, after a
        // rotation rebuilds the screen) would scroll it somewhere arbitrary.
        _initialScrollPosition.value = 0
        viewModelScope.launch {
            settingsRepository.saveLastSongId(songId)
            // The saved scroll offset belongs to the song just left; this one opens at the top.
            settingsRepository.saveSongScrollPosition(0)
        }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value
        if (currentState is SongDetailUiState.Success) {
            viewModelScope.launch {
                val songId = currentSongId.value
                val isFavorite = toggleFavoriteUseCase(songId)
                if (isFavorite) {
                    notificationManager.showFavoriteAdded()
                } else {
                    notificationManager.showFavoriteRemoved()
                }
                // Reload to get updated favorite status
                loadSong(songId)
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

    private companion object {
        const val KEY_CURRENT_SONG_ID = "current_song_id"
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
