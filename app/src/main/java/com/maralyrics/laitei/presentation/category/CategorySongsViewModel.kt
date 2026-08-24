package com.maralyrics.laitei.presentation.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.Song
import com.maralyrics.laitei.domain.model.SongLayoutType
import com.maralyrics.laitei.domain.model.SongSortOrder
import com.maralyrics.laitei.domain.usecase.GetAllSongsUseCase
import com.maralyrics.laitei.domain.usecase.ToggleFavoriteUseCase
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategorySongsViewModel @Inject constructor(
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val notificationManager: NotificationManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryKey: String = checkNotNull(savedStateHandle["category"])
    val category = categoryKey

    private val _sortOrder = MutableStateFlow(SongSortOrder.NUMBER_ASC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _layoutType = MutableStateFlow(SongLayoutType.NUMBER_TITLE)
    val layoutType = _layoutType.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val songs: StateFlow<List<Song>> = _sortOrder
        .flatMapLatest { sort ->
            getAllSongsUseCase(listOf(categoryKey), sort)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = songs.map { it.isEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onSortOrderChange(order: SongSortOrder) {
        _sortOrder.value = order
    }

    fun onLayoutTypeChange(type: SongLayoutType) {
        _layoutType.value = type
    }


    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            val isFavorite = toggleFavoriteUseCase(songId)
            if (isFavorite) {
                notificationManager.showFavoriteAdded()
            } else {
                notificationManager.showFavoriteRemoved()
            }
        }
    }
}
