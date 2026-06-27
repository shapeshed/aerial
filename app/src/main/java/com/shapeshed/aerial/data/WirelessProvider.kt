package com.shapeshed.aerial.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class WirelessProvider : Provider {
    private var job: Job? = null
    private var transitionSignal = Channel<Unit>(Channel.CONFLATED)
    private var lastPublishedInfo: NowPlayingInfo? = null

    override fun canEnrich(station: Station): Boolean = isWirelessStation(station)

    override fun notifyTransition() {
        transitionSignal.trySend(Unit)
    }

    override fun start(station: Station, scope: CoroutineScope) {
        job?.cancel()
        transitionSignal = Channel(Channel.CONFLATED)
        job = scope.launch(Dispatchers.IO) {
            val stationId = resolveWirelessStationId(station)
            if (stationId == null) {
                Log.w(TAG, "Could not resolve stationId for ${station.name}")
                return@launch
            }
            var stepIndex = 0
            while (isActive) {
                val content = fetchWirelessMetadata(stationId)
                if (content != null) {
                    val info = buildNowPlayingInfo(station.id, content)
                    if (info != lastPublishedInfo) {
                        lastPublishedInfo = info
                        NowPlayingStore.set(info)
                    }
                }
                val delayMs = REFRESH_STEPS_MS.getOrElse(stepIndex) { REFRESH_STEADY_MS }
                stepIndex = minOf(stepIndex + 1, REFRESH_STEPS_MS.size)
                Log.d(TAG, "Wireless polling delay=${delayMs}ms station=${station.name}")
                withTimeoutOrNull(delayMs) { transitionSignal.receive() }
            }
        }
    }

    override fun pause() {
        job?.cancel()
        job = null
    }

    override fun stop() {
        job?.cancel()
        job = null
        lastPublishedInfo = null
        NowPlayingStore.set(null)
    }

    companion object {
        private const val TAG = "WirelessStationProvider"
        private val REFRESH_STEPS_MS = longArrayOf(5_000L, 10_000L)
        private const val REFRESH_STEADY_MS = 30_000L
    }
}

private fun buildNowPlayingInfo(stationId: Long, content: WirelessMetadataContent): NowPlayingInfo {
    val hasTrack = content.trackArtist != null && content.trackTitle != null
    val track = if (hasTrack) TrackInfo(artist = content.trackArtist.orEmpty(), title = content.trackTitle.orEmpty()) else null
    return NowPlayingInfo(
        stationId = stationId,
        programmeTitle = content.showTitle,
        programmeSubtitle = null,
        artworkUrl = if (hasTrack) null else content.showArtworkUrl,
        artworkData = if (hasTrack) null else content.showArtworkData,
        track = track,
    )
}
