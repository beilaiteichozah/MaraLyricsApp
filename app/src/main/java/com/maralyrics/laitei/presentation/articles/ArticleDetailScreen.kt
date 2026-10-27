package com.maralyrics.laitei.presentation.articles

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.Article
import com.maralyrics.laitei.presentation.common.components.ListSkeleton
import com.maralyrics.laitei.presentation.common.components.LoadErrorState
import com.maralyrics.laitei.presentation.common.components.OfflineNotice
import com.maralyrics.laitei.utils.formatMediumDate

private const val SITE_ARTICLE_URL = "https://maralyrics.com/article/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    onBackClick: () -> Unit,
    viewModel: ArticleDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val articleUrl = SITE_ARTICLE_URL + viewModel.slug
    val success = uiState as? ArticleDetailUiState.Success

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    if (success != null) {
                        IconButton(onClick = { shareArticle(context, success.article, articleUrl) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.article_share))
                        }
                    }
                    IconButton(onClick = { uriHandler.openUri(articleUrl) }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = stringResource(R.string.article_open_website))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ArticleDetailUiState.Loading -> ListSkeleton()
                is ArticleDetailUiState.Error -> LoadErrorState(onRetry = viewModel::load)
                is ArticleDetailUiState.Success -> {
                    if (state.isFromCache) OfflineNotice()
                    ArticleBody(state.article)
                }
            }
        }
    }
}

@Composable
private fun ArticleBody(article: Article) {
    val linkColor = MaterialTheme.colorScheme.primary
    val body = remember(article.contentHtml, linkColor) {
        article.contentHtml?.let {
            AnnotatedString.fromHtml(
                htmlString = it,
                linkStyles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            )
        }
    }
    val meta = listOfNotNull(article.authorName, article.publishedAt?.let(::formatMediumDate))
        .joinToString(" • ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        // Fall back to the summary if the server ever sends an article without a body.
        when {
            body != null -> Text(text = body, style = MaterialTheme.typography.bodyLarge)
            article.summary != null -> Text(text = article.summary, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun shareArticle(context: Context, article: Article, url: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, "${article.title}\n$url")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}
