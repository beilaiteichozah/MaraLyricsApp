package com.maralyrics.presentation.setup

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.R
import com.maralyrics.domain.model.AppLanguage
import com.maralyrics.domain.model.SongCategory

@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.setup_welcome),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        }
                    }, label = "SetupStep"
                ) { step ->
                    when (step) {
                        0 -> LanguageStep(
                            selectedLanguage = uiState.selectedLanguage,
                            onLanguageSelected = viewModel::setLanguage
                        )
                        1 -> CategoryStep(
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = viewModel::setCategory
                        )
                        2 -> DownloadStep(
                            progress = uiState.downloadProgress,
                            error = uiState.error,
                            isComplete = uiState.isDownloadComplete
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (uiState.currentStep > 0 && uiState.currentStep < 2) {
                    TextButton(onClick = viewModel::previousStep) {
                        Text(stringResource(R.string.btn_back))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (uiState.currentStep < 2) {
                    Button(onClick = viewModel::nextStep) {
                        Text(stringResource(R.string.btn_next))
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageStep(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.setup_language_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(Modifier.selectableGroup()) {
            AppLanguage.entries.forEach { language ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (language == selectedLanguage),
                            onClick = { onLanguageSelected(language) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (language == selectedLanguage),
                        onClick = null
                    )
                    Text(
                        text = if (language == AppLanguage.MARA) "Mara" else "English",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryStep(
    selectedCategory: SongCategory,
    onCategorySelected: (SongCategory) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.setup_category_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.setup_category_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(Modifier.selectableGroup()) {
            SongCategory.entries.forEach { category ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (category == selectedCategory),
                            onClick = { onCategorySelected(category) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (category == selectedCategory),
                        onClick = null
                    )
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadStep(
    progress: com.maralyrics.domain.model.DownloadProgress?,
    error: String?,
    isComplete: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.setup_download_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (error != null) {
            Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
        } else if (isComplete) {
            Text(text = stringResource(R.string.sync_complete))
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        } else {
            LinearProgressIndicator(
                progress = { progress?.percentage?.div(100f) ?: 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.sync_progress,
                    progress?.downloadedSongs ?: 0
                )
            )
            if (progress != null) {
                Text(
                    text = stringResource(
                        R.string.sync_remaining,
                        progress.estimatedSecondsRemaining
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
