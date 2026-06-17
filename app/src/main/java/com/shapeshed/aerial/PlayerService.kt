package com.shapeshed.aerial

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlayerService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(icyListener)
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent())
            .build()
    }

    private val icyListener = object : Player.Listener {
        @OptIn(UnstableApi::class)
        override fun onMetadata(metadata: Metadata) {
            for (i in 0 until metadata.length()) {
                val entry = metadata[i]
                if (entry is IcyInfo) {
                    val title = entry.title?.trim()
                    if (title.isNullOrEmpty()) return
                    val item = player.currentMediaItem ?: return
                    player.replaceMediaItem(
                        player.currentMediaItemIndex,
                        item.buildUpon().setMediaMetadata(
                            item.mediaMetadata.buildUpon()
                                .setArtist(title)
                                .setSubtitle(title)
                                .build()
                        ).build()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        player.removeListener(icyListener)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    private fun pendingIntent(): PendingIntent =
        TaskStackBuilder.create(this).run {
            addNextIntent(Intent(this@PlayerService, MainActivity::class.java))
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
}
