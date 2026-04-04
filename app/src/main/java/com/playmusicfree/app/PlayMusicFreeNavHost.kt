package com.playmusicfree.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.playmusicfree.app.player.PlayerViewModel
import com.playmusicfree.app.ui.components.MiniPlayer
import com.playmusicfree.app.ui.screens.HomeScreen
import com.playmusicfree.app.ui.screens.PlayerScreen
import com.playmusicfree.app.ui.screens.PlaylistDetailScreen
import com.playmusicfree.app.ui.screens.PlaylistScreen
import com.playmusicfree.app.ui.screens.SettingsScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Songs("songs", "Songs", Icons.Default.MusicNote),
    Playlists("playlists", "Playlists", Icons.Default.LibraryMusic),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayMusicFreeNavHost(mediaController: MediaController?) {
    val viewModel: PlayerViewModel = viewModel()
    val navController = rememberNavController()

    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val availableFolders by viewModel.availableFolders.collectAsState()
    val excludedFolders by viewModel.excludedFolders.collectAsState()
    val minDurationSeconds by viewModel.minDurationSeconds.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isMainScreen = currentRoute in listOf(Tab.Songs.route, Tab.Playlists.route)

    LaunchedEffect(mediaController) {
        viewModel.setMediaController(mediaController)
    }

    Scaffold(
        topBar = {
            if (isMainScreen) {
                TopAppBar(
                    title = { Text("Play Music Free") },
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (isMainScreen) {
                Column {
                    currentSong?.let { song ->
                        val progress = if (song.duration > 0)
                            (currentPosition.toFloat() / song.duration).coerceIn(0f, 1f) else 0f
                        MiniPlayer(
                            song = song,
                            isPlaying = isPlaying,
                            progress = progress,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipNext,
                            onClick = { navController.navigate("player") }
                        )
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Songs.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Tab.Songs.route) {
                HomeScreen(
                    songs = songs,
                    currentSong = currentSong,
                    playlists = playlists,
                    onSongClick = { viewModel.playSong(it) },
                    onAddSongToPlaylist = { songId, playlistId ->
                        viewModel.addSongToPlaylist(playlistId, songId)
                    },
                    onCreatePlaylistAndAdd = { name, songId ->
                        viewModel.createPlaylistAndAddSong(name, songId)
                    }
                )
            }

            composable(Tab.Playlists.route) {
                PlaylistScreen(
                    playlists = playlists,
                    onPlaylistClick = { playlist ->
                        navController.navigate("playlist/${playlist.id}")
                    },
                    onCreatePlaylist = viewModel::createPlaylist,
                    onDeletePlaylist = viewModel::deletePlaylist
                )
            }

            composable("playlist/{playlistId}") { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull()
                val playlist = playlists.find { it.id == playlistId }
                if (playlist != null) {
                    val playlistSongs = viewModel.getSongsForPlaylist(playlist)
                    PlaylistDetailScreen(
                        playlistName = playlist.name,
                        songs = playlistSongs,
                        currentSong = currentSong,
                        onSongClick = { song -> viewModel.playSong(song, playlistSongs) },
                        onBack = { navController.popBackStack() },
                        onRemoveSong = { song ->
                            viewModel.removeSongFromPlaylist(playlist.id, song.id)
                        }
                    )
                }
            }

            composable("player") {
                PlayerScreen(
                    song = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    onBack = { navController.popBackStack() },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onSeek = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onToggleRepeat = viewModel::toggleRepeat
                )
            }

            composable("settings") {
                SettingsScreen(
                    availableFolders = availableFolders,
                    excludedFolders = excludedFolders,
                    minDurationSeconds = minDurationSeconds,
                    onToggleFolder = viewModel::toggleFolderExclusion,
                    onSetMinDuration = viewModel::setMinDuration,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
