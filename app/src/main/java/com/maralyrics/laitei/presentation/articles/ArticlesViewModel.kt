package com.maralyrics.laitei.presentation.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.Article
import com.maralyrics.laitei.domain.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticlesUiState(
    val articles: List<Article> = emptyList(),
    /** First page in flight with nothing on screen yet. */
    val isLoading: Boolean = true,
    /** Pull-to-refresh in flight over an existing list. */
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    /** First page failed and there is nothing saved to show instead. */
    val loadFailed: Boolean = false,
    val loadMoreFailed: Boolean = false,
    val isFromCache: Boolean = false
)

@HiltViewModel
class ArticlesViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ArticlesUiState())
    val state: StateFlow<ArticlesUiState> = _state.asStateFlow()

    private var nextPage = 1
    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = it.articles.isEmpty(),
                    isRefreshing = it.articles.isNotEmpty(),
                    loadFailed = false,
                    loadMoreFailed = false,
                    isLoadingMore = false
                )
            }
            articleRepository.getArticles(page = 1)
                .onSuccess { result ->
                    nextPage = 2
                    _state.value = ArticlesUiState(
                        articles = result.data.articles,
                        isLoading = false,
                        hasMore = result.data.hasMore,
                        isFromCache = result.isFromCache
                    )
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, isRefreshing = false, loadFailed = it.articles.isEmpty()) }
                }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.isLoading || current.isRefreshing || current.isLoadingMore) return

        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
            articleRepository.getArticles(page = nextPage)
                .onSuccess { result ->
                    nextPage++
                    _state.update { state ->
                        val known = state.articles.mapTo(HashSet()) { it.slug }
                        state.copy(
                            articles = state.articles + result.data.articles.filter { it.slug !in known },
                            hasMore = result.data.hasMore,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingMore = false, loadMoreFailed = true) }
                }
        }
    }
}
