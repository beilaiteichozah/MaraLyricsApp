package com.maralyrics.laitei.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.maralyrics.laitei.domain.model.AppColorTheme
import com.maralyrics.laitei.domain.model.AppTheme
import com.maralyrics.laitei.utils.findActivity

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

    // Status bar icon color must follow the app's own theme choice, not just system
    // dark mode — the XML theme (values/values-night) can't tell these apart since the
    // user can override light/dark independently of the device setting.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaraLyricsTypography,
        shapes = MaraLyricsShapes,
        content = content
    )
}
