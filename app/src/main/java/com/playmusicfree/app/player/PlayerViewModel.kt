package com.playmusicfree.app.player

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.playmusicfree.app.PlayMusicFreeApp
import com.playmusicfree.app.data.model.Playlist
import com.playmusicfree.app.data.model.Song
import com.playmusicfree.app.data.remote.ItunesMetadataLookup
import com.playmusicfree.app.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private data class SongMetadataOverride(
        val title: String?,
        val artist: String?,
        val album: String?,
        val artworkUri: Uri?
    )

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

    private val _isMetadataLookupRunning = MutableStateFlow(false)
    val isMetadataLookupRunning: StateFlow<Boolean> = _isMetadataLookupRunning.asStateFlow()

    private val _hasPendingMetadataForCurrentSong = MutableStateFlow(false)
    val hasPendingMetadataForCurrentSong: StateFlow<Boolean> =
        _hasPendingMetadataForCurrentSong.asStateFlow()

    private val _hasAppliedMetadataForCurrentSong = MutableStateFlow(false)
    val hasAppliedMetadataForCurrentSong: StateFlow<Boolean> =
        _hasAppliedMetadataForCurrentSong.asStateFlow()

    private val _metadataLookupEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val metadataLookupEvents: SharedFlow<String> = _metadataLookupEvents

    // Scan settings state
    private val _minDurationSeconds = MutableStateFlow(repository.scanPreferences.minDurationSeconds)
    val minDurationSeconds: StateFlow<Int> = _minDurationSeconds.asStateFlow()

    private val _availableFolders = MutableStateFlow<List<String>>(emptyList())
    val availableFolders: StateFlow<List<String>> = _availableFolders.asStateFlow()

    private val _excludedFolders = MutableStateFlow(repository.scanPreferences.excludedFolders)
    val excludedFolders: StateFlow<Set<String>> = _excludedFolders.asStateFlow()

    private var mediaController: MediaController? = null
    private var playerListener: Player.Listener? = null
    private var positionUpdateJob: Job? = null
    private var sourceSongs: List<Song> = emptyList()
    private var allSongs: List<Song> = emptyList()
    private var customSongTitles: Map<Long, String> = emptyMap()
    private val pendingMetadataOverrides = mutableMapOf<Long, SongMetadataOverride>()
    private val appliedMetadataOverrides = mutableMapOf<Long, SongMetadataOverride>()

    init {
        if (hasReadMediaAudioPermission()) {
            refreshLibrary()
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            val loaded = runCatching { repository.loadSongs() }.getOrDefault(emptyList())
            sourceSongs = loaded
            customSongTitles = repository.getCustomSongTitles()
            allSongs = sourceSongs.map(::applyMetadataOverride)
            _songs.value = allSongs
            syncCurrentSongFromController()
        }
    }

    private fun loadAvailableFolders() {
        viewModelScope.launch {
            _availableFolders.value = runCatching { repository.getAvailableFolders() }
                .getOrDefault(emptyList())
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
        if (mediaController === controller) return

        mediaController?.let { currentController ->
            playerListener?.let(currentController::removeListener)
        }
        positionUpdateJob?.cancel()

        mediaController = controller
        if (controller == null) {
            _isPlaying.value = false
            _currentPosition.value = 0L
            return
        }

        if (playerListener == null) {
            playerListener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val id = mediaItem?.mediaId?.toLongOrNull()
                    _currentSong.value = allSongs.find { it.id == id }
                    updateCurrentMetadataActionState()
                }

                override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                    _shuffleEnabled.value = enabled
                }

                override fun onRepeatModeChanged(mode: Int) {
                    _repeatMode.value = mode
                }
            }
        }

        playerListener?.let(controller::addListener)
        _isPlaying.value = controller.isPlaying
        _shuffleEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode
        _currentPosition.value = controller.currentPosition
        syncCurrentSongFromController()
        startPositionUpdater()
    }

    fun onAudioPermissionChanged(granted: Boolean) {
        if (granted) {
            refreshLibrary()
        } else {
            sourceSongs = emptyList()
            allSongs = emptyList()
            customSongTitles = emptyMap()
            _songs.value = emptyList()
            _availableFolders.value = emptyList()
            _currentSong.value = null
            pendingMetadataOverrides.clear()
            appliedMetadataOverrides.clear()
            updateCurrentMetadataActionState()
        }
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

    fun renameSongTitle(songId: Long, newTitle: String) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isBlank()) return

        viewModelScope.launch {
            repository.setCustomSongTitle(songId, cleanTitle)
            customSongTitles = repository.getCustomSongTitles()
            refreshSongsWithOverrides()
        }
    }

    fun lookupMetadataForCurrentSong() {
        if (_isMetadataLookupRunning.value) return
        val song = _currentSong.value
        if (song == null) {
            _metadataLookupEvents.tryEmit("No song playing")
            return
        }

        viewModelScope.launch {
            _isMetadataLookupRunning.value = true
            try {
                val lookup = runCatching {
                    withContext(Dispatchers.IO) {
                        ItunesMetadataLookup.searchSong(song.title, song.artist)
                    }
                }.getOrNull()

                if (lookup == null) {
                    _metadataLookupEvents.tryEmit("No metadata found")
                    return@launch
                }

                val metadataOverride = SongMetadataOverride(
                    title = lookup.title,
                    artist = lookup.artist,
                    album = lookup.album,
                    artworkUri = lookup.artworkUrl?.let(Uri::parse)
                )
                pendingMetadataOverrides[song.id] = metadataOverride
                updateCurrentMetadataActionState()
                _metadataLookupEvents.tryEmit("Suggestion ready")
            } finally {
                _isMetadataLookupRunning.value = false
            }
        }
    }

    fun applyPendingMetadataForCurrentSong() {
        val songId = _currentSong.value?.id ?: run {
            _metadataLookupEvents.tryEmit("No song playing")
            return
        }
        val pending = pendingMetadataOverrides.remove(songId) ?: run {
            _metadataLookupEvents.tryEmit("No suggestion")
            return
        }
        appliedMetadataOverrides[songId] = pending
        refreshSongsWithOverrides()
        _metadataLookupEvents.tryEmit("Metadata applied")
    }

    fun discardPendingMetadataForCurrentSong() {
        val songId = _currentSong.value?.id ?: run {
            _metadataLookupEvents.tryEmit("No song playing")
            return
        }
        if (pendingMetadataOverrides.remove(songId) != null) {
            updateCurrentMetadataActionState()
            _metadataLookupEvents.tryEmit("Suggestion discarded")
        } else {
            _metadataLookupEvents.tryEmit("No suggestion")
        }
    }

    fun revertMetadataForCurrentSong() {
        val songId = _currentSong.value?.id ?: run {
            _metadataLookupEvents.tryEmit("No song playing")
            return
        }
        val removedApplied = appliedMetadataOverrides.remove(songId) != null
        val removedPending = pendingMetadataOverrides.remove(songId) != null
        if (removedApplied || removedPending) {
            refreshSongsWithOverrides()
            _metadataLookupEvents.tryEmit("Metadata reverted")
        } else {
            _metadataLookupEvents.tryEmit("Nothing to revert")
        }
    }

    fun getSongsForPlaylist(playlist: Playlist): List<Song> {
        val ids = playlist.getSongIdList()
        return allSongs.filter { it.id in ids }
    }

    private fun startPositionUpdater() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                _currentPosition.value = mediaController?.currentPosition ?: 0L
                delay(500)
            }
        }
    }

    override fun onCleared() {
        mediaController?.let { controller ->
            playerListener?.let(controller::removeListener)
        }
        positionUpdateJob?.cancel()
        super.onCleared()
    }

    private fun refreshLibrary() {
        loadSongs()
        loadAvailableFolders()
    }

    private fun syncCurrentSongFromController() {
        val id = mediaController?.currentMediaItem?.mediaId?.toLongOrNull()
        _currentSong.value = allSongs.find { it.id == id }
        updateCurrentMetadataActionState()
    }

    private fun refreshSongsWithOverrides() {
        allSongs = sourceSongs.map(::applyMetadataOverride)
        _songs.value = allSongs
        _currentSong.value = _currentSong.value?.let { current ->
            allSongs.find { it.id == current.id } ?: current
        }
        updateCurrentMetadataActionState()
    }

    private fun applyMetadataOverride(song: Song): Song {
        val metadataOverride = appliedMetadataOverrides[song.id]
        val songWithMetadata = if (metadataOverride == null) {
            song
        } else {
            song.copy(
                title = metadataOverride.title ?: song.title,
                artist = metadataOverride.artist ?: song.artist,
                album = metadataOverride.album ?: song.album,
                albumArtUri = metadataOverride.artworkUri ?: song.albumArtUri
            )
        }

        val customTitle = customSongTitles[song.id]?.trim().orEmpty()
        return if (customTitle.isBlank()) {
            songWithMetadata
        } else {
            songWithMetadata.copy(title = customTitle)
        }
    }

    private fun updateCurrentMetadataActionState() {
        val songId = _currentSong.value?.id
        _hasPendingMetadataForCurrentSong.value =
            songId != null && pendingMetadataOverrides.containsKey(songId)
        _hasAppliedMetadataForCurrentSong.value =
            songId != null && appliedMetadataOverrides.containsKey(songId)
    }

    private fun hasReadMediaAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
}
