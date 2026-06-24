package com.maralyrics.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    OnboardingContent(
        onFinish = {
            viewModel.completeOnboarding(onFinish)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingContent(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(top = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < 3) {
                    TextButton(onClick = { onFinish() }) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator
                Row(
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(4) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 12.dp else 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    if (pagerState.currentPage > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Next / Get Started Button
                    if (pagerState.currentPage < 3) {
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(56.dp).padding(horizontal = 16.dp)
                        ) {
                            Text(stringResource(R.string.onboarding_next))
                        }
                    } else {
                        Button(
                            onClick = { onFinish() },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(stringResource(R.string.onboarding_get_started))
                        }
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            OnboardingPage(page = page)
        }
    }
}


@Composable
fun OnboardingPage(page: Int) {
    val title = when (page) {
        0 -> stringResource(R.string.onboarding_title_1)
        1 -> stringResource(R.string.onboarding_title_2)
        2 -> stringResource(R.string.onboarding_title_3)
        3 -> stringResource(R.string.onboarding_title_4)
        else -> ""
    }

    val description = when (page) {
        0 -> stringResource(R.string.onboarding_desc_1)
        1 -> stringResource(R.string.onboarding_desc_2)
        2 -> stringResource(R.string.onboarding_desc_3)
        3 -> stringResource(R.string.onboarding_desc_4)
        else -> ""
    }

    val features = when (page) {
        0 -> listOf(
            stringResource(R.string.onboarding_feature_1_1),
            stringResource(R.string.onboarding_feature_1_2),
            stringResource(R.string.onboarding_feature_1_3)
        )
        1 -> listOf(
            stringResource(R.string.onboarding_feature_2_1),
            stringResource(R.string.onboarding_feature_2_2),
            stringResource(R.string.onboarding_feature_2_3)
        )
        2 -> listOf(
            stringResource(R.string.onboarding_feature_3_1),
            stringResource(R.string.onboarding_feature_3_2),
            stringResource(R.string.onboarding_feature_3_3),
            stringResource(R.string.onboarding_feature_3_4)
        )
        3 -> listOf(
            stringResource(R.string.onboarding_feature_4_1),
            stringResource(R.string.onboarding_feature_4_2),
            stringResource(R.string.onboarding_feature_4_3),
            stringResource(R.string.onboarding_feature_4_4)
        )
        else -> emptyList()
    }

    val icon = when (page) {
        0 -> Icons.Default.MusicNote
        1 -> Icons.Default.Search
        2 -> Icons.Default.Favorite
        3 -> Icons.Default.Settings
        else -> Icons.Default.Info
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (page == 0) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.mara_lyrics_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(32.dp))
            )
        } else {
            Surface(
                modifier = Modifier.size(150.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(40.dp)
                        .fillMaxSize(),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


