package com.maralyrics.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.FavoriteStats
import com.maralyrics.domain.model.Song
import com.maralyrics.domain.model.SongLayoutType
import com.maralyrics.domain.model.SongSortOrder
import com.maralyrics.domain.repository.SettingsRepository
import com.maralyrics.domain.usecase.*
import com.maralyrics.presentation.common.notification.NotificationManager
import com.maralyrics.utils.SearchUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteSongsViewModel @Inject constructor(
    private val getFavoriteSongsUseCase: GetFavoriteSongsUseCase,
    private val getFavoriteStatsUseCase: GetFavoriteStatsUseCase,
    private val getFavoriteCategoriesUseCase: GetFavoriteCategoriesUseCase,
    private val getSurpriseSongUseCase: GetSurpriseSongUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _sortOrder = MutableStateFlow(SongSortOrder.RECENTLY_ADDED)
    val sortOrder = _sortOrder.asStateFlow()

    private val _layoutType = MutableStateFlow(SongLayoutType.NUMBER_TITLE)
    val layoutType = _layoutType.asStateFlow()

    private val _initialScrollState = MutableStateFlow(0 to 0)
    val initialScrollState = _initialScrollState.asStateFlow()

    val stats: StateFlow<FavoriteStats> = getFavoriteStatsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FavoriteStats(0, 0, 0))

    val availableCategories: StateFlow<List<String>> = getFavoriteCategoriesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val settings = getSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val songs: StateFlow<List<Song>> = combine(
        getFavoriteSongsUseCase(),
        _searchQuery,
        _selectedCategory,
        _sortOrder
    ) { allFavorites, query, category, sort ->
        var filtered = allFavorites

        // Filter by category
        if (category != null) {
            filtered = filtered.filter { it.category == category }
        }

        // Filter by search query
        if (query.isNotBlank()) {
            val normalizedQuery = SearchUtils.normalize(query)
            filtered = filtered.filter {
                SearchUtils.normalize(it.title).contains(normalizedQuery) ||
                (it.artistName != null && SearchUtils.normalize(it.artistName).contains(normalizedQuery)) ||
                (it.composerName != null && SearchUtils.normalize(it.composerName).contains(normalizedQuery))
            }
        }

        // Sort
        when (sort) {
            SongSortOrder.NUMBER_ASC -> filtered.sortedBy { it.id }
            SongSortOrder.NUMBER_DESC -> filtered.sortedByDescending { it.id }
            SongSortOrder.A_Z -> filtered.sortedBy { it.title } // Ideally use MaraAlphabetUtils.compare
            SongSortOrder.Z_A -> filtered.sortedByDescending { it.title }
            SongSortOrder.NEWEST -> filtered.sortedByDescending { it.createdAt }
            SongSortOrder.OLDEST -> filtered.sortedBy { it.createdAt }
            SongSortOrder.RECENTLY_ADDED -> filtered // Already sorted by favorited_at from DAO
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val settings = getSettingsUseCase().first()
            if (settings.resumeSessionEnabled) {
                _searchQuery.value = settingsRepository.getFavoriteSearchQuery().first()
                _sortOrder.value = settingsRepository.getFavoriteSortOrder().first()
                _selectedCategory.value = settingsRepository.getFavoriteCategoryFilter().first()
                _layoutType.value = settingsRepository.getFavoriteLayoutType().first()
                _initialScrollState.value = settingsRepository.getFavoriteScrollState().first()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            settingsRepository.saveFavoriteSearchQuery(query)
        }
    }

    fun onCategorySelect(category: String?) {
        val next = if (_selectedCategory.value == category) null else category
        _selectedCategory.value = next
        viewModelScope.launch {
            settingsRepository.saveFavoriteCategoryFilter(next)
        }
    }

    fun onSortOrderChange(order: SongSortOrder) {
        _sortOrder.value = order
        viewModelScope.launch {
            settingsRepository.saveFavoriteSortOrder(order)
        }
    }

    fun onLayoutTypeChange(type: SongLayoutType) {
        _layoutType.value = type
        viewModelScope.launch {
            settingsRepository.saveFavoriteLayoutType(type)
        }
    }

    fun onScrollStateChanged(index: Int, offset: Int) {
        viewModelScope.launch {
            settingsRepository.saveFavoriteScrollState(index, offset)
        }
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

    fun getSurpriseSong(onSongSelected: (Long) -> Unit) {
        viewModelScope.launch {
            getSurpriseSongUseCase()?.let {
                onSongSelected(it.id)
            }
        }
    }
}
