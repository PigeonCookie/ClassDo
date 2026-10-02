package com.coursework.tracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun lightScheme(accent: AccentPalette): ColorScheme = lightColorScheme(
    primary = accent.lightPrimary,
    onPrimary = accent.lightOnPrimary,
    primaryContainer = accent.lightPrimaryContainer,
    onPrimaryContainer = accent.lightOnPrimaryContainer,
    inversePrimary = accent.lightPrimary,

    secondary = accent.lightPrimary,
    onSecondary = accent.lightOnPrimary,
    secondaryContainer = accent.lightPrimaryContainer,
    onSecondaryContainer = accent.lightOnPrimaryContainer,

    tertiary = StatusToday,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),

    background = LightBackground,
    onBackground = LightOnSurface,

    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = accent.lightPrimary,
    inverseSurface = Color(0xFF2B2F3A),
    inverseOnSurface = Color(0xFFF1F2F6),

    outline = LightOutline,
    outlineVariant = LightOutline,

    error = StatusOverdue,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E8),
    onErrorContainer = Color(0xFF4C0519),
)

private fun darkScheme(accent: AccentPalette): ColorScheme = darkColorScheme(
    primary = accent.darkPrimary,
    onPrimary = accent.darkOnPrimary,
    primaryContainer = accent.darkPrimaryContainer,
    onPrimaryContainer = accent.darkOnPrimaryContainer,
    inversePrimary = accent.darkPrimary,

    secondary = accent.darkPrimary,
    onSecondary = accent.darkOnPrimary,
    secondaryContainer = accent.darkPrimaryContainer,
    onSecondaryContainer = accent.darkOnPrimaryContainer,

    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),

    background = DarkBackground,
    onBackground = DarkOnSurface,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = accent.darkPrimary,
    inverseSurface = Color(0xFFE8EAF0),
    inverseOnSurface = Color(0xFF232733),

    outline = DarkOutline,
    outlineVariant = DarkOutline,

    error = Color(0xFFFDA4AF),
    onError = Color(0xFF4C0519),
    errorContainer = Color(0xFF881337),
    onErrorContainer = Color(0xFFFFE4E8),
)

@Composable
fun ClassDoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentPalette = Accents.Default,
    content: @Composable () -> Unit,
) {
    val scheme = remember(accent, darkTheme) {
        if (darkTheme) darkScheme(accent) else lightScheme(accent)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            // 顶部永远是彩色渐变头部，所以状态栏图标一直用浅色才看得清
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppTheme provides AppTheme(accent, darkTheme)) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content,
        )
    }
}
