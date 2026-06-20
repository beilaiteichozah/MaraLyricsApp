package com.maralyrics.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.maralyrics.domain.model.AppColorTheme
import com.maralyrics.domain.model.AppTheme

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
        typography = MaterialTheme.typography, // We can refine this later if needed
        content = content
    )
}
