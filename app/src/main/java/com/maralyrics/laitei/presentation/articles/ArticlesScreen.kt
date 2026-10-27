package com.maralyrics.laitei.presentation.articles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.Article
import com.maralyrics.laitei.presentation.common.components.EmptyState
import com.maralyrics.laitei.presentation.common.components.ListSkeleton
import com.maralyrics.laitei.presentation.common.components.LoadErrorState
import com.maralyrics.laitei.presentation.common.components.OfflineNotice
import com.maralyrics.laitei.utils.formatMediumDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesScreen(
    onBackClick: () -> Unit,
    onArticleClick: (String) -> Unit,
    viewModel: ArticlesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.articles_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> ListSkeleton()
                state.loadFailed -> LoadErrorState(onRetry = viewModel::refresh)
                state.articles.isEmpty() -> EmptyState(
                    icon = Icons.Default.Newspaper,
                    message = stringResource(R.string.articles_empty_title),
                    description = stringResource(R.string.articles_empty_desc)
                )
                else -> {
                    if (state.isFromCache) OfflineNotice()
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ArticleList(
                            state = state,
                            onArticleClick = onArticleClick,
                            onLoadMore = viewModel::loadMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleList(
    state: ArticlesUiState,
    onArticleClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    // Ask for the next page once the reader is within a few items of the end.
    val nearEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.articles.size) {
        if (nearEnd) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.articles, key = { it.slug }) { article ->
            ArticleCard(article = article, onClick = { onArticleClick(article.slug) })
        }
        if (state.isLoadingMore || state.loadMoreFailed) {
            item(key = "footer") {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    if (state.loadMoreFailed) {
                        TextButton(onClick = onLoadMore) { Text(stringResource(R.string.btn_try_again)) }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(article: Article, onClick: () -> Unit) {
    val meta = listOfNotNull(article.authorName, article.publishedAt?.let(::formatMediumDate))
        .joinToString(" • ")

    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (article.summary != null) {
                Text(
                    text = article.summary,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
