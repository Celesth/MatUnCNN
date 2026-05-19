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
