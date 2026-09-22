package com.shapeshed.aerial

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.shapeshed.aerial.SHOW_HOME_KEY
import com.shapeshed.aerial.data.ACTION_SLEEP_TIMER_CANCEL
import com.shapeshed.aerial.data.ACTION_SLEEP_TIMER_SET
import com.shapeshed.aerial.data.ACTION_TOGGLE_FAVORITE
import com.shapeshed.aerial.data.AERIAL_CUSTOM_COMMAND_ACTIONS
import com.shapeshed.aerial.data.AERIAL_USER_AGENT
import com.shapeshed.aerial.data.FavoriteToggleAction
import com.shapeshed.aerial.data.MediaBrowseTree
import com.shapeshed.aerial.data.PlayHistoryEntry
import com.shapeshed.aerial.data.PlaybackSnapshotStore
import com.shapeshed.aerial.data.RECENT_ID
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.SLEEP_TIMER_DURATION_MS
import com.shapeshed.aerial.data.SleepTimerController
import com.shapeshed.aerial.data.SleepTimerStore
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationArtworkResolver
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.applyFavoriteToggleLocally
import com.shapeshed.aerial.data.favoriteToggleAction
import com.shapeshed.aerial.data.httpGetText
import com.shapeshed.aerial.data.parseTrackMetadata
import com.shapeshed.aerial.data.resolveStreamUrl
import com.shapeshed.aerial.data.streamMetadataFrames
import com.shapeshed.aerial.widget.WidgetPlaybackStore
import com.shapeshed.aerial.widget.requestAerialWidgetUpdate
import com.shapeshed.aerial.widget.widgetNavigationAvailability
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class PlayerService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val favoriteCommand = SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)
    private lateinit var sleepTimerController: SleepTimerController

    private lateinit var player: ExoPlayer
    private lateinit var sessionPlayer: Player
    private lateinit var mediaSession: MediaLibrarySession
    private lateinit var repository: StationRepository
    private lateinit var registryRepository: RegistryRepository
    private lateinit var artworkResolver: StationArtworkResolver
    private lateinit var mediaBrowseTree: MediaBrowseTree
    private lateinit var sessionCoordinator: PlaybackSessionCoordinator
    private val playbackSnapshotStore by lazy { PlaybackSnapshotStore(dataStore) }
    private var stations: List<Station> = emptyList()
    private var lastRecordedStationKey: String? = null
    private var lastIcyTitle: String? = null
    private var lastId3Title: String? = null
    private var pausedAtMs: Long? = null
    private val reconnectThrottle = ReconnectThrottle(RECONNECT_RETRY_COOLDOWN_MS, SystemClock::elapsedRealtime)

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().also {
                it.setSmallIcon(R.drawable.ic_notification)
            },
        )
        repository = (application as AerialApp).repository
        registryRepository = (application as AerialApp).registryRepository
        artworkResolver = StationArtworkResolver(registryRepository)
        mediaBrowseTree = MediaBrowseTree(this, repository, registryRepository)
        sessionCoordinator = PlaybackSessionCoordinator(
            context = this,
            browseTree = mediaBrowseTree,
            repository = repository,
            snapshotStore = playbackSnapshotStore,
            artworkResolver = artworkResolver,
        )
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(AERIAL_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(HTTP_TIMEOUT_MS)
            .setReadTimeoutMs(HTTP_TIMEOUT_MS)
        // Some stations' stream URLs point at a .pls/.m3u/.asx playlist file rather than the
        // audio itself. ExoPlayer can't play those containers, so unwrap them to the real
        // stream URL here — on the loader thread, right before the connection opens, so the
        // fetch is lazy (per item, including queue neighbours) and never touches the UI thread.
        // A non-playlist URL (the vast majority) passes through untouched with no network cost;
        // a playlist that can't be fetched or parsed also passes through, surfacing the normal
        // playback error. Note: a playlist resolving to HLS (.m3u8) won't switch ExoPlayer to
        // its HLS source type, since resolution happens below source selection — rare in practice.
        val playlistResolvingFactory = ResolvingDataSource.Factory(httpDataSourceFactory) { dataSpec ->
            val original = dataSpec.uri.toString()
            val resolved = resolveStreamUrl(original) { httpGetText(it) }
            if (resolved == original) dataSpec else dataSpec.withUri(resolved.toUri())
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(playlistResolvingFactory)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .build()
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            // Player's default seekToPrevious() (what notification and hardware/Bluetooth
            // "previous" calls use) restarts the current item when playback is beyond this
            // threshold. Live stations have no meaningful rewind position, so use an unlimited
            // threshold to make Back always move to the previous station in the queue.
            .setMaxSeekToPreviousPositionMs(Long.MAX_VALUE)
            .build()
        // Wraps skip next/previous around a browsed list's queue (e.g. Android Auto's mood
        // folders), matching the phone UI's circular swipe-through-favourites pager.
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(icyListener)
        // Notifications and lock-screen controls call seekToNext()/seekToPrevious(). For live
        // radio those generic methods can restart the current item instead of moving through the
        // playlist. Expose a forwarding player to the session so those calls always navigate by
        // media item; the service continues to use the ExoPlayer instance directly.
        sessionPlayer = createSessionPlayer(player)
        mediaSession = MediaLibrarySession.Builder(this, sessionPlayer, librarySessionCallback)
            .setSessionActivity(pendingIntent())
            .setMediaButtonPreferences(listOf(favoriteButton(null)))
            // Coil provides SVG support and the app's configured network client. Media3 still
            // applies its own size limit; caching avoids repeating equivalent artwork requests.
            .setBitmapLoader(CacheBitmapLoader(CoilBitmapLoader(this)))
            .build()
        sleepTimerController = SleepTimerController(
            scope = serviceScope,
            nowMs = SystemClock::elapsedRealtime,
            readVolume = { player.volume },
            writeVolume = { player.volume = it },
            pause = { player.pause() },
            publishState = SleepTimerStore::set,
        )
        log("onCreate")
        serviceScope.launch {
            repository.getAll().collectLatest { updatedStations ->
                stations = updatedStations
                updateFavoriteButton()
                requestAerialWidgetUpdate(this@PlayerService)
            }
        }
        serviceScope.launch {
            dataStore.data
                .map { it[SHOW_HOME_KEY] ?: true }
                .distinctUntilChanged()
                .collectLatest(mediaBrowseTree::setShowHome)
        }
    }

    private val icyListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            log("onPlayWhenReadyChanged=$playWhenReady reason=$reason")
            publishWidgetPlaybackState()
            if (!playWhenReady) {
                pausedAtMs = SystemClock.elapsedRealtime()
                return
            }

            val pausedForMs = pausedAtMs?.let { SystemClock.elapsedRealtime() - it }
            pausedAtMs = null
            if (isStalePause(pausedForMs, STALE_BUFFER_THRESHOLD_MS)) {
                reconnectCurrentStream("resuming after ${pausedForMs}ms pause")
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            log("onIsPlayingChanged=$isPlaying")
            publishWidgetPlaybackState()
            if (isPlaying) {
                recordPlayOnce()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            log("onMediaItemTransition reason=$reason mediaId=${mediaItem?.mediaId}")
            publishWidgetPlaybackState()
            lastIcyTitle = null
            lastId3Title = null
            updateFavoriteButton()
            persistPlaybackSnapshot()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            persistPlaybackSnapshot()
            publishWidgetPlaybackState()
        }

        override fun onMetadata(metadata: Metadata) {
            val frames = streamMetadataFrames(metadata)

            frames.icyTitle?.let { rawTitle ->
                val title = rawTitle.trim()
                if (title.isEmpty() || title == lastIcyTitle) return
                lastIcyTitle = title
                val item = player.currentMediaItem ?: return
                val stationName = currentStation()?.name ?: stationNameFromMediaMetadata(
                    item.mediaMetadata.extras?.getString("stationName"),
                    item.mediaMetadata.title,
                )
                val parsedTrack = parseTrackMetadata(title)
                WidgetPlaybackStore.writeMetadata(
                    this@PlayerService,
                    item.mediaId,
                    parsedTrack.title ?: title,
                    parsedTrack.artist,
                )
                requestAerialWidgetUpdate(this@PlayerService)
                replaceCurrentMediaItem(
                    item,
                    index = player.currentMediaItemIndex,
                    stationName = stationName,
                    artist = parsedTrack.artist,
                    title = parsedTrack.title ?: title,
                    artworkData = item.mediaMetadata.artworkData,
                    artworkUri = item.mediaMetadata.artworkUri,
                )
            }

            val id3Title = frames.id3Title
            if (id3Title != null) {
                if (id3Title != lastId3Title) {
                    lastId3Title = id3Title
                    val item = player.currentMediaItem ?: return
                    val stationName = currentStation()?.name ?: stationNameFromMediaMetadata(
                        item.mediaMetadata.extras?.getString("stationName"),
                        item.mediaMetadata.title,
                    )
                    WidgetPlaybackStore.writeMetadata(
                        this@PlayerService,
                        item.mediaId,
                        id3Title,
                        frames.id3Artist,
                    )
                    requestAerialWidgetUpdate(this@PlayerService)
                    replaceCurrentMediaItem(
                        item,
                        index = player.currentMediaItemIndex,
                        stationName = stationName,
                        artist = frames.id3Artist,
                        title = id3Title,
                        artworkData = frames.id3Artwork ?: item.mediaMetadata.artworkData,
                        artworkUri = if (frames.id3Artwork != null) null else item.mediaMetadata.artworkUri,
                    )
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            log("onPlayerError code=${error.errorCode} message=${error.message}")
            reconnectCurrentStream("player error ${error.errorCode}")
        }
    }

    private val librarySessionCallback = object : MediaLibrarySession.Callback {
        override fun onConnectAsync(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.ConnectionResult> = Futures.immediateFuture(
            MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                        .buildUpon()
                        .apply {
                            AERIAL_CUSTOM_COMMAND_ACTIONS.forEach {
                                add(SessionCommand(it, Bundle.EMPTY))
                            }
                        }
                        .build(),
                )
                .build(),
        )

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_SLEEP_TIMER_SET -> {
                    sleepTimerController.start(args.getLong(SLEEP_TIMER_DURATION_MS, 0L))
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                ACTION_SLEEP_TIMER_CANCEL -> {
                    sleepTimerController.cancel()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                ACTION_TOGGLE_FAVORITE -> {
                    val station = currentStation()
                        ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
                    serviceScope.launch {
                        // Mirrors MainViewModel.toggleFavorite: row existence means "favourited".
                        // The repository flow refreshes `stations`; the local patch just avoids a
                        // stale heart until that lands.
                        withContext(Dispatchers.IO) {
                            when (favoriteToggleAction(station)) {
                                FavoriteToggleAction.Save -> repository.saveAsFavorite(station)

                                FavoriteToggleAction.MarkFavorite ->
                                    repository.update(station.copy(isFavorite = true))

                                FavoriteToggleAction.Remove -> repository.delete(station)
                            }
                        }
                        stations = applyFavoriteToggleLocally(stations, station)
                        updateFavoriteButton()
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                else -> return super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(sessionCoordinator.libraryRoot())

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = serviceFuture {
            sessionCoordinator.resolveItem(mediaId)
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = serviceFuture {
            sessionCoordinator.children(parentId, page, pageSize, params)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = serviceFuture {
            val resultCount = sessionCoordinator.searchCount(query)
            session.notifySearchResultChanged(browser, query, resultCount, params)
            LibraryResult.ofVoid()
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = serviceFuture {
            sessionCoordinator.searchResults(query, page, pageSize, params)
        }

        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = serviceFuture {
            sessionCoordinator.setMediaItems(mediaItems, startIndex, startPositionMs, controller.packageName)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = serviceFuture {
            sessionCoordinator.playbackResumption()
        }
    }

    private fun <T> serviceFuture(block: suspend () -> T): ListenableFuture<T> = serviceScope.asServiceFuture(block)

    // Records a listen the moment audio actually starts (onIsPlayingChanged=true) — the single
    // choke point every surface's playback passes through (phone, Android Auto, Google TV
    // later). Recording on onMediaItemTransition instead would count plays that never happen:
    // the paused last-station restore on every app launch, and REPEAT_MODE_ALL re-transitions
    // when a live stream drops. Deduped by station identity — NOT mediaId, which is "0" for
    // every phone-played unsaved station — so buffering pauses and same-station restarts
    // don't double-count; playing a different station in between resets the guard.
    private fun recordPlayOnce() {
        val station = stationFromMediaItem(player.currentMediaItem, stations) ?: return
        val stationKey = stationPlaybackKey(station)
        if (stationKey == lastRecordedStationKey) return
        lastRecordedStationKey = stationKey
        val playedAt = System.currentTimeMillis()
        // Ephemeral stations (id=0, not yet saved locally) have no row to update.
        if (station.id != 0L) {
            serviceScope.launch { repository.recordPlay(station.id, playedAt) }
        }
        // Recently Played (any station played, favorited or not) only resolves for
        // registry-backed stations — a locally-added custom station has no provider
        // identity to record it by.
        if (station.provider.isNotBlank() && station.providerId.isNotBlank()) {
            serviceScope.launch {
                repository.recordHistoryPlay(PlayHistoryEntry(station.provider, station.providerId, playedAt))
                // Refreshes Android Auto's Recently Played list live, for any browser
                // currently subscribed to it (not just on next re-entry into the folder).
                val recentCount = mediaBrowseTree.children(RECENT_ID)?.size ?: 0
                mediaSession.notifyChildrenChanged(RECENT_ID, recentCount, null)
            }
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
            if (isFavorite) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED,
        )
            .setDisplayName(
                getString(if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
            )
            .setEnabled(station != null)
            .setSessionCommand(favoriteCommand)
            .build()
    }

    private fun currentStation(): Station? = stationFromMediaItem(player.currentMediaItem, stations)

    private fun publishWidgetPlaybackState() {
        val navigation = widgetNavigationAvailability(
            index = player.currentMediaItemIndex,
            size = player.mediaItemCount,
        )
        WidgetPlaybackStore.write(
            this,
            player.currentMediaItem?.mediaId,
            player.playWhenReady,
            navigation.previous,
            navigation.next,
        )
        requestAerialWidgetUpdate(this)
    }

    private fun persistPlaybackSnapshot() {
        val current = currentStation() ?: return
        val queue = (0 until player.mediaItemCount)
            .mapNotNull { index -> stationFromMediaItem(player.getMediaItemAt(index), stations) }
        serviceScope.launch { playbackSnapshotStore.write(current, queue) }
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
        val mediaMetadata = trackDisplayMetadata(
            base = item.mediaMetadata,
            stationName = stationName,
            title = title,
            artist = artist,
            liveRadio = getString(R.string.live_radio).orEmpty(),
        ).buildUpon()
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

    private fun reconnectCurrentStream(reason: String) {
        if (player.currentMediaItem == null) return
        if (!reconnectThrottle.tryAcquire()) {
            log("skip reconnectCurrentStream reason=$reason")
            return
        }
        try {
            val shouldResume = player.playWhenReady
            log("reconnectCurrentStream reason=$reason shouldResume=$shouldResume")
            lastIcyTitle = null
            lastId3Title = null
            runCatching {
                reconnectPlayerAfterError(player, shouldResume)
            }.onFailure { error ->
                Log.w(TAG, "Failed to reconnect current stream", error)
            }
        } finally {
            reconnectThrottle.release()
        }
    }

    override fun onDestroy() {
        WidgetPlaybackStore.markStopped(this)
        requestAerialWidgetUpdate(this)
        serviceScope.cancel()
        SleepTimerStore.set(null)
        player.removeListener(icyListener)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    private fun pendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val TAG = "AerialPlayerService"
        const val STALE_BUFFER_THRESHOLD_MS = 3_000L
        const val MIN_BUFFER_MS = 15_000
        const val MAX_BUFFER_MS = 30_000
        const val BUFFER_FOR_PLAYBACK_MS = 1_500
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000
        const val RECONNECT_RETRY_COOLDOWN_MS = 10_000L
        const val HTTP_TIMEOUT_MS = 8_000
    }
}

/** Re-prepares a failed item without replacing the player's timeline. */
@OptIn(UnstableApi::class)
internal fun reconnectPlayerAfterError(player: Player, shouldResume: Boolean = player.playWhenReady) {
    player.stop()
    player.prepare()
    player.playWhenReady = shouldResume
}
