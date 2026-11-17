package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeMode

private val MhopLightColorScheme = lightColorScheme(
    primary = MhopBlue,
    onPrimary = MhopWhite,
    primaryContainer = MhopBlueTint,
    onPrimaryContainer = MhopBlueDark,
    secondary = MhopBlueLight,
    onSecondary = MhopWhite,
    secondaryContainer = MhopOffWhite,
    onSecondaryContainer = MhopBlack,
    tertiary = MhopBlueDark,
    onTertiary = MhopWhite,
    background = MhopWhite,
    onBackground = MhopBlack,
    surface = MhopWhite,
    onSurface = MhopBlack,
    surfaceVariant = MhopOffWhite,
    onSurfaceVariant = MhopInkSecondary,
    outline = MhopDividerLight,
    outlineVariant = MhopBlueSubtleBorder,
    error = MhopErrorRed,
    onError = MhopWhite,
    errorContainer = MhopErrorContainer,
    onErrorContainer = MhopErrorRed,
)

private val MhopDarkColorScheme = darkColorScheme(
    primary = MhopDarkPrimary,
    onPrimary = MhopDarkBackground,
    primaryContainer = MhopDarkPrimaryContainer,
    onPrimaryContainer = MhopDarkOnPrimaryContainer,
    secondary = MhopBlueLight,
    onSecondary = MhopDarkBackground,
    secondaryContainer = MhopDarkSurfaceElevated,
    onSecondaryContainer = MhopDarkText,
    tertiary = MhopBlueSoft,
    onTertiary = MhopDarkBackground,
    background = MhopDarkBackground,
    onBackground = MhopDarkText,
    surface = MhopDarkSurface,
    onSurface = MhopDarkText,
    surfaceVariant = MhopDarkSurfaceElevated,
    onSurfaceVariant = MhopDarkTextSecondary,
    outline = MhopDarkOutline,
    outlineVariant = MhopDarkOutline,
    error = MhopDarkError,
    onError = MhopDarkBackground,
    errorContainer = MhopDarkErrorContainer,
    onErrorContainer = MhopDarkError,
)

val MhopShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun MhopSongsTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (isDark) MhopDarkColorScheme else MhopLightColorScheme,
        typography = MhopTypography,
        shapes = MhopShapes,
        content = content,
    )
}
