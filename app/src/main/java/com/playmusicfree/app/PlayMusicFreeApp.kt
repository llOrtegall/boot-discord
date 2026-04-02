package com.playmusicfree.app

import android.app.Application
import com.playmusicfree.app.data.local.MusicDatabase
import com.playmusicfree.app.data.local.ScanPreferences

class PlayMusicFreeApp : Application() {

    lateinit var database: MusicDatabase
        private set

    lateinit var scanPreferences: ScanPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = MusicDatabase.create(this)
        scanPreferences = ScanPreferences(this)
    }
}
