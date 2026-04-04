package com.playmusicfree.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.playmusicfree.app.data.model.Song

@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }
    val borderColor = if (isPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        tonalElevation = if (isPlaying) 2.dp else 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(
                start = 12.dp,
                end = if (onMoreClick != null) 2.dp else 12.dp,
                top = 10.dp,
                bottom = 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                model = song.albumArtUri,
                contentDescription = song.displayAlbum(),
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(10.dp),
                placeholderIconSize = 18.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.displayArtist(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatDuration(song.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
                modifier = Modifier.padding(end = if (onMoreClick != null) 4.dp else 0.dp)
            )

            if (onMoreClick != null) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
