package com.maralyrics.laitei.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.*
import com.maralyrics.laitei.domain.repository.SettingsRepository
import com.maralyrics.laitei.domain.usecase.*
import com.maralyrics.laitei.utils.StorageUtils
import com.maralyrics.laitei.presentation.common.notification.NotificationManager
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
    private val settingsRepository: SettingsRepository,
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

    private val _initialScrollState = MutableStateFlow(0 to 0)
    val initialScrollState = _initialScrollState.asStateFlow()

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
        val categories = if (s.defaultCategories.contains("All")) null else s.defaultCategories
        categories to sort
    }.flatMapLatest { (categories, sort) ->
        getAllSongsUseCase(categories, sort)
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
                searchSongsWithFuzzyUseCase(query, null)
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

    init {
        viewModelScope.launch {
            val settings = getSettingsUseCase().first()
            if (settings.resumeSessionEnabled) {
                _searchQuery.value = settingsRepository.getHomeSearchQuery().first()
                _sortOrder.value = settingsRepository.getHomeSortOrder().first()
                _layoutType.value = settingsRepository.getHomeLayoutType().first()
                _initialScrollState.value = settingsRepository.getHomeScrollState().first()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            settingsRepository.saveHomeSearchQuery(query)
        }
    }

    fun onSortOrderChange(order: SongSortOrder) {
        _sortOrder.value = order
        viewModelScope.launch {
            settingsRepository.saveHomeSortOrder(order)
        }
    }

    fun onLayoutTypeChange(type: SongLayoutType) {
        _layoutType.value = type
        viewModelScope.launch {
            settingsRepository.saveHomeLayoutType(type)
        }
    }

    fun onScrollStateChanged(index: Int, offset: Int) {
        viewModelScope.launch {
            settingsRepository.saveHomeScrollState(index, offset)
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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncDatabaseUseCase.checkAndSync().onFailure { exception ->
                if (exception is InsufficientStorageException) {
                    notificationManager.showStorageWarning(StorageUtils.getUsedStoragePercentage())
                }
            }
            _isRefreshing.value = false
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            notificationManager.showInfo("exit_press_back")
        }
    }
}
