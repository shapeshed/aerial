package com.shapeshed.aerial

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
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
import com.shapeshed.aerial.data.FAVORITES_SORT_KEY
import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.LAST_PLAYED_STATION_KEY
import com.shapeshed.aerial.data.MediaBrowseTree
import com.shapeshed.aerial.data.PlayHistoryEntry
import com.shapeshed.aerial.data.RECENT_ID
import com.shapeshed.aerial.data.httpGetText
import com.shapeshed.aerial.data.lastPlayedStationSnapshot
import com.shapeshed.aerial.data.resolveQueueStart
import com.shapeshed.aerial.data.resolveStreamUrl
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.SLEEP_TIMER_DURATION_MS
import com.shapeshed.aerial.data.SleepTimerState
import com.shapeshed.aerial.data.SleepTimerStore
import com.shapeshed.aerial.data.sortStations
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.parseIcyTitle
import com.shapeshed.aerial.data.toLastPlayedJson
import com.shapeshed.aerial.SHOW_HOME_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private lateinit var sessionPlayer: Player
    private lateinit var mediaSession: MediaLibrarySession
    private lateinit var repository: StationRepository
    private lateinit var registryRepository: RegistryRepository
    private lateinit var mediaBrowseTree: MediaBrowseTree
    private var stations: List<Station> = emptyList()
    private val parentIdByMediaId = mutableMapOf<String, String>()
    private var lastRecordedStationKey: String? = null
    private var lastIcyTitle: String? = null
    private var lastId3Title: String? = null
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
            if (resolved == original) dataSpec else dataSpec.withUri(Uri.parse(resolved))
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
        sessionPlayer = object : ForwardingPlayer(player) {
            override fun seekToPrevious() = seekToPreviousMediaItem()
            override fun seekToNext() = seekToNextMediaItem()
        }
        mediaSession = MediaLibrarySession.Builder(this, sessionPlayer, librarySessionCallback)
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
            dataStore.data
                .map { it[SHOW_HOME_KEY] ?: true }
                .distinctUntilChanged()
                .collectLatest(mediaBrowseTree::setShowHome)
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
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            log("onMediaItemTransition reason=$reason mediaId=${mediaItem?.mediaId}")
            lastIcyTitle = null
            lastId3Title = null
            updateFavoriteButton()
            persistPlaybackSnapshot()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            persistPlaybackSnapshot()
        }

        @OptIn(UnstableApi::class)
        override fun onMetadata(metadata: Metadata) {
            var icyInfo: IcyInfo? = null
            var id3Title: String? = null
            var id3Artist: String? = null
            var id3Artwork: ByteArray? = null

            for (i in 0 until metadata.length()) {
                val entry = metadata[i]
                when (entry) {
                    is IcyInfo -> icyInfo = entry
                    is TextInformationFrame -> when (entry.id) {
                        "TIT2" -> id3Title = entry.values.first().trim().takeIf { it.isNotEmpty() }
                        "TPE1" -> id3Artist = entry.values.first().trim().takeIf { it.isNotEmpty() }
                    }
                    is ApicFrame -> id3Artwork = entry.pictureData
                    else -> Unit
                }
            }

            icyInfo?.let { icy ->
                val title = icy.title?.trim()
                if (title.isNullOrEmpty() || title == lastIcyTitle) return
                lastIcyTitle = title
                val item = player.currentMediaItem ?: return
                val stationName = currentStation()?.name ?: stationNameFromMediaMetadata(
                    item.mediaMetadata.extras?.getString("stationName"),
                    item.mediaMetadata.title,
                )
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
            }

            if (id3Title != null) {
                if (id3Title != lastId3Title) {
                    lastId3Title = id3Title
                    val item = player.currentMediaItem ?: return
                    val stationName = currentStation()?.name ?: stationNameFromMediaMetadata(
                        item.mediaMetadata.extras?.getString("stationName"),
                        item.mediaMetadata.title,
                    )
                    replaceCurrentMediaItem(
                        item,
                        index = player.currentMediaItemIndex,
                        stationName = stationName,
                        artist = id3Artist,
                        title = id3Title,
                        artworkData = id3Artwork ?: item.mediaMetadata.artworkData,
                        artworkUri = if (id3Artwork != null) null else item.mediaMetadata.artworkUri,
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
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
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
            expandControllerQueue(
                mediaItems = mediaItems,
                startIndex = startIndex,
                startPositionMs = startPositionMs,
                controllerPackage = controller.packageName,
                appPackage = packageName,
                parentIdForMediaId = { parentIdByMediaId[it] },
                childrenForParent = { mediaBrowseTree.children(it) },
                resolveMediaItem = { mediaBrowseTree.resolve(it) },
            )
        }

        // Called when a controller (lock-screen/notification, Bluetooth, Assistant) reconnects
        // to a session whose player has no media item — e.g. the whole process was killed while
        // the screen was off and the system is now restarting the service to handle a media
        // button press. Without this, that reconnection carries only whatever single item the
        // system cached, so Previous/Next on the lock screen have nothing to navigate — the same
        // "queue collapses to one station" bug loadStationPaused fixes for the app-driven restore
        // path, but for the case where the app itself never reopens.
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = serviceFuture {
            val snapshot = dataStore.data.first()[LAST_PLAYED_STATION_KEY]?.let(::lastPlayedStationSnapshot)
                ?: throw UnsupportedOperationException("No last-played station to resume")
            val savedStation = snapshot.station.id.takeIf { it > 0 }?.let { repository.getById(it) }
                ?: repository.getByStreamUrl(snapshot.station.streamUrl)
            val queue = snapshot.queue.takeIf { it.size > 1 } ?: run {
                // Backward compatibility for snapshots written before ordered queues were
                // persisted. New snapshots restore the exact Media3 timeline instead of
                // rebuilding it from mutable Last/Most Played statistics.
                val sort = dataStore.data.first()[FAVORITES_SORT_KEY]
                    ?.let { saved -> FavoritesSort.entries.firstOrNull { it.name == saved } }
                    ?: FavoritesSort.AZ
                sortStations(repository.getAll().first(), sort)
            }
            val resumed = savedStation ?: snapshot.station.copy(id = 0)
            val startIndex = resolveQueueStart(queue, resumed)
            if (startIndex != null) {
                MediaSession.MediaItemsWithStartPosition(
                    queue.map { it.toPlayableMediaItem(this@PlayerService) },
                    startIndex,
                    C.TIME_UNSET,
                )
            } else {
                MediaSession.MediaItemsWithStartPosition(
                    listOf(resumed.toPlayableMediaItem(this@PlayerService)),
                    0,
                    C.TIME_UNSET,
                )
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
    // when a live stream drops. Deduped by station identity — NOT mediaId, which is "0" for
    // every phone-played unsaved station — so buffering pauses and same-station restarts
    // don't double-count; playing a different station in between resets the guard.
    private fun recordPlayOnce() {
        val station = stationForMediaItem(player.currentMediaItem) ?: return
        val stationKey = "${station.provider}|${station.providerId}|${station.streamUrl}"
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

    private fun persistPlaybackSnapshot() {
        val current = currentStation() ?: return
        val queue = (0 until player.mediaItemCount)
            .mapNotNull { index -> stationForMediaItem(player.getMediaItemAt(index)) }
        serviceScope.launch {
            dataStore.edit { preferences ->
                preferences[LAST_PLAYED_STATION_KEY] = current.toLastPlayedJson(queue).toString()
            }
        }
    }

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
            name = stationNameFromMediaMetadata(
                mediaItem.mediaMetadata.extras?.getString("stationName"),
                mediaItem.mediaMetadata.title,
            ),
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
        runCatching {
            reconnectPlayerAfterError(player, shouldResume)
        }.onFailure { error ->
            Log.w(TAG, "Failed to reconnect current stream", error)
        }
        reconnectingStream = false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        SleepTimerStore.set(null)
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

/** Re-prepares a failed item without replacing the player's timeline. */
internal fun reconnectPlayerAfterError(player: Player, shouldResume: Boolean = player.playWhenReady) {
    player.stop()
    player.prepare()
    player.playWhenReady = shouldResume
}

internal suspend fun expandControllerQueue(
    mediaItems: List<MediaItem>,
    startIndex: Int,
    startPositionMs: Long,
    controllerPackage: String,
    appPackage: String,
    parentIdForMediaId: (String) -> String?,
    childrenForParent: suspend (String) -> List<MediaItem>?,
    resolveMediaItem: suspend (String) -> MediaItem?,
): MediaSession.MediaItemsWithStartPosition {
    val effectiveIndex = startIndex.takeIf { it in mediaItems.indices } ?: 0
    val tappedId = mediaItems.getOrNull(effectiveIndex)?.mediaId
    val parentId = tappedId
        ?.takeIf { mediaItems.size == 1 && controllerPackage != appPackage }
        ?.let(parentIdForMediaId)
    val siblings = if (parentId != null) childrenForParent(parentId) else null
    val siblingIndex = siblings?.indexOfFirst { it.mediaId == tappedId } ?: -1
    return if (siblings != null && siblingIndex >= 0) {
        MediaSession.MediaItemsWithStartPosition(siblings, siblingIndex, startPositionMs)
    } else {
        val resolved = mediaItems.map { item -> resolveMediaItem(item.mediaId) ?: item }
        MediaSession.MediaItemsWithStartPosition(resolved, startIndex, startPositionMs)
    }
}
