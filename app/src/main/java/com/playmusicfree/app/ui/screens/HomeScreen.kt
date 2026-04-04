package com.playmusicfree.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.playmusicfree.app.data.model.Playlist
import com.playmusicfree.app.data.model.Song
import com.playmusicfree.app.data.model.SongMetadataSuggestion
import com.playmusicfree.app.ui.components.AlbumArt
import com.playmusicfree.app.ui.components.SongItem
import com.playmusicfree.app.ui.components.displayTitle

@Composable
fun HomeScreen(
    songs: List<Song>,
    currentSong: Song?,
    playlists: List<Playlist>,
    onSongClick: (Song) -> Unit,
    onAddSongToPlaylist: (songId: Long, playlistId: Long) -> Unit,
    onCreatePlaylistAndAdd: (name: String, songId: Long) -> Unit,
    onRenameSong: (songId: Long, newTitle: String) -> Unit,
    metadataLookupSongId: Long?,
    pendingMetadataSuggestion: SongMetadataSuggestion?,
    metadataOverriddenSongIds: Set<Long>,
    onLookupSongMetadata: (songId: Long) -> Unit,
    onApplySongMetadata: (songId: Long) -> Unit,
    onDiscardSongMetadata: (songId: Long) -> Unit,
    onRevertSongMetadata: (songId: Long) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var songToAdd by remember { mutableStateOf<Song?>(null) }
    val context = LocalContext.current

    if (songs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No songs found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongItem(
                    song = song,
                    isPlaying = currentSong?.id == song.id,
                    onClick = { onSongClick(song) },
                    onMoreClick = { songToAdd = song }
                )
            }
        }
    }

    songToAdd?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            isMetadataLookupRunning = metadataLookupSongId == song.id,
            pendingMetadataSuggestion = pendingMetadataSuggestion?.takeIf { it.songId == song.id },
            hasAppliedMetadataOverride = song.id in metadataOverriddenSongIds,
            onDismiss = { songToAdd = null },
            onSelectPlaylist = { playlist ->
                onAddSongToPlaylist(song.id, playlist.id)
                Toast.makeText(
                    context,
                    "Song added",
                    Toast.LENGTH_SHORT
                ).show()
                songToAdd = null
            },
            onCreatePlaylist = { name ->
                onCreatePlaylistAndAdd(name, song.id)
                Toast.makeText(
                    context,
                    "Song added",
                    Toast.LENGTH_SHORT
                ).show()
                songToAdd = null
            },
            onRenameSong = { newTitle ->
                onRenameSong(song.id, newTitle)
                Toast.makeText(
                    context,
                    "Song renamed",
                    Toast.LENGTH_SHORT
                ).show()
                songToAdd = null
            },
            onLookupMetadata = { onLookupSongMetadata(song.id) },
            onApplyMetadata = {
                onApplySongMetadata(song.id)
                songToAdd = null
            },
            onDiscardMetadata = { onDiscardSongMetadata(song.id) },
            onRevertMetadata = { onRevertSongMetadata(song.id) }
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    song: Song,
    playlists: List<Playlist>,
    isMetadataLookupRunning: Boolean,
    pendingMetadataSuggestion: SongMetadataSuggestion?,
    hasAppliedMetadataOverride: Boolean,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenameSong: (String) -> Unit,
    onLookupMetadata: () -> Unit,
    onApplyMetadata: () -> Unit,
    onDiscardMetadata: () -> Unit,
    onRevertMetadata: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        NewPlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            }
        )
    } else if (showRenameDialog) {
        RenameSongDialog(
            initialName = song.displayTitle(),
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                onRenameSong(newName)
                showRenameDialog = false
            }
        )
    } else if (pendingMetadataSuggestion != null) {
        MetadataPreviewDialog(
            suggestion = pendingMetadataSuggestion,
            onApply = onApplyMetadata,
            onDiscard = onDiscardMetadata,
            onDismiss = onDismiss
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Song options") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Song: ${song.displayTitle().trim().ifBlank { "X" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRenameDialog = true }
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Rename song",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isMetadataLookupRunning, onClick = onLookupMetadata)
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isMetadataLookupRunning) "Searching metadata..." else "Find metadata",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isMetadataLookupRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    if (hasAppliedMetadataOverride) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRevertMetadata() }
                                .padding(vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Revert metadata",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateDialog = true }
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "New playlist",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    if (playlists.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        playlists.forEach { playlist ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlaylist(playlist) }
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "${playlist.getSongIdList().size} songs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MetadataPreviewDialog(
    suggestion: SongMetadataSuggestion,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Metadata preview") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Current",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = suggestion.originalTitle.trim().ifBlank { "X" },
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = suggestion.originalArtist.trim().ifBlank { "X" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = suggestion.originalAlbum.trim().ifBlank { "X" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.size(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Suggested",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = suggestion.suggestedTitle.trim().ifBlank { "X" },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = suggestion.suggestedArtist.trim().ifBlank { "X" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = suggestion.suggestedAlbum.trim().ifBlank { "X" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        AlbumArt(
                            model = suggestion.suggestedArtworkUri,
                            contentDescription = "Suggested artwork",
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(10.dp),
                            placeholderIconSize = 18.dp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onApply) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard")
            }
        }
    )
}

@Composable
private fun NewPlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RenameSongDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var nameField by remember(initialName) {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(initialName.length)
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename song") },
        text = {
            OutlinedTextField(
                value = nameField,
                onValueChange = { nameField = it },
                label = { Text("New name") },
                singleLine = false,
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanName = nameField.text.trim()
                    if (cleanName.isNotBlank()) onRename(cleanName)
                },
                enabled = nameField.text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
