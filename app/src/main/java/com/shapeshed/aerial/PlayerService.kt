package com.shapeshed.aerial

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
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
import com.shapeshed.aerial.data.BbcNowPlayingState
import com.shapeshed.aerial.data.BbcNowPlayingStore
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.fetchBbcNowPlaying
import com.shapeshed.aerial.data.isBbcStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class PlayerService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val favoriteCommand = SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var repository: StationRepository
    private var stations: List<Station> = emptyList()
    private var lastIcyTitle: String? = null
    private var icyMetadataGeneration: Int = 0
    private var bbcNowPlayingJob: Job? = null
    private var bbcRefreshJob: Job? = null

    private fun log(message: String) {
        Log.d(TAG, message)
    }

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
            .build()
            .apply { enableAudioOffload() }
        player.addListener(icyListener)
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent())
            .setCallback(sessionCallback)
            .setMediaButtonPreferences(listOf(favoriteButton(null)))
            .build()
        log("onCreate")
        serviceScope.launch {
            repository.getAll().collectLatest { updatedStations ->
                stations = updatedStations
                updateFavoriteButton()
            }
        }
    }

    private val icyListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            log("onIsPlayingChanged=$isPlaying")
            if (!isPlaying) {
                stopBbcNowPlayingRefresh()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            log("onMediaItemTransition reason=$reason mediaId=${mediaItem?.mediaId}")
            lastIcyTitle = null
            stationForMediaItem(mediaItem)?.let { station ->
                if (isBbcStation(station)) {
                    BbcNowPlayingStore.set(null)
                    scheduleBbcNowPlayingRefresh(station)
                } else {
                    BbcNowPlayingStore.set(null)
                    stopBbcNowPlayingRefresh()
                }
            }
            updateFavoriteButton()
        }

        @OptIn(UnstableApi::class)
        override fun onMetadata(metadata: Metadata) {
            for (i in 0 until metadata.length()) {
                val entry = metadata[i]
                if (entry is IcyInfo) {
                    val title = entry.title?.trim()
                    log("ICY metadata title=$title entry=$entry")
                    if (title.isNullOrEmpty()) return
                    if (title == lastIcyTitle) return
                    val item = player.currentMediaItem ?: return
                    val station = currentStation()
                    if (station != null && isBbcStation(station)) {
                        updateBbcTrackTitle(station.id, title)
                        return
                    }
                    lastIcyTitle = title
                    icyMetadataGeneration += 1
                    replaceCurrentMediaItem(
                        item,
                        index = player.currentMediaItemIndex,
                        stationName = station?.name ?: item.mediaMetadata.title?.toString().orEmpty(),
                        title = title,
                        artworkData = item.mediaMetadata.artworkData,
                        artworkUri = item.mediaMetadata.artworkUri,
                    )
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
                updateFavoriteButton()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun updateFavoriteButton() {
        if (::mediaSession.isInitialized) {
            mediaSession.setMediaButtonPreferences(listOf(favoriteButton(currentStation())))
        }
    }

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

    private fun stationForMediaItem(mediaItem: MediaItem?): Station? {
        val id = mediaItem?.mediaId?.toLongOrNull() ?: return null
        return stations.firstOrNull { it.id == id }
    }

    private fun requestBbcNowPlayingUpdate(station: Station) {
        bbcNowPlayingJob?.cancel()
        bbcNowPlayingJob = serviceScope.launch(Dispatchers.IO) {
            log("BBC fetch start station=${station.name}")
            val info = fetchBbcNowPlaying(station) ?: return@launch
            withContext(Dispatchers.Main.immediate) {
                val currentItem = player.currentMediaItem ?: return@withContext
                if (currentItem.mediaId.toLongOrNull() != station.id) return@withContext
                log(
                    "BBC apply programme=${info.programmeTitle} track=${info.trackTitle} station=${station.name}"
                )
                BbcNowPlayingStore.set(
                    BbcNowPlayingState(
                        stationId = station.id,
                        programmeTitle = info.programmeTitle,
                        trackTitle = info.trackTitle,
                        artworkUrl = info.artworkUrl,
                        artworkData = info.artworkData,
                    )
                )
            }
        }
    }

    private fun scheduleBbcNowPlayingRefresh(station: Station) {
        if (!isBbcStation(station)) return
        log("BBC refresh job start station=${station.name}")
        bbcRefreshJob?.cancel()
        bbcRefreshJob = serviceScope.launch {
            requestBbcNowPlayingUpdate(station)
            delay(BBC_NOW_PLAYING_REFRESH_MS)
            val shouldRefreshAgain = withContext(Dispatchers.Main.immediate) {
                val currentStation = currentStation()
                currentStation?.id == station.id && player.isPlaying
            }
            if (shouldRefreshAgain) {
                requestBbcNowPlayingUpdate(station)
            }
        }
    }

    private fun updateBbcTrackTitle(stationId: Long, trackTitle: String) {
        BbcNowPlayingStore.update { current ->
            when {
                current == null -> BbcNowPlayingState(
                    stationId = stationId,
                    trackTitle = trackTitle,
                )
                current.stationId != stationId -> current
                current.trackTitle == trackTitle -> current
                else -> current.copy(trackTitle = trackTitle)
            }
        }
    }

    private fun stopBbcNowPlayingRefresh() {
        log("BBC refresh job stop")
        bbcRefreshJob?.cancel()
        bbcRefreshJob = null
        bbcNowPlayingJob?.cancel()
        bbcNowPlayingJob = null
    }

    private fun replaceCurrentMediaItem(
        item: MediaItem,
        index: Int,
        stationName: String,
        title: String,
        artworkData: ByteArray? = null,
        artworkUri: Uri?,
    ) {
        val mediaMetadata = item.mediaMetadata.buildUpon()
            .setTitle(title)
            .setArtist(stationName)
            .setSubtitle(title)
            .apply {
                if (artworkData != null) {
                    setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
                if (artworkUri != null) {
                    setArtworkUri(artworkUri)
                }
            }
            .build()
        player.replaceMediaItem(index, item.buildUpon().setMediaMetadata(mediaMetadata).build())
    }

    override fun onDestroy() {
        serviceScope.cancel()
        stopBbcNowPlayingRefresh()
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
        const val TAG = "AerialPlayerService"
        const val ACTION_TOGGLE_FAVORITE = "com.shapeshed.aerial.action.TOGGLE_FAVORITE"
        const val BBC_NOW_PLAYING_REFRESH_MS = 5_000L
    }
}
