package com.playmusicfree.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.playmusicfree.app.data.model.Playlist
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAll(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    @Update
    suspend fun update(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)
}
