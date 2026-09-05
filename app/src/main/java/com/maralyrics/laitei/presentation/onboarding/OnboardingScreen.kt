package com.maralyrics.laitei.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.AppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val privacyAccepted by viewModel.privacyAccepted.collectAsState()

    OnboardingContent(
        selectedLanguage = selectedLanguage,
        privacyAccepted = privacyAccepted,
        onLanguageSelected = viewModel::setLanguage,
        awaitLanguagePersisted = viewModel::awaitLanguagePersisted,
        onPrivacyAccepted = viewModel::acceptPrivacyPolicy,
        onFinish = {
            viewModel.completeOnboarding(onFinish)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingContent(
    selectedLanguage: AppLanguage,
    privacyAccepted: Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
    awaitLanguagePersisted: suspend () -> Unit,
    onPrivacyAccepted: () -> Unit,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 6 })
    val scope = rememberCoroutineScope()
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(top = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage in 1..4) {
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
                    .navigationBarsPadding()
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
                    repeat(6) { iteration ->
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
                    if (pagerState.currentPage < 5) {
                        Button(
                            onClick = {
                                if (pagerState.currentPage == 0 && !privacyAccepted) {
                                    scope.launch {
                                        awaitLanguagePersisted()
                                        showPrivacyDialog = true
                                    }
                                } else {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.height(56.dp).padding(horizontal = 16.dp)
                        ) {
                            Text(stringResource(R.string.onboarding_next))
                        }
                    } else {
                        Button(
                            onClick = { onFinish() },
                            shape = MaterialTheme.shapes.large,
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
            userScrollEnabled = pagerState.currentPage != 0 || privacyAccepted,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            if (page == 0) {
                LanguageSelectionPage(
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            } else {
                OnboardingPage(page = page - 1)
            }
        }
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onAccepted = {
                onPrivacyAccepted()
                showPrivacyDialog = false
                scope.launch {
                    pagerState.animateScrollToPage(1)
                }
            },
            onDismiss = {
                showPrivacyDialog = false
            }
        )
    }
}

@Composable
fun PrivacyPolicyDialog(
    onAccepted: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeclineMessage by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val canAccept = remember {
        derivedStateOf {
            scrollState.value >= (scrollState.maxValue - 50).coerceAtLeast(0)
        }
    }
    // Captured here (outside Dialog's own window boundary) and re-provided below,
    // since Dialog can otherwise lose the app's in-app locale override for its content.
    val localizedContext = LocalContext.current
    val localizedConfiguration = LocalConfiguration.current
    // Dialog's own window doesn't reliably report system bar insets, so the bottom
    // buttons can end up drawn under the 3-button nav bar. Capture the real inset here
    // (still in the correctly-inset host composition) and apply it as explicit padding.
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
      CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration
      ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .statusBarsPadding()
                .padding(bottom = navigationBarPadding),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.privacy_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PrivacySection(stringResource(R.string.privacy_intro_title), stringResource(R.string.privacy_intro_content))
                        PrivacySection(stringResource(R.string.privacy_data_title), stringResource(R.string.privacy_data_content))
                        PrivacySection(stringResource(R.string.privacy_permissions_title), stringResource(R.string.privacy_permissions_content))
                        PrivacySection(stringResource(R.string.privacy_contact_title), stringResource(R.string.privacy_contact_content))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showDeclineMessage) {
                    Text(
                        text = stringResource(R.string.privacy_decline_msg),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (!canAccept.value) {
                    Text(
                        text = stringResource(R.string.privacy_scroll_to_read),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            if (showDeclineMessage) {
                                onDismiss()
                            } else {
                                showDeclineMessage = true 
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(if (showDeclineMessage) stringResource(R.string.btn_ok) else stringResource(R.string.btn_decline))
                    }

                    Button(
                        onClick = onAccepted,
                        enabled = canAccept.value,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(R.string.btn_agree))
                    }
                }
            }
        }
      }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LanguageSelectionPage(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.mara_lyrics_logo),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.setup_language_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppLanguage.entries.forEach { language ->
                val isSelected = language == selectedLanguage
                Surface(
                    onClick = { onLanguageSelected(language) },
                    shape = MaterialTheme.shapes.large,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                        Text(
                            text = when(language) {
                                AppLanguage.MARA -> stringResource(R.string.lang_mara) + " (" + stringResource(R.string.lang_native) + ")"
                                AppLanguage.BURMESE -> stringResource(R.string.lang_burmese)
                                else -> stringResource(R.string.lang_english)
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = 24.sp
                            ),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
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
        4 -> stringResource(R.string.onboarding_title_5)
        else -> ""
    }

    val description = when (page) {
        0 -> stringResource(R.string.onboarding_desc_1)
        1 -> stringResource(R.string.onboarding_desc_2)
        2 -> stringResource(R.string.onboarding_desc_3)
        3 -> stringResource(R.string.onboarding_desc_4)
        4 -> stringResource(R.string.onboarding_desc_5)
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
            stringResource(R.string.onboarding_feature_3_3)
        )
        3 -> listOf(
            stringResource(R.string.onboarding_feature_4_1),
            stringResource(R.string.onboarding_feature_4_2),
            stringResource(R.string.onboarding_feature_4_3),
            stringResource(R.string.onboarding_feature_4_4)
        )
        4 -> listOf(
            stringResource(R.string.onboarding_feature_5_1),
            stringResource(R.string.onboarding_feature_5_2),
            stringResource(R.string.onboarding_feature_5_3),
            stringResource(R.string.onboarding_feature_5_4)
        )
        else -> emptyList()
    }

    val icon = when (page) {
        0 -> Icons.Default.MusicNote
        1 -> Icons.Default.Share
        2 -> Icons.Default.Search
        3 -> Icons.Default.Favorite
        4 -> Icons.Default.Settings
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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp
                        ),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
