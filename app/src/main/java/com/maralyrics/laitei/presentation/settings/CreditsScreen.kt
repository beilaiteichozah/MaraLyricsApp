package com.maralyrics.laitei.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.Contributor
import com.maralyrics.laitei.domain.model.Supporter
import com.maralyrics.laitei.domain.model.TeamMember
import com.maralyrics.laitei.domain.model.TeamRole
import com.maralyrics.laitei.presentation.common.components.ProfileImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel,
    extrasViewModel: CreditsExtrasViewModel = hiltViewModel()
) {
    val credits by viewModel.credits.collectAsState()
    val extras by extrasViewModel.extras.collectAsState()
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
                credits.specialThanks.isEmpty() &&
                extras.isEmpty

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
            val sponsorsTitle = stringResource(R.string.credits_sponsors)
            val sponsorsIntro = stringResource(R.string.credits_sponsors_intro)
            val partnersTitle = stringResource(R.string.credits_partners)
            val partnersIntro = stringResource(R.string.credits_partners_intro)
            val editorsTitle = stringResource(R.string.credits_editors)
            val editorsIntro = stringResource(R.string.credits_editors_intro)

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

                // From the website's Project page; shown once loaded (or from the saved copy offline).
                if (extras.sponsors.isNotEmpty()) {
                    creditsHeader(sponsorsTitle, sponsorsIntro)
                    supporterItems(extras.sponsors) { url -> uriHandler.openUri(url) }
                }
                if (extras.partners.isNotEmpty()) {
                    creditsHeader(partnersTitle, partnersIntro)
                    supporterItems(extras.partners) { url -> uriHandler.openUri(url) }
                }
                if (extras.team.isNotEmpty()) {
                    creditsHeader(editorsTitle, editorsIntro)
                    items(extras.team) { member ->
                        TeamMemberItem(member)
                        CreditsDivider()
                    }
                }

                if (credits.songContributors.isNotEmpty()) {
                    creditsHeader(songContributorsTitle)
                    items(credits.songContributors) { contributor ->
                        ContributorItem(
                            contributor = contributor,
                            onClick = { uriHandler.openUri(contributor.url) }
                        )
                        CreditsDivider()
                    }
                }

                if (credits.translators.isNotEmpty()) {
                    creditsHeader(translatorsTitle)
                    items(credits.translators) { contributor ->
                        ContributorItem(
                            contributor = contributor,
                            onClick = { uriHandler.openUri(contributor.url) }
                        )
                        CreditsDivider()
                    }
                }

                if (credits.supporters.isNotEmpty()) {
                    creditsHeader(supportersTitle)
                    items(credits.supporters) { contributor ->
                        ContributorItem(
                            contributor = contributor,
                            onClick = { uriHandler.openUri(contributor.url) }
                        )
                        CreditsDivider()
                    }
                }

                if (credits.specialThanks.isNotEmpty()) {
                    creditsHeader(specialThanksTitle)
                    items(credits.specialThanks) { contributor ->
                        ContributorItem(
                            contributor = contributor,
                            onClick = { uriHandler.openUri(contributor.url) }
                        )
                        CreditsDivider()
                    }
                }
            }
        }
    }
}

private fun LazyListScope.creditsHeader(title: String, intro: String? = null) {
    item {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (intro != null) {
                Text(
                    text = intro,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun LazyListScope.supporterItems(supporters: List<Supporter>, openUrl: (String) -> Unit) {
    items(supporters) { supporter ->
        SupporterItem(supporter = supporter, onClick = supporter.websiteUrl?.let { url -> { openUrl(url) } })
        CreditsDivider()
    }
}

@Composable
private fun CreditsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
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

@Composable
private fun SupporterItem(supporter: Supporter, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Logos are shown whole inside a rounded square, not cropped into a circle.
        ProfileImage(
            imageUrl = supporter.logoUrl,
            name = supporter.name,
            size = 48.dp,
            shape = RoundedCornerShape(10.dp),
            contentScale = ContentScale.Fit
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = supporter.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (supporter.description != null) {
                Text(
                    text = supporter.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TeamMemberItem(member: TeamMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProfileImage(imageUrl = member.photoUrl, name = member.name, size = 44.dp)
        Column {
            Text(text = member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            val role = when (member.role) {
                TeamRole.EDITOR -> stringResource(R.string.role_editor)
                TeamRole.MANAGER -> stringResource(R.string.role_manager)
                TeamRole.SUPER_ADMIN -> stringResource(R.string.role_super_admin)
                TeamRole.OTHER -> null
            }
            if (role != null) {
                Text(text = role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
