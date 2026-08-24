package com.maralyrics.laitei.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val defaultTypography = Typography()

// Refines the Material3 baseline scale for stronger hierarchy and more
// relaxed body text, since lyrics are read in long multi-line blocks.
val MaraLyricsTypography = defaultTypography.copy(
    headlineLarge = defaultTypography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = defaultTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = defaultTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = defaultTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = defaultTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = defaultTypography.titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyLarge = defaultTypography.bodyLarge.copy(lineHeight = 26.sp)
)
