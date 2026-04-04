package com.playmusicfree.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Surface,
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
