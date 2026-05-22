package com.matuncnn.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = AppColors.accent,
    onPrimary = AppColors.bg,
    primaryContainer = AppColors.card,
    onPrimaryContainer = AppColors.textPrimary,
    secondary = AppColors.textSecondary,
    onSecondary = AppColors.bg,
    secondaryContainer = AppColors.card,
    onSecondaryContainer = AppColors.textSecondary,
    tertiary = AppColors.accent,
    onTertiary = AppColors.bg,
    background = AppColors.bg,
    onBackground = AppColors.textPrimary,
    surface = AppColors.card,
    onSurface = AppColors.textPrimary,
    surfaceVariant = AppColors.card,
    onSurfaceVariant = AppColors.textSecondary,
    outline = AppColors.border,
    outlineVariant = AppColors.border,
    error = AppColors.statusError,
    onError = AppColors.bg,
    errorContainer = Color(0xFF3A0000),
    onErrorContainer = Color(0xFFFFCDD2),
)

@Composable
fun MatUnCnnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
