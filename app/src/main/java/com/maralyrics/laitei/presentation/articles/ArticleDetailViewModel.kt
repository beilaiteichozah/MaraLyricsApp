package com.maralyrics.laitei.presentation.articles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maralyrics.laitei.domain.model.Article
import com.maralyrics.laitei.domain.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ArticleDetailUiState {
    data object Loading : ArticleDetailUiState
    data class Success(val article: Article, val isFromCache: Boolean) : ArticleDetailUiState
    data object Error : ArticleDetailUiState
}

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val slug: String = checkNotNull(savedStateHandle["slug"])

    private val _uiState = MutableStateFlow<ArticleDetailUiState>(ArticleDetailUiState.Loading)
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ArticleDetailUiState.Loading
            articleRepository.getArticle(slug)
                .onSuccess { _uiState.value = ArticleDetailUiState.Success(it.data, it.isFromCache) }
                .onFailure { _uiState.value = ArticleDetailUiState.Error }
        }
    }
}
