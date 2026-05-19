package com.matuncnn.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003544),
    onPrimaryContainer = Color(0xFFB3E5FC),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00695C),
    onSecondaryContainer = Color(0xFFA7FFEB),
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    surface = Color.Black,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF616161),
    outlineVariant = Color(0xFF333333),
    error = Color(0xFFCF6679),
    onError = Color.Black,
)

private val HyprlandColorScheme = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF005C00),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFF69F0AE),
    onSecondary = Color(0xFF003D1A),
    secondaryContainer = Color(0xFF006B38),
    onSecondaryContainer = Color(0xFFB9F6CA),
    tertiary = Color(0xFF40C4FF),
    onTertiary = Color(0xFF002B40),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFC9D1D9),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFC9D1D9),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF21262D),
    error = Color(0xFFF85149),
    onError = Color(0xFF001A17),
)

@Composable
fun MatUnCnnTheme(
    themeIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val colorScheme = when (themeIndex) {
        3 -> AmoledColorScheme
        4 -> HyprlandColorScheme
        1 -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                dynamicLightColorScheme(context)
            else LightColorScheme
        }
        2 -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                dynamicDarkColorScheme(context)
            else DarkColorScheme
        }
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                if (isDark) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            else if (isDark) DarkColorScheme
            else LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
