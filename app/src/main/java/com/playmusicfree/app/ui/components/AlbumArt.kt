package com.playmusicfree.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun AlbumArt(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    placeholderIconSize: Dp = 20.dp
) {
    val hasArtwork = model.toArtworkToken().isNotBlank()
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(surfaceColor)
            .border(BorderStroke(1.dp, borderColor), shape),
        contentAlignment = Alignment.Center
    ) {
        if (!hasArtwork) {
            AlbumArtFallback(placeholderIconSize)
        } else {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = { AlbumArtFallback(placeholderIconSize) }
            )
        }
    }
}

@Composable
private fun AlbumArtFallback(iconSize: Dp) {
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "No artwork",
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        modifier = Modifier.size(iconSize)
    )
}

private fun Any?.toArtworkToken(): String {
    return when (this) {
        null -> ""
        is String -> this.trim()
        else -> this.toString().trim()
    }
}
