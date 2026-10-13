package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CriminalDarkColorScheme = darkColorScheme(
    primary = CriminalEmerald,
    onPrimary = Color(0xFF002110),
    primaryContainer = Color(0xFF005227),
    onPrimaryContainer = Color(0xFF75FFAD),
    secondary = CriminalCyan,
    onSecondary = Color(0xFF001E2F),
    secondaryContainer = Color(0xFF004B70),
    onSecondaryContainer = Color(0xFFC7E7FF),
    tertiary = CriminalAmber,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = CriminalCrimson,
    onError = Color.White
)

private val CriminalLightColorScheme = lightColorScheme(
    primary = Color(0xFF00A854),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9FDD3),
    onPrimaryContainer = Color(0xFF003919),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFD97706),
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = CriminalCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Dark mode as default theme per requirements
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CriminalDarkColorScheme else CriminalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
