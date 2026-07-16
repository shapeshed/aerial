package com.shapeshed.aerial

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
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
import com.google.common.util.concurrent.SettableFuture
import com.shapeshed.aerial.data.ACTION_SLEEP_TIMER_CANCEL
import com.shapeshed.aerial.data.ACTION_SLEEP_TIMER_SET
import com.shapeshed.aerial.data.AERIAL_USER_AGENT
import com.shapeshed.aerial.data.MediaBrowseTree
import com.shapeshed.aerial.data.Provider
import com.shapeshed.aerial.data.NowPlayingInfo
import com.shapeshed.aerial.data.NowPlayingStore
import com.shapeshed.aerial.data.PlayHistoryEntry
import com.shapeshed.aerial.data.RECENT_ID
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.SLEEP_TIMER_DURATION_MS
import com.shapeshed.aerial.data.SleepTimerState
import com.shapeshed.aerial.data.SleepTimerStore
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.parseIcyTitle
import com.shapeshed.aerial.ENRICH_METADATA_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class PlayerService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val favoriteCommand = SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)
    private val sleepTimerSetCommand = SessionCommand(ACTION_SLEEP_TIMER_SET, Bundle.EMPTY)
    private val sleepTimerCancelCommand = SessionCommand(ACTION_SLEEP_TIMER_CANCEL, Bundle.EMPTY)
    private var sleepTimerJob: Job? = null

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private lateinit var repository: StationRepository
    private lateinit var registryRepository: RegistryRepository
    private lateinit var mediaBrowseTree: MediaBrowseTree
    private var stations: List<Station> = emptyList()
    private val parentIdByMediaId = mutableMapOf<String, String>()
    private var lastRecordedMediaId: String? = null
    private var lastIcyTitle: String? = null
    private var lastId3Title: String? = null
    private var activeEnricher: Provider? = null
    private var lastAppliedNowPlayingSignature: String? = null
    private var enrichMetadataEnabled = false
    private var pausedAtMs: Long? = null
    private var reconnectingStream = false
    private var lastReconnectAtMs = 0L

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
        registryRepository = (application as AerialApp).registryRepository
        mediaBrowseTree = MediaBrowseTree(this, repository, registryRepository)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(AERIAL_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(HTTP_TIMEOUT_MS)
            .setReadTimeoutMs(HTTP_TIMEOUT_MS)
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)
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
            .build()
        // Wraps skip next/previous around a browsed list's queue (e.g. Android Auto's mood
        // folders), matching the phone UI's circular swipe-through-favourites pager.
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(icyListener)
        mediaSession = MediaLibrarySession.Builder(this, player, librarySessionCallback)
            .setSessionActivity(pendingIntent())
            .setMediaButtonPreferences(listOf(favoriteButton(null)))
            .setBitmapLoader(CoilBitmapLoader(this))
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
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            log("onPlayWhenReadyChanged=$playWhenReady reason=$reason")
            if (!playWhenReady) {
                pausedAtMs = SystemClock.elapsedRealtime()
                return
            }

            val pausedForMs = pausedAtMs?.let { SystemClock.elapsedRealtime() - it }
            pausedAtMs = null
            if (pausedForMs != null && pausedForMs > STALE_BUFFER_THRESHOLD_MS) {
                reconnectCurrentStream("resuming after ${pausedForMs}ms pause")
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            log("onIsPlayingChanged=$isPlaying")
            if (isPlaying) {
                recordPlayOnce()
                currentStation()?.let { activeEnricher?.start(it, serviceScope) }
            } else {
                activeEnricher?.pause()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            log("onMediaItemTransition reason=$reason mediaId=${mediaItem?.mediaId}")
            lastIcyTitle = null
            lastId3Title = null
            lastAppliedNowPlayingSignature = null
            activeEnricher?.stop()
            activeEnricher = null
            val station = stationForMediaItem(mediaItem)
            if (enrichMetadataEnabled) {
                station?.let {
                    val enricher = (application as AerialApp).providers.firstOrNull { it.canEnrich(station) }
                    activeEnricher = enricher
                    if (player.isPlaying) enricher?.start(station, serviceScope)
                }
            }
            updateFavoriteButton()
        }

        @OptIn(UnstableApi::class)
        override fun onMetadata(metadata: Metadata) {
            var icyInfo: IcyInfo? = null
            var id3Title: String? = null
            var id3Artist: String? = null
            var id3Artwork: ByteArray? = null
            var hasUnhandled = false

            for (i in 0 until metadata.length()) {
                val entry = metadata[i]
                when (entry) {
                    is IcyInfo -> icyInfo = entry
                    is TextInformationFrame -> when (entry.id) {
                        "TIT2" -> id3Title = entry.values.first().trim().takeIf { it.isNotEmpty() }
                        "TPE1" -> id3Artist = entry.values.first().trim().takeIf { it.isNotEmpty() }
                    }
                    is ApicFrame -> id3Artwork = entry.pictureData
                    else -> hasUnhandled = true
                }
            }

            icyInfo?.let { icy ->
                val title = icy.title?.trim()
                if (title.isNullOrEmpty() || title == lastIcyTitle) return
                lastIcyTitle = title
                val item = player.currentMediaItem ?: return
                val stationName = currentStation()?.name ?: item.mediaMetadata.title?.toString().orEmpty()
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
            }

            if (id3Title != null) {
                if (id3Title != lastId3Title) {
                    lastId3Title = id3Title
                    val item = player.currentMediaItem ?: return
                    val stationName = currentStation()?.name ?: item.mediaMetadata.title?.toString().orEmpty()
                    replaceCurrentMediaItem(
                        item,
                        index = player.currentMediaItemIndex,
                        stationName = stationName,
                        artist = id3Artist,
                        title = id3Title,
                        artworkData = id3Artwork ?: item.mediaMetadata.artworkData,
                        artworkUri = if (id3Artwork != null) null else item.mediaMetadata.artworkUri,
                    )
                    activeEnricher?.notifyTransition()
                }
            } else if (icyInfo == null && (id3Artwork != null || hasUnhandled)) {
                // Non-ICY, non-ID3-title metadata — signal transition for enriched stations.
                // (Note: BBC HLS currently drops ID3 timed metadata due to a Media3 bug.)
                activeEnricher?.notifyTransition()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            log("onPlayerError code=${error.errorCode} message=${error.message}")
            reconnectCurrentStream("player error ${error.errorCode}")
        }
    }

    private val librarySessionCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            log("onConnect package=${controller.packageName} trusted=${controller.isTrusted}")
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                        .buildUpon()
                        .add(favoriteCommand)
                        .add(sleepTimerSetCommand)
                        .add(sleepTimerCancelCommand)
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
            when (customCommand.customAction) {
                ACTION_SLEEP_TIMER_SET -> {
                    startSleepTimer(args.getLong(SLEEP_TIMER_DURATION_MS, 0L))
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_SLEEP_TIMER_CANCEL -> {
                    cancelSleepTimer()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_TOGGLE_FAVORITE -> {
                    val station = currentStation()
                        ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
                    serviceScope.launch {
                        // Mirrors MainViewModel.toggleFavorite: row existence means "favourited",
                        // so unfavouriting deletes the row and favouriting (re-)saves one. The
                        // repository flow refreshes `stations`; the local patch just avoids a
                        // stale heart until that lands.
                        withContext(Dispatchers.IO) {
                            when {
                                station.id == 0L -> repository.saveAsFavorite(station)
                                !station.isFavorite -> repository.update(station.copy(isFavorite = true))
                                else -> repository.delete(station)
                            }
                        }
                        stations = if (station.isFavorite) {
                            stations.filter { it.id != station.id }
                        } else {
                            stations.map { if (it.id == station.id) station.copy(isFavorite = true) else it }
                        }
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
        ): ListenableFuture<LibraryResult<MediaItem>> {
            log("onGetLibraryRoot package=${browser.packageName}")
            // Folders (Favorites/Moods/Recently Played) read as a list; station logos read well
            // as a grid, similar to most radio/podcast apps on Android Auto.
            val rootExtras = Bundle().apply {
                putInt(
                    MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                    MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
                )
                putInt(
                    MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                    MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
                )
            }
            val rootParams = LibraryParams.Builder().setExtras(rootExtras).build()
            return Futures.immediateFuture(LibraryResult.ofItem(mediaBrowseTree.rootItem(), rootParams))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = serviceFuture {
            mediaBrowseTree.resolve(mediaId)?.let { LibraryResult.ofItem(it, null) }
                ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = serviceFuture {
            val children = mediaBrowseTree.children(parentId)
            if (children == null) {
                LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
            } else {
                // Remembered per mediaId (not just "whichever folder was browsed last") so a tap
                // on one of these (via onSetMediaItems) can queue the tapped item's whole folder,
                // giving Android Auto skip next/previous and an Up Next queue between stations
                // instead of a single-item timeline. Auto prefetches sibling folders' contents in
                // the background (e.g. for artwork), so a single last-folder variable would get
                // clobbered before the user actually taps play. Only the mediaId -> folder
                // mapping is kept; the folder's contents are rebuilt fresh at play time.
                if (children.isNotEmpty() && children.all { it.mediaMetadata.isPlayable == true }) {
                    children.forEach { parentIdByMediaId[it.mediaId] = parentId }
                }
                LibraryResult.ofItemList(children.paginated(page, pageSize), params)
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = serviceFuture {
            val resultCount = mediaBrowseTree.search(query).size
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
            LibraryResult.ofItemList(mediaBrowseTree.search(query).paginated(page, pageSize), params)
        }

        // Android Auto's legacy MediaBrowserCompat bridge plays a tapped browse item (or a voice
        // search/resumption result) by dispatching a MediaItem carrying only a mediaId, not the
        // fully resolved item the browse tree returned — so it must be looked up again here before
        // ExoPlayer can play it. Falls back to the incoming item unchanged if it doesn't resolve
        // (e.g. an ephemeral station the phone UI is already playing directly with a real URI).
        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = serviceFuture {
            // startIndex arrives as C.INDEX_UNSET (-1) — "unspecified" — for a single-item legacy
            // play request, not an actual out-of-range index.
            val effectiveIndex = startIndex.takeIf { it in mediaItems.indices } ?: 0
            val tappedId = mediaItems.getOrNull(effectiveIndex)?.mediaId
            // Queue expansion is for browse-driven controllers (Android Auto) only — the in-app
            // controller sends deliberate single-item plays, and its mediaIds are the same
            // numeric row ids Auto browses, so without the package check a phone tap would be
            // silently expanded into whatever folder Auto last prefetched.
            val parentId = tappedId
                ?.takeIf { mediaItems.size == 1 && controller.packageName != packageName }
                ?.let { parentIdByMediaId[it] }
            // Rebuilt fresh (not served from a browse-time cache) so a folder edited since it
            // was browsed — an unfavorited station, a changed stream URL — can't queue stale
            // entries.
            val siblings = parentId?.let { mediaBrowseTree.children(it) }
            val siblingIndex = siblings?.indexOfFirst { it.mediaId == tappedId } ?: -1
            if (siblings != null && siblingIndex >= 0) {
                MediaSession.MediaItemsWithStartPosition(siblings, siblingIndex, startPositionMs)
            } else {
                val resolved = mediaItems.map { item -> mediaBrowseTree.resolve(item.mediaId) ?: item }
                MediaSession.MediaItemsWithStartPosition(resolved, startIndex, startPositionMs)
            }
        }
    }

    private fun <T> serviceFuture(block: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        serviceScope.launch {
            runCatching { block() }
                .onSuccess { future.set(it) }
                .onFailure { future.setException(it) }
        }
        return future
    }

    private fun List<MediaItem>.paginated(page: Int, pageSize: Int): List<MediaItem> {
        if (pageSize <= 0) return this
        val from = (page * pageSize).coerceIn(0, size)
        val to = (from + pageSize).coerceIn(from, size)
        return subList(from, to)
    }

    // Records a listen the moment audio actually starts (onIsPlayingChanged=true) — the single
    // choke point every surface's playback passes through (phone, Android Auto, Google TV
    // later). Recording on onMediaItemTransition instead would count plays that never happen:
    // the paused last-station restore on every app launch, and REPEAT_MODE_ALL re-transitions
    // when a live stream drops. Deduped per mediaId so buffering pauses and same-station
    // restarts don't double-count; playing a different station in between resets the guard.
    private fun recordPlayOnce() {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        if (mediaId == lastRecordedMediaId) return
        val station = stationForMediaItem(player.currentMediaItem) ?: return
        lastRecordedMediaId = mediaId
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
            if (isFavorite) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED
        )
            .setDisplayName(if (isFavorite) "Remove from favorites" else "Add to favorites")
            .setEnabled(station != null)
            .setSessionCommand(favoriteCommand)
            .build()
    }

    private fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        if (durationMs <= 0L) {
            cancelSleepTimer()
            return
        }
        player.volume = 1f // clear any leftover fade from a previous timer
        val endAt = SystemClock.elapsedRealtime() + durationMs
        sleepTimerJob = serviceScope.launch {
            while (isActive) {
                val remaining = endAt - SystemClock.elapsedRealtime()
                if (remaining <= 0L) break
                SleepTimerStore.set(SleepTimerState(totalMs = durationMs, remainingMs = remaining))
                delay(remaining.coerceAtMost(1_000L))
            }
            fadeOutAndPause()
        }
    }

    private fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        player.volume = 1f // undo any in-progress fade
        SleepTimerStore.set(null)
    }

    // Ease the volume down over ~4s so the timer doesn't cut playback off abruptly, then pause.
    // Volume is restored so the next play() isn't silent. Cancellation mid-fade is handled by
    // cancelSleepTimer(), which resets the volume.
    private suspend fun fadeOutAndPause() {
        val startVolume = player.volume
        val steps = 20
        for (i in 1..steps) {
            player.volume = startVolume * (1f - i / steps.toFloat())
            delay(FADE_STEP_MS)
        }
        player.pause()
        player.volume = 1f
        sleepTimerJob = null
        SleepTimerStore.set(null)
    }

    private fun currentStation(): Station? = stationForMediaItem(player.currentMediaItem)

    private fun stationForMediaItem(mediaItem: MediaItem?): Station? {
        if (mediaItem == null) return null
        mediaItem.mediaId.toLongOrNull()?.let { id ->
            stations.firstOrNull { it.id == id }?.let { return it }
        }
        val extras = mediaItem.mediaMetadata.extras ?: return null
        val streamUrl = extras.getString("streamUrl")?.takeIf { it.isNotBlank() } ?: return null
        // A station playing under an ephemeral mediaId ("0" from the phone, "reg:4492" from
        // the Android Auto browse tree) may still exist as a saved row — matched the same way
        // StationRepository.findExisting does — and must resolve to it, or the favorite toggle
        // would see isFavorite=false forever and re-save instead of unfavoriting.
        val provider = extras.getString("provider").orEmpty()
        val providerId = extras.getString("providerId").orEmpty()
        if (provider.isNotBlank() && providerId.isNotBlank()) {
            stations.firstOrNull { it.provider == provider && it.providerId == providerId }?.let { return it }
        }
        stations.firstOrNull { it.streamUrl == streamUrl }?.let { return it }
        return Station(
            id = 0,
            name = mediaItem.mediaMetadata.title?.toString() ?: "",
            streamUrl = streamUrl,
            logoPath = extras.getString("logoPath").orEmpty(),
            provider = provider,
            providerId = providerId,
        )
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

    private fun reconnectCurrentStream(reason: String) {
        if (reconnectingStream) return
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastReconnectAtMs < RECONNECT_RETRY_COOLDOWN_MS) {
            log("skip reconnectCurrentStream reason=$reason cooldown")
            return
        }
        val item = player.currentMediaItem ?: return
        val shouldResume = player.playWhenReady
        reconnectingStream = true
        lastReconnectAtMs = nowMs
        log("reconnectCurrentStream reason=$reason shouldResume=$shouldResume")
        lastIcyTitle = null
        lastId3Title = null
        lastAppliedNowPlayingSignature = null
        runCatching {
            player.stop()
            player.setMediaItem(item)
            player.prepare()
            player.playWhenReady = shouldResume
        }.onFailure { error ->
            Log.w(TAG, "Failed to reconnect current stream", error)
        }
        reconnectingStream = false
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
            .setTitle(
                if (info.track != null) {
                    info.track.artist.takeIf { it.isNotBlank() } ?: info.programmeTitle ?: currentStation.name
                } else {
                    currentStation.name
                }
            )
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
        SleepTimerStore.set(null)
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
        const val FADE_STEP_MS = 200L
        const val STALE_BUFFER_THRESHOLD_MS = 3_000L
        const val MIN_BUFFER_MS = 15_000
        const val MAX_BUFFER_MS = 30_000
        const val BUFFER_FOR_PLAYBACK_MS = 1_500
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000
        const val RECONNECT_RETRY_COOLDOWN_MS = 10_000L
        const val HTTP_TIMEOUT_MS = 8_000
    }
}
