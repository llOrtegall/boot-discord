package com.playmusicfree.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val songIds: String = "" // comma-separated song IDs
) {
    fun getSongIdList(): List<Long> =
        if (songIds.isBlank()) emptyList()
        else songIds.split(",").mapNotNull { it.trim().toLongOrNull() }

    fun withSongId(songId: Long): Playlist {
        val ids = getSongIdList().toMutableList()
        if (songId !in ids) ids.add(songId)
        return copy(songIds = ids.joinToString(","))
    }

    fun withoutSongId(songId: Long): Playlist {
        val ids = getSongIdList().filter { it != songId }
        return copy(songIds = ids.joinToString(","))
    }
}
