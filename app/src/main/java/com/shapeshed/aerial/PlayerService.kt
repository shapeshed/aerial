package com.shapeshed.aerial

import android.app.PendingIntent
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
import com.shapeshed.aerial.data.Provider
import com.shapeshed.aerial.data.NowPlayingInfo
import com.shapeshed.aerial.data.NowPlayingStore
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.parseIcyTitle
import com.shapeshed.aerial.ui.ENRICH_METADATA_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    private var activeEnricher: Provider? = null
    private var lastAppliedNowPlayingSignature: String? = null
    private var enrichMetadataEnabled = false

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
        serviceScope.launch {
            NowPlayingStore.state.collectLatest { info ->
                applyNowPlayingInfo(info)
            }
        }
        serviceScope.launch {
            dataStore.data
                .map { it[ENRICH_METADATA_KEY] ?: false }
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    enrichMetadataEnabled = enabled
                    if (!enabled) {
                        activeEnricher?.stop()
                        activeEnricher = null
                    } else {
                        currentStation()?.let { station ->
                            val enricher = (application as AerialApp).providers.firstOrNull { it.canEnrich(station) }
                            activeEnricher = enricher
                            if (player.isPlaying) enricher?.start(station, serviceScope)
                        }
                    }
                }
        }
    }

    private val icyListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            log("onIsPlayingChanged=$isPlaying")
            if (isPlaying) {
                currentStation()?.let { activeEnricher?.start(it, serviceScope) }
            } else {
                activeEnricher?.pause()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            log("onMediaItemTransition reason=$reason mediaId=${mediaItem?.mediaId}")
            lastIcyTitle = null
            lastAppliedNowPlayingSignature = null
            activeEnricher?.stop()
            activeEnricher = null
            if (enrichMetadataEnabled) {
                stationForMediaItem(mediaItem)?.let { station ->
                    val enricher = (application as AerialApp).providers.firstOrNull { it.canEnrich(station) }
                    activeEnricher = enricher
                    enricher?.start(station, serviceScope)
                }
            }
            updateFavoriteButton()
        }

        @OptIn(UnstableApi::class)
        override fun onMetadata(metadata: Metadata) {
            for (i in 0 until metadata.length()) {
                val entry = metadata[i]
                log("onMetadata type=${entry::class.simpleName}")
                if (entry is IcyInfo) {
                    val title = entry.title?.trim()
                    log("ICY title=$title")
                    if (title.isNullOrEmpty()) return
                    if (title == lastIcyTitle) return
                    lastIcyTitle = title
                    icyMetadataGeneration += 1
                    val item = player.currentMediaItem ?: return
                    val station = currentStation()
                    val stationName = station?.name ?: item.mediaMetadata.title?.toString().orEmpty()
                    val (icyArtist, icyTrackTitle) = parseIcyTitle(title)
                    replaceCurrentMediaItem(
                        item,
                        index = player.currentMediaItemIndex,
                        stationName = stationName,
                        artist = icyArtist,
                        title = icyTrackTitle,
                        artworkData = item.mediaMetadata.artworkData,
                        artworkUri = item.mediaMetadata.artworkUri,
                    )
                    activeEnricher?.onIcyTitle(title)
                } else {
                    // Any non-ICY metadata may signal a track transition for enriched stations.
                    // (Note: BBC HLS currently drops ID3 timed metadata due to a Media3 bug,
                    // so this is a future-proofing hook rather than an active signal.)
                    activeEnricher?.notifyTransition()
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

    private fun replaceCurrentMediaItem(
        item: MediaItem,
        index: Int,
        stationName: String,
        artist: String? = null,
        title: String,
        artworkData: ByteArray? = null,
        artworkUri: Uri?,
    ) {
        val mediaMetadata = item.mediaMetadata.buildUpon()
            .setTitle(title)
            .setArtist(artist ?: stationName)
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

    private fun applyNowPlayingInfo(info: NowPlayingInfo?) {
        val currentMediaItem = player.currentMediaItem ?: return
        val currentStation = currentStation() ?: return
        if (info?.stationId != currentStation.id) return

        val artworkData = info.track?.artworkData ?: info.artworkData
        val signature = buildString {
            append(info.programmeTitle.orEmpty())
            append('|')
            append(info.programmeSubtitle.orEmpty())
            append('|')
            append(info.track?.artist.orEmpty())
            append('|')
            append(info.track?.title.orEmpty())
            append('|')
            append(System.identityHashCode(artworkData))
        }
        if (signature == lastAppliedNowPlayingSignature) return
        lastAppliedNowPlayingSignature = signature

        val mediaMetadata = currentMediaItem.mediaMetadata.buildUpon()
            .setTitle(info.track?.artist ?: info.programmeTitle ?: currentMediaItem.mediaMetadata.title)
            .setArtist(
                when {
                    info.track != null -> info.track.title
                    else -> info.programmeTitle ?: currentMediaItem.mediaMetadata.artist
                }
            )
            .setSubtitle(
                when {
                    info.track != null -> info.track.title
                    else -> info.programmeSubtitle ?: currentMediaItem.mediaMetadata.subtitle
                }
            )
            .apply {
                if (artworkData != null) {
                    setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            }
            .build()

        player.replaceMediaItem(
            player.currentMediaItemIndex,
            currentMediaItem.buildUpon().setMediaMetadata(mediaMetadata).build()
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        activeEnricher?.stop()
        activeEnricher = null
        player.removeListener(icyListener)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    private fun pendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val TAG = "AerialPlayerService"
        const val ACTION_TOGGLE_FAVORITE = "com.shapeshed.aerial.action.TOGGLE_FAVORITE"
    }
}
