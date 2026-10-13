package com.example.gallery.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DD0C4),
    onPrimary = Color(0xFF00201D),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFA0F2E9),
    secondary = Color(0xFFB0CCC8),
    onSecondary = Color(0xFF1B3532),
    secondaryContainer = Color(0xFF2D4A46),
    onSecondaryContainer = Color(0xFFCCE8E4),
    background = Color(0xFF0E0F13),
    onBackground = Color(0xFFE3E3E8),
    surface = Color(0xFF0E0F13),
    onSurface = Color(0xFFE3E3E8),
    surfaceVariant = Color(0xFF23262D),
    onSurfaceVariant = Color(0xFFC3C6CF),
    surfaceContainerLowest = Color(0xFF0A0B0E),
    surfaceContainerLow = Color(0xFF12141A),
    surfaceContainer = Color(0xFF171A20),
    surfaceContainerHigh = Color(0xFF1D2027),
    surfaceContainerHighest = Color(0xFF262A32),
    outline = Color(0xFF5B5F69),
    outlineVariant = Color(0xFF3A3E47),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0A06)
)

@Composable
fun GalleryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
