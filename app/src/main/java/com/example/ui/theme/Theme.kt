package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MasRed,
    onPrimary = Color.White,
    primaryContainer = MasRedDark,
    onPrimaryContainer = MasRedLight,
    secondary = MasAmber,
    onSecondary = Color.White,
    tertiary = MasGreen,
    onTertiary = Color.White,
    background = MasInk,
    onBackground = Color(0xFFEEEEEE),
    surface = MasSurfaceDark,
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = MasInkLight,
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = MasBorderDark,
    outlineVariant = Color(0xFF262522)
)

private val LightColorScheme = lightColorScheme(
    primary = MasRed,
    onPrimary = Color.White,
    primaryContainer = MasRedLight,
    onPrimaryContainer = MasRedDark,
    secondary = MasAmber,
    onSecondary = Color.White,
    tertiary = MasGreen,
    onTertiary = Color.White,
    background = MasPaperSoft,
    onBackground = MasInk,
    surface = MasPaper,
    onSurface = MasInk,
    surfaceVariant = MasPaperSoft,
    onSurfaceVariant = MasMuted,
    outline = MasRule,
    outlineVariant = Color(0xFFEDEAE3)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
