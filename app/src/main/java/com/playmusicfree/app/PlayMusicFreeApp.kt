package com.playmusicfree.app

import android.app.Application
import com.playmusicfree.app.data.local.MusicDatabase

class PlayMusicFreeApp : Application() {

    lateinit var database: MusicDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = MusicDatabase.create(this)
    }
}
