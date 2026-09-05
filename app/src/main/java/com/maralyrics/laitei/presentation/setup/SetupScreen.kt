package com.maralyrics.laitei.presentation.setup

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.maralyrics.laitei.R
import com.maralyrics.laitei.domain.model.AppLanguage

@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.mara_lyrics_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.setup_welcome),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = stringResource(R.string.setup_description),
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

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
                        0 -> CategoryStep(
                            selectedCategories = uiState.selectedCategories,
                            onCategorySelected = viewModel::setCategory
                        )
                        1 -> DownloadStep(
                            progress = uiState.downloadProgress,
                            error = uiState.error,
                            isComplete = uiState.isDownloadComplete,
                            isOnline = uiState.isOnline,
                            onRetry = viewModel::retryDownload,
                            onCheckInternet = {
                                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                            },
                            onContinueOffline = viewModel::continueOffline,
                            onImportOfflineData = viewModel::importOfflineData
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.currentStep > 0) {
                    TextButton(
                        onClick = viewModel::previousStep,
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_back),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (uiState.currentStep < 1) {
                    Button(
                        onClick = viewModel::nextStep,
                        modifier = Modifier
                            .height(56.dp)
                            .padding(start = 16.dp)
                            .weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = stringResource(R.string.btn_next),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryStep(
    selectedCategories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("All", "Gospel", "Love", "Patriotic", "Traditional")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.setup_category_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )
        )
        Text(
            text = stringResource(R.string.setup_category_desc),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategories.contains(category)
                Surface(
                    onClick = { onCategorySelected(category) },
                    shape = MaterialTheme.shapes.large,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null
                        )
                        val displayName = when (category) {
                            "All" -> stringResource(R.string.cat_all)
                            "Gospel" -> stringResource(R.string.cat_gospel)
                            "Love" -> stringResource(R.string.cat_love)
                            "Patriotic" -> stringResource(R.string.cat_patriotic)
                            "Traditional" -> stringResource(R.string.cat_traditional)
                            else -> category
                        }
                        Text(
                            text = displayName,
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
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun DownloadStep(
    progress: com.maralyrics.laitei.domain.model.DownloadProgress?,
    error: String?,
    isComplete: Boolean,
    isOnline: Boolean,
    onRetry: () -> Unit,
    onCheckInternet: () -> Unit,
    onContinueOffline: () -> Unit,
    onImportOfflineData: (android.net.Uri) -> Unit
) {
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportOfflineData) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (!isOnline && !isComplete && progress == null) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.connection_required),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.connection_required_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onCheckInternet,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.btn_check_internet))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onContinueOffline,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.btn_continue_offline))
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(stringResource(R.string.import_offline_data))
            }
        } else {
            Text(
                text = if (isComplete) stringResource(R.string.setup_complete_title) else stringResource(R.string.setup_download_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.error_prefix, error),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_try_again))
                }
            } else if (isComplete) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.sync_complete),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress?.percentage?.div(100f) ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(
                        R.string.sync_progress,
                        progress?.downloadedItems ?: 0
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (progress != null) {
                    Text(
                        text = stringResource(R.string.sync_downloading_prefix, progress.currentEntity),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
