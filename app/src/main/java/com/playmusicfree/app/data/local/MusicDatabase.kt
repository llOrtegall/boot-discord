package com.playmusicfree.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.playmusicfree.app.data.model.Playlist

@Database(entities = [Playlist::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    companion object {
        fun create(context: Context): MusicDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MusicDatabase::class.java,
                "play_music_free.db"
            ).build()
    }
}
