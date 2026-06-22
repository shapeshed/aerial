package com.shapeshed.aerial

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class PlayerService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val favoriteCommand = SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)
    private val seekToStartCommand = SessionCommand(ACTION_SEEK_TO_START, Bundle.EMPTY)
    private val seekToLiveCommand = SessionCommand(ACTION_SEEK_TO_LIVE, Bundle.EMPTY)

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var repository: StationRepository
    private var stations: List<Station> = emptyList()
    private var lastIcyTitle: String? = null
    private var isSeekable = false
    private var isAtLive = true
    private var liveOffsetJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().also {
                it.setSmallIcon(R.drawable.ic_notification)
            }
        )
        repository = (application as AerialApp).repository
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(30_000)
            .setSeekForwardIncrementMs(30_000)
            .build()
            .apply { enableAudioOffload() }
        player.addListener(icyListener)
        // Wrap the player so Media3's legacy bridge sees seekable DVR streams as non-live.
        // Without this, isCurrentMediaItemLive() == true causes the bridge to skip
        // ACTION_SEEK_TO, so the seekbar never appears in system media controls.
        val sessionPlayer = object : ForwardingPlayer(player) {
            override fun isCurrentMediaItemLive(): Boolean =
                if (isCurrentMediaItemSeekable) false else super.isCurrentMediaItemLive()
        }
        mediaSession = MediaSession.Builder(this, sessionPlayer)
            .setSessionActivity(pendingIntent())
            .setCallback(sessionCallback)
            .setMediaButtonPreferences(listOf(favoriteButton(null)))
            .build()
        serviceScope.launch {
            repository.getAll().collectLatest { updatedStations ->
                stations = updatedStations
                updateMediaButtonPreferences()
            }
        }
    }

    private val icyListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            lastIcyTitle = null
            isSeekable = false
            isAtLive = true
            liveOffsetJob?.cancel()
            updateMediaButtonPreferences()
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                val seekable = player.isCurrentMediaItemSeekable
                if (seekable != isSeekable) {
                    isSeekable = seekable
                    updateMediaButtonPreferences()
                }
                if (isSeekable && player.isPlaying) startLiveOffsetTracking()
            } else if (state == Player.STATE_IDLE || state == Player.STATE_ENDED) {
                liveOffsetJob?.cancel()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying && isSeekable) startLiveOffsetTracking() else liveOffsetJob?.cancel()
        }

        @OptIn(UnstableApi::class)
        override fun onMetadata(metadata: Metadata) {
            for (i in 0 until metadata.length()) {
                val entry = metadata[i]
                if (entry is IcyInfo) {
                    val title = entry.title?.trim()
                    if (title.isNullOrEmpty()) return
                    if (title == lastIcyTitle) return
                    val item = player.currentMediaItem ?: return
                    lastIcyTitle = title
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

    private fun startLiveOffsetTracking() {
        liveOffsetJob?.cancel()
        liveOffsetJob = serviceScope.launch {
            while (true) {
                delay(2_000)
                val nowAtLive = player.currentLiveOffset < 5_000L
                if (nowAtLive != isAtLive) {
                    isAtLive = nowAtLive
                    updateMediaButtonPreferences()
                }
            }
        }
    }

    private fun ExoPlayer.enableAudioOffload() {
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setAudioOffloadPreferences(
                AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                    .build()
            )
            .build()
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(favoriteCommand)
                        .add(seekToStartCommand)
                        .add(seekToLiveCommand)
                        .build()
                )
                .setAvailablePlayerCommands(
                    Player.Commands.Builder()
                        .addAllCommands()
                        .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .build()
                )
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_SEEK_TO_START) {
                player.seekTo(0L)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == ACTION_SEEK_TO_LIVE) {
                player.seekToDefaultPosition()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction != ACTION_TOGGLE_FAVORITE) {
                return super.onCustomCommand(session, controller, customCommand, args)
            }
            val station = currentStation()
                ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
            serviceScope.launch {
                val updatedStation = station.copy(isFavorite = !station.isFavorite)
                withContext(Dispatchers.IO) {
                    repository.update(updatedStation)
                }
                stations = stations.map { if (it.id == updatedStation.id) updatedStation else it }
                updateMediaButtonPreferences()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun updateMediaButtonPreferences() {
        if (!::mediaSession.isInitialized) return
        val buttons = buildList {
            if (isSeekable) {
                add(seekBackButton())
                add(seekForwardButton())
                add(seekToStartButton())
                if (!isAtLive) add(seekToLiveButton())
            } else {
                add(favoriteButton(currentStation()))
            }
        }
        mediaSession.setMediaButtonPreferences(buttons)
    }

    private fun seekBackButton() = CommandButton.Builder(CommandButton.ICON_SKIP_BACK_30)
        .setPlayerCommand(Player.COMMAND_SEEK_BACK)
        .setDisplayName("Rewind 30s")
        .build()

    private fun seekForwardButton() = CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
        .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
        .setDisplayName("Skip forward 30s")
        .build()

    private fun seekToStartButton() = CommandButton.Builder(CommandButton.ICON_PREVIOUS)
        .setSessionCommand(seekToStartCommand)
        .setDisplayName("Start")
        .build()

    private fun seekToLiveButton() = CommandButton.Builder(CommandButton.ICON_NEXT)
        .setSessionCommand(seekToLiveCommand)
        .setDisplayName("Live")
        .build()

    private fun favoriteButton(station: Station?): CommandButton {
        val isFavorite = station?.isFavorite == true
        return CommandButton.Builder(
            if (isFavorite) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED
        )
            .setDisplayName(if (isFavorite) "Remove from favorites" else "Add to favorites")
            .setEnabled(station != null)
            .setSessionCommand(favoriteCommand)
            .build()
    }

    private fun currentStation(): Station? {
        val id = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return null
        return stations.firstOrNull { it.id == id }
    }

    override fun onDestroy() {
        serviceScope.cancel()
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

    private companion object {
        const val ACTION_TOGGLE_FAVORITE = "com.shapeshed.aerial.action.TOGGLE_FAVORITE"
        const val ACTION_SEEK_TO_START = "com.shapeshed.aerial.action.SEEK_TO_START"
        const val ACTION_SEEK_TO_LIVE = "com.shapeshed.aerial.action.SEEK_TO_LIVE"
    }
}
