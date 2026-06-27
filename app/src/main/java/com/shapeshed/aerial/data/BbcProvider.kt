package com.shapeshed.aerial.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class BbcProvider : Provider {
    private var job: Job? = null
    private var transitionSignal = Channel<Unit>(Channel.CONFLATED)
    private var lastPublishedInfo: NowPlayingInfo? = null

    override fun canEnrich(station: Station): Boolean = isBbcStation(station)

    override fun notifyTransition() {
        transitionSignal.trySend(Unit)
    }

    override fun start(station: Station, scope: CoroutineScope) {
        job?.cancel()
        transitionSignal = Channel(Channel.CONFLATED)
        job = scope.launch(Dispatchers.IO) {
            var stepIndex = 0
            while (isActive) {
                val content = fetchBbcMetadata(station)
                if (content != null) {
                    val info = buildNowPlayingInfo(station.id, content)
                    if (info != lastPublishedInfo) {
                        lastPublishedInfo = info
                        NowPlayingStore.set(info)
                    }
                }
                val delayMs = REFRESH_STEPS_MS.getOrElse(stepIndex) { REFRESH_STEADY_MS }
                stepIndex = minOf(stepIndex + 1, REFRESH_STEPS_MS.size)
                Log.d(TAG, "BBC polling delay=${delayMs}ms station=${station.name}")
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
        private const val TAG = "BbcStationProvider"
        // Poll at t=0 (immediately), then after 5s, 10s, then every 30s steady-state.
        // BBC exposes a 30s polling hint on the live endpoints, so we keep the transition
        // path snappy but back off once the station settles.
        private val REFRESH_STEPS_MS = longArrayOf(5_000L, 10_000L)
        private const val REFRESH_STEADY_MS = 30_000L
    }
}

private fun buildNowPlayingInfo(stationId: Long, content: BbcMetadataContent): NowPlayingInfo {
    val track = if (content.trackArtist != null && content.trackTitle != null) {
        TrackInfo(
            artist = content.trackArtist,
            title = content.trackTitle,
            artworkUrl = content.trackArtworkUrl,
            artworkData = content.trackArtworkData,
        )
    } else null
    return NowPlayingInfo(
        stationId = stationId,
        programmeTitle = content.showTitle,
        programmeSubtitle = content.episodeTitle,
        artworkUrl = content.programmeArtworkUrl ?: if (track != null) content.trackArtworkUrl else null,
        artworkData = content.programmeArtworkData ?: if (track != null) content.trackArtworkData else null,
        track = track,
    )
}
