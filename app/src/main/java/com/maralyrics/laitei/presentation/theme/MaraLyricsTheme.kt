package com.maralyrics.laitei.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.maralyrics.laitei.domain.model.AppColorTheme
import com.maralyrics.laitei.domain.model.AppTheme

@Composable
fun MaraLyricsTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    colorTheme: AppColorTheme = AppColorTheme.MARA,
    content: @Composable () -> Unit
) {
    val darkTheme = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = MaraColorSchemes.getColorScheme(colorTheme, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaraLyricsTypography,
        shapes = MaraLyricsShapes,
        content = content
    )
}
