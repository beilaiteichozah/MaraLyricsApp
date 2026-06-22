package com.maralyrics.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maralyrics.R
import com.maralyrics.domain.model.Contributor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel
) {
    val credits by viewModel.credits.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.credits_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        val isAllEmpty = credits.songContributors.isEmpty() &&
                credits.translators.isEmpty() &&
                credits.supporters.isEmpty() &&
                credits.specialThanks.isEmpty()

        if (isAllEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.credits_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val songContributorsTitle = stringResource(R.string.credits_song_contributors)
            val translatorsTitle = stringResource(R.string.credits_translators)
            val supportersTitle = stringResource(R.string.credits_supporters)
            val specialThanksTitle = stringResource(R.string.credits_special_thanks)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.credits_description),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                        )
                    }
                }

                if (credits.songContributors.isNotEmpty()) {
                    creditsHeader(songContributorsTitle)
                    items(credits.songContributors) { contributor ->
                        ContributorItem(
                            contributor = contributor,
                            onClick = { uriHandler.openUri(contributor.url) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                if (credits.translators.isNotEmpty()) {
                    creditsHeader(translatorsTitle)
                    items(credits.translators) { contributor ->
                        ContributorItem(
                            contributor = contributor,
                            onClick = { uriHandler.openUri(contributor.url) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                if (credits.supporters.isNotEmpty()) {
                    creditsHeader(supportersTitle)
                    items(credits.supporters) { contributor ->
                        ContributorItem(
                            contributor = contributor,
                            onClick = { uriHandler.openUri(contributor.url) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                if (credits.specialThanks.isNotEmpty()) {
                    creditsHeader(specialThanksTitle)
                    items(credits.specialThanks) { contributor ->
                        ContributorItem(
                            contributor = contributor,
                            onClick = { uriHandler.openUri(contributor.url) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.creditsHeader(title: String) {
    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun ContributorItem(
    contributor: Contributor,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = contributor.name,
            style = MaterialTheme.typography.bodyLarge
        )
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}
