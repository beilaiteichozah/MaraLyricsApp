package com.maralyrics.laitei.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.maralyrics.laitei.domain.model.AppColorTheme

object MaraColorSchemes {

    // 1. Mara Green (Identity)
    private val MaraPrimary = Color(0xFF2E7D32)
    private val MaraSecondary = Color(0xFF43A047)
    private val MaraTertiary = Color(0xFF81C784)

    val MaraLight = lightColorScheme(
        primary = MaraPrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC8E6C9),
        onPrimaryContainer = Color(0xFF003300),
        secondary = MaraSecondary,
        tertiary = MaraTertiary
    )

    val MaraDark = darkColorScheme(
        primary = MaraTertiary,
        onPrimary = Color(0xFF003300),
        primaryContainer = MaraPrimary,
        onPrimaryContainer = Color(0xFFC8E6C9),
        secondary = MaraSecondary,
        tertiary = MaraSecondary
    )

    // 2. Ocean Blue
    private val OceanPrimary = Color(0xFF1565C0)
    private val OceanSecondary = Color(0xFF1976D2)
    private val OceanTertiary = Color(0xFF64B5F6)

    val OceanLight = lightColorScheme(
        primary = OceanPrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFBBDEFB),
        onPrimaryContainer = Color(0xFF0D47A1),
        secondary = OceanSecondary,
        tertiary = OceanTertiary
    )

    val OceanDark = darkColorScheme(
        primary = OceanTertiary,
        onPrimary = Color(0xFF0D47A1),
        primaryContainer = OceanPrimary,
        onPrimaryContainer = Color(0xFFBBDEFB),
        secondary = OceanSecondary,
        tertiary = OceanSecondary
    )

    // 3. Emerald Green
    private val EmeraldPrimary = Color(0xFF00695C)
    private val EmeraldSecondary = Color(0xFF00897B)
    private val EmeraldTertiary = Color(0xFF4DB6AC)

    val EmeraldLight = lightColorScheme(
        primary = EmeraldPrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB2DFDB),
        onPrimaryContainer = Color(0xFF004D40),
        secondary = EmeraldSecondary,
        tertiary = EmeraldTertiary
    )

    val EmeraldDark = darkColorScheme(
        primary = EmeraldTertiary,
        onPrimary = Color(0xFF004D40),
        primaryContainer = EmeraldPrimary,
        onPrimaryContainer = Color(0xFFB2DFDB),
        secondary = EmeraldSecondary,
        tertiary = EmeraldSecondary
    )

    // 4. Sunset Orange
    private val SunsetPrimary = Color(0xFFD84315)
    private val SunsetSecondary = Color(0xFFF4511E)
    private val SunsetTertiary = Color(0xFFFF8A65)

    val SunsetLight = lightColorScheme(
        primary = SunsetPrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFCCBC),
        onPrimaryContainer = Color(0xFFBF360C),
        secondary = SunsetSecondary,
        tertiary = SunsetTertiary
    )

    val SunsetDark = darkColorScheme(
        primary = SunsetTertiary,
        onPrimary = Color(0xFFBF360C),
        primaryContainer = SunsetPrimary,
        onPrimaryContainer = Color(0xFFFFCCBC),
        secondary = SunsetSecondary,
        tertiary = SunsetSecondary
    )

    // 5. Royal Purple
    private val PurplePrimary = Color(0xFF6A1B9A)
    private val PurpleSecondary = Color(0xFF8E24AA)
    private val PurpleTertiary = Color(0xFFBA68C8)

    val PurpleLight = lightColorScheme(
        primary = PurplePrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE1BEE7),
        onPrimaryContainer = Color(0xFF4A148C),
        secondary = PurpleSecondary,
        tertiary = PurpleTertiary
    )

    val PurpleDark = darkColorScheme(
        primary = PurpleTertiary,
        onPrimary = Color(0xFF4A148C),
        primaryContainer = PurplePrimary,
        onPrimaryContainer = Color(0xFFE1BEE7),
        secondary = PurpleSecondary,
        tertiary = PurpleSecondary
    )

    // 6. Rose Pink
    private val RosePrimary = Color(0xFFC2185B)
    private val RoseSecondary = Color(0xFFD81B60)
    private val RoseTertiary = Color(0xFFF06292)

    val RoseLight = lightColorScheme(
        primary = RosePrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF8BBD0),
        onPrimaryContainer = Color(0xFF880E4F),
        secondary = RoseSecondary,
        tertiary = RoseTertiary
    )

    val RoseDark = darkColorScheme(
        primary = RoseTertiary,
        onPrimary = Color(0xFF880E4F),
        primaryContainer = RosePrimary,
        onPrimaryContainer = Color(0xFFF8BBD0),
        secondary = RoseSecondary,
        tertiary = RoseSecondary
    )

    // 7. Slate Gray
    private val SlatePrimary = Color(0xFF37474F)
    private val SlateSecondary = Color(0xFF546E7A)
    private val SlateTertiary = Color(0xFF90A4AE)

    val SlateLight = lightColorScheme(
        primary = SlatePrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCFD8DC),
        onPrimaryContainer = Color(0xFF263238),
        secondary = SlateSecondary,
        tertiary = SlateTertiary
    )

    val SlateDark = darkColorScheme(
        primary = SlateTertiary,
        onPrimary = Color(0xFF263238),
        primaryContainer = SlatePrimary,
        onPrimaryContainer = Color(0xFFCFD8DC),
        secondary = SlateSecondary,
        tertiary = SlateSecondary
    )

    fun getColorScheme(theme: AppColorTheme, isDark: Boolean): ColorScheme {
        return when (theme) {
            AppColorTheme.MARA -> if (isDark) MaraDark else MaraLight
            AppColorTheme.OCEAN -> if (isDark) OceanDark else OceanLight
            AppColorTheme.EMERALD -> if (isDark) EmeraldDark else EmeraldLight
            AppColorTheme.SUNSET -> if (isDark) SunsetDark else SunsetLight
            AppColorTheme.PURPLE -> if (isDark) PurpleDark else PurpleLight
            AppColorTheme.ROSE -> if (isDark) RoseDark else RoseLight
            AppColorTheme.SLATE -> if (isDark) SlateDark else SlateLight
        }
    }
}
