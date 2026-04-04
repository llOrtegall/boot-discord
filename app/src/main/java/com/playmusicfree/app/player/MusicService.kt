package com.playmusicfree.app.player

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.playmusicfree.app.R

@UnstableApi
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val closeSessionCommand = SessionCommand(CLOSE_SESSION_ACTION, Bundle.EMPTY)

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(closeSessionCommand)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return if (customCommand.customAction == CLOSE_SESSION_ACTION) {
                stopPlaybackAndService()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            } else {
                super.onCustomCommand(session, controller, customCommand, args)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(CloseAwareNotificationProvider(this, CLOSE_SESSION_ACTION))

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handle audio focus
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val closeButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setSessionCommand(closeSessionCommand)
            .setDisplayName(getString(R.string.close_app))
            .setIconResId(R.drawable.ic_close_notification)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .setMediaButtonPreferences(listOf(closeButton))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0
            || player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun stopPlaybackAndService() {
        mediaSession?.player?.run {
            playWhenReady = false
            stop()
            clearMediaItems()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private companion object {
        const val CLOSE_SESSION_ACTION = "com.playmusicfree.app.ACTION_CLOSE_SESSION"
    }
}
