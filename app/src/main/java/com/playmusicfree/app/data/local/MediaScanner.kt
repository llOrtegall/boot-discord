package com.playmusicfree.app.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.playmusicfree.app.data.model.Song
import java.io.File

object MediaScanner {

    private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")

    fun scanSongs(
        context: Context,
        minDurationSeconds: Int = 0,
        excludedFolders: Set<String> = emptySet()
    ): List<Song> {
        val songs = mutableListOf<Song>()
        val minDurationMs = minDurationSeconds * 1000L

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val duration = cursor.getLong(durationCol)
                if (duration < minDurationMs) continue

                val filePath = cursor.getString(dataCol) ?: ""
                val folder = File(filePath).parent ?: ""
                if (excludedFolders.any { folder.startsWith(it) }) continue

                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)

                songs.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unknown",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        duration = duration,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        ),
                        albumArtUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
                    )
                )
            }
        }

        return songs
    }

    fun getAvailableFolders(context: Context): List<String> {
        val folders = mutableSetOf<String>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media.DATA),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null
        )?.use { cursor ->
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue
                val folder = File(path).parent ?: continue
                folders.add(folder)
            }
        }

        return folders.sorted()
    }
}
