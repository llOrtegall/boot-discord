package com.playmusicfree.app

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.playmusicfree.app.player.MusicService
import com.playmusicfree.app.ui.theme.PlayMusicFreeTheme

class MainActivity : ComponentActivity() {

    private var mediaController by mutableStateOf<MediaController?>(null)
    private var hasAudioPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasAudioPermission = hasPermission(Manifest.permission.READ_MEDIA_AUDIO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasAudioPermission = hasPermission(Manifest.permission.READ_MEDIA_AUDIO)
        requestPermissions()
        setContent {
            PlayMusicFreeTheme {
                PlayMusicFreeNavHost(
                    mediaController = mediaController,
                    hasAudioPermission = hasAudioPermission
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasAudioPermission = hasPermission(Manifest.permission.READ_MEDIA_AUDIO)
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
            { mediaController = controllerFuture.get() },
            MoreExecutors.directExecutor()
        )
    }

    override fun onStop() {
        super.onStop()
        mediaController?.release()
        mediaController = null
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (!hasPermission(Manifest.permission.READ_MEDIA_AUDIO)) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }
        if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
