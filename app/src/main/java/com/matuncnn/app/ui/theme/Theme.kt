package com.matuncnn.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = AppColors.Mid,
    onPrimary = AppColors.Cream,
    primaryContainer = AppColors.Dark,
    onPrimaryContainer = AppColors.Cream,
    secondary = AppColors.Slate,
    onSecondary = AppColors.Cream,
    secondaryContainer = AppColors.Dark,
    onSecondaryContainer = AppColors.Slate,
    tertiary = AppColors.Mid,
    onTertiary = AppColors.Cream,
    background = AppColors.Navy,
    onBackground = AppColors.Cream,
    surface = AppColors.Dark,
    onSurface = AppColors.Cream,
    surfaceVariant = AppColors.Dark,
    onSurfaceVariant = AppColors.Slate,
    outline = AppColors.Slate,
    outlineVariant = AppColors.Slate,
    error = Color(0xFFD32F2F),
    onError = AppColors.Cream,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = AppColors.Cream,
)

@Composable
fun MatUnCnnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
