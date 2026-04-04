package com.playmusicfree.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = Secondary,
    tertiary = Tertiary,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = OnSurface,
    onSurface = OnSurface,
)

@Composable
fun PlayMusicFreeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
