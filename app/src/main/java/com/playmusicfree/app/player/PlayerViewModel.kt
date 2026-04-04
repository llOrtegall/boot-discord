package com.playmusicfree.app.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.playmusicfree.app.PlayMusicFreeApp
import com.playmusicfree.app.data.model.Playlist
import com.playmusicfree.app.data.model.Song
import com.playmusicfree.app.data.repository.MusicRepository
import com.playmusicfree.app.data.local.ScanPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PlayMusicFreeApp
    private val repository = MusicRepository(
        application,
        app.database.playlistDao(),
        app.scanPreferences
    )

    // Songs
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    // Playlists
    val playlists = repository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player state
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // Scan settings state
    private val _minDurationSeconds = MutableStateFlow(repository.scanPreferences.minDurationSeconds)
    val minDurationSeconds: StateFlow<Int> = _minDurationSeconds.asStateFlow()

    private val _availableFolders = MutableStateFlow<List<String>>(emptyList())
    val availableFolders: StateFlow<List<String>> = _availableFolders.asStateFlow()

    private val _excludedFolders = MutableStateFlow(repository.scanPreferences.excludedFolders)
    val excludedFolders: StateFlow<Set<String>> = _excludedFolders.asStateFlow()

    private var mediaController: MediaController? = null
    private var allSongs: List<Song> = emptyList()

    init {
        loadSongs()
        loadAvailableFolders()
    }

    fun loadSongs() {
        viewModelScope.launch {
            val loaded = repository.loadSongs()
            allSongs = loaded
            _songs.value = loaded
        }
    }

    private fun loadAvailableFolders() {
        viewModelScope.launch {
            _availableFolders.value = repository.getAvailableFolders()
        }
    }

    fun setMinDuration(seconds: Int) {
        repository.scanPreferences.minDurationSeconds = seconds
        _minDurationSeconds.value = seconds
        loadSongs()
    }

    fun toggleFolderExclusion(folder: String) {
        val current = _excludedFolders.value.toMutableSet()
        if (folder in current) current.remove(folder) else current.add(folder)
        repository.scanPreferences.excludedFolders = current
        _excludedFolders.value = current
        loadSongs()
    }

    fun setMediaController(controller: MediaController?) {
        mediaController = controller
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId?.toLongOrNull()
                _currentSong.value = allSongs.find { it.id == id }
            }

            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                _shuffleEnabled.value = enabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                _repeatMode.value = mode
            }
        })
        startPositionUpdater()
    }

    fun playSong(song: Song, queue: List<Song> = allSongs) {
        val controller = mediaController ?: return
        val mediaItems = queue.map { s ->
            MediaItem.Builder()
                .setMediaId(s.id.toString())
                .setUri(s.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .setArtworkUri(s.albumArtUri)
                        .build()
                )
                .build()
        }
        val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun seekTo(position: Long) { mediaController?.seekTo(position) }
    fun skipNext() { mediaController?.seekToNextMediaItem() }
    fun skipPrevious() { mediaController?.seekToPreviousMediaItem() }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun toggleRepeat() {
        val controller = mediaController ?: return
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch { repository.deletePlaylist(playlist) }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.addSongToPlaylist(playlistId, songId) }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(name)
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun getSongsForPlaylist(playlist: Playlist): List<Song> {
        val ids = playlist.getSongIdList()
        return allSongs.filter { it.id in ids }
    }

    private fun startPositionUpdater() {
        viewModelScope.launch {
            while (isActive) {
                _currentPosition.value = mediaController?.currentPosition ?: 0L
                delay(500)
            }
        }
    }
}
