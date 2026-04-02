package com.playmusicfree.app.data.repository

import android.content.Context
import com.playmusicfree.app.data.local.MediaScanner
import com.playmusicfree.app.data.local.PlaylistDao
import com.playmusicfree.app.data.model.Playlist
import com.playmusicfree.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val playlistDao: PlaylistDao
) {

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        MediaScanner.scanSongs(context)
    }

    fun getPlaylists(): Flow<List<Playlist>> = playlistDao.getAll()

    suspend fun getPlaylistById(id: Long): Playlist? = playlistDao.getById(id)

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insert(Playlist(name = name))

    suspend fun updatePlaylist(playlist: Playlist) =
        playlistDao.update(playlist)

    suspend fun deletePlaylist(playlist: Playlist) =
        playlistDao.delete(playlist)

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val playlist = playlistDao.getById(playlistId) ?: return
        playlistDao.update(playlist.withSongId(songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        val playlist = playlistDao.getById(playlistId) ?: return
        playlistDao.update(playlist.withoutSongId(songId))
    }
}
