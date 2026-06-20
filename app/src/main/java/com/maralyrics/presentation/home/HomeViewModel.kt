package com.maralyrics.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.domain.model.*
import com.maralyrics.domain.usecase.*
import com.maralyrics.presentation.common.notification.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val searchSongsUseCase: SearchSongsUseCase,
    private val searchSongsWithFuzzyUseCase: SearchSongsWithFuzzyUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val getSearchInitializationStateUseCase: GetSearchInitializationStateUseCase,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SongSortOrder.NUMBER_ASC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _layoutType = MutableStateFlow(SongLayoutType.NUMBER_TITLE)
    val layoutType = _layoutType.asStateFlow()

    val searchInitializationState: StateFlow<SearchInitializationState> = getSearchInitializationStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchInitializationState.IDLE
        )

    private val settings = getSettingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSongs: StateFlow<List<Song>> = combine(
        settings.filterNotNull(),
        _sortOrder
    ) { s, sort ->
        val category = if (s.defaultCategory == "All") null else s.defaultCategory
        category to sort
    }.flatMapLatest { (category, sort) ->
        getAllSongsUseCase(category, sort)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResponse: StateFlow<SearchResponse> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchResponse.Empty)
            } else {
                val category = settings.value?.defaultCategory.let { if (it == "All") null else it }
                searchSongsWithFuzzyUseCase(query, category)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchResponse.Empty
        )

    // Keep searchResults for backward compatibility if needed in UI, 
    // but we'll transition to searchResponse
    val searchResults: StateFlow<List<Song>> = searchResponse.map { 
        if (it is SearchResponse.Results) it.songs else emptyList() 
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOrderChange(order: SongSortOrder) {
        _sortOrder.value = order
    }

    fun onLayoutTypeChange(type: SongLayoutType) {
        _layoutType.value = type
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            toggleFavoriteUseCase(songId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncDatabaseUseCase.checkAndSync()
            _isRefreshing.value = false
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            notificationManager.showInfo("Press back again to exit.")
        }
    }
}
