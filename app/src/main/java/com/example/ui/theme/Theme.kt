package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinematicDarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = Color(0xFF003355),
    primaryContainer = Color(0xFF004B75),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = ElectricCyan,
    onSecondary = Color(0xFF003355),
    secondaryContainer = Color(0xFF004B75),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = CoralRed,
    onTertiary = Color(0xFF601410),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
    error = CoralRed,
    onError = Color(0xFF601410)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // We enforce our custom Cinematic Dark Theme for a premium video-editor feel
    MaterialTheme(
        colorScheme = CinematicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
