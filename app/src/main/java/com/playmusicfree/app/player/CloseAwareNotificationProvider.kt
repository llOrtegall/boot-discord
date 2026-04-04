package com.playmusicfree.app.player

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

@UnstableApi
class CloseAwareNotificationProvider(
    context: android.content.Context,
    private val closeAction: String
) : DefaultMediaNotificationProvider(context) {

    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        showPauseButton: Boolean
    ): ImmutableList<CommandButton> {
        val buttons = super.getMediaButtons(
            session,
            playerCommands,
            mediaButtonPreferences,
            showPauseButton
        )

        var compactIndex = 0
        fun markCompact(button: CommandButton?) {
            if (button == null) return
            button.extras.putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, compactIndex++)
        }

        markCompact(buttons.firstOrNull { it.playerCommand == Player.COMMAND_PLAY_PAUSE })
        markCompact(
            buttons.firstOrNull {
                it.playerCommand == Player.COMMAND_SEEK_TO_NEXT ||
                    it.playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
            }
        )
        markCompact(buttons.firstOrNull { it.sessionCommand?.customAction == closeAction })

        return buttons
    }
}
