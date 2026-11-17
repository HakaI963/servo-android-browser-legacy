package com.servodroid.browser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servodroid.browser.R
import com.servodroid.browser.model.ThemeMode

/**
 * "Ink" palette - one tinted neutral ramp with a single jade accent. The web page is the only
 * bright thing on screen; the browser chrome stays quiet so it never competes with content.
 */
object Ink {
    val Background = Color(0xFF0B0E11)
    val Surface = Color(0xFF141A1F)
    val SurfaceRaised = Color(0xFF1C242B)
    val Outline = Color(0xFF232C33)

    val TextPrimary = Color(0xFFE6EDF3)
    val TextSecondary = Color(0xFF98A4AF)
    val TextMuted = Color(0xFF64707B)

    val Accent = Color(0xFF5FC79A)
    val AccentDim = Color(0xFF2E5A47)
    val OnAccent = Color(0xFF06120C)

    val Danger = Color(0xFFD9736A)
    val PrivateAccent = Color(0xFFA78BD2)
    val PrivateSurface = Color(0xFF17151F)
}

object Dimensions {
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 24.dp
    val SpaceXxl = 32.dp

    val ToolbarHeight = 56.dp
    val BottomBarHeight = 56.dp
    val AddressBarHeight = 44.dp
    val IconSize = 20.dp
    val TouchTarget = 48.dp

    val RadiusControl = 10.dp
    val RadiusCard = 14.dp
    val RadiusSheet = 20.dp
    val RadiusPill = 24.dp
}

private val Plex = FontFamily(
    Font(R.font.ibm_plex_sans_variable, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_variable, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_variable, FontWeight.SemiBold),
)

private val JetBrains = FontFamily(
    Font(R.font.jetbrains_mono_variable, FontWeight.Normal),
    Font(R.font.jetbrains_mono_variable, FontWeight.Medium),
)

object AppTypography {
    val Display = TextStyle(
        fontFamily = Plex,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    )
    val Headline = TextStyle(
        fontFamily = Plex,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    )
    val Title = TextStyle(
        fontFamily = Plex,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
    val Body = TextStyle(
        fontFamily = Plex,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val Label = TextStyle(
        fontFamily = Plex,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val Caption = TextStyle(
        fontFamily = Plex,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
    val Mono = TextStyle(
        fontFamily = JetBrains,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
    val MonoSmall = TextStyle(
        fontFamily = JetBrains,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )

}

private val DarkScheme = darkColorScheme(
    primary = Ink.Accent,
    onPrimary = Ink.OnAccent,
    primaryContainer = Ink.AccentDim,
    onPrimaryContainer = Ink.TextPrimary,
    secondary = Ink.TextSecondary,
    onSecondary = Ink.Background,
    background = Ink.Background,
    onBackground = Ink.TextPrimary,
    surface = Ink.Surface,
    onSurface = Ink.TextPrimary,
    surfaceVariant = Ink.SurfaceRaised,
    onSurfaceVariant = Ink.TextSecondary,
    surfaceContainerHighest = Ink.SurfaceRaised,
    outline = Ink.Outline,
    outlineVariant = Ink.Outline,
    error = Ink.Danger,
    onError = Ink.Background,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1F7A57),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6EFE2),
    onPrimaryContainer = Color(0xFF072A1D),
    secondary = Color(0xFF53616B),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F7F8),
    onBackground = Color(0xFF111820),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111820),
    surfaceVariant = Color(0xFFE8ECEF),
    onSurfaceVariant = Color(0xFF53616B),
    surfaceContainerHighest = Color(0xFFE8ECEF),
    outline = Color(0xFFCBD3D9),
    outlineVariant = Color(0xFFCBD3D9),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

@Composable
fun ServoTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = androidx.compose.material3.Typography(
            displayLarge = AppTypography.Display,
            headlineMedium = AppTypography.Headline,
            titleLarge = AppTypography.Title,
            bodyLarge = AppTypography.Body,
            bodyMedium = AppTypography.Body,
            labelMedium = AppTypography.Label,
            labelSmall = AppTypography.Caption,
        ),
        content = content,
    )
}
