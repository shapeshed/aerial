package com.shapeshed.aerial.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class BbcNowPlayingEnricher : NowPlayingEnricher {
    private var job: Job? = null
    private var transitionSignal = Channel<Unit>(Channel.CONFLATED)

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
                val content = fetchBbcNowPlaying(station)
                if (content != null) {
                    NowPlayingStore.set(buildNowPlayingInfo(station.id, content))
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
        NowPlayingStore.set(null)
    }

    companion object {
        private const val TAG = "BbcNowPlayingEnricher"
        // Poll at t=0 (immediately), then after 5s, 10s, then every 10s steady-state.
        // BBC HLS has no metadata events, so polling is the only signal. 10s means at most
        // 10s display lag after a track change, which is acceptable for 3-4 minute songs.
        private val REFRESH_STEPS_MS = longArrayOf(5_000L, 10_000L)
        private const val REFRESH_STEADY_MS = 10_000L
    }
}

private fun buildNowPlayingInfo(stationId: Long, content: BbcNowPlayingContent): NowPlayingInfo {
    // A segment is a real music track if its title doesn't begin with the show title.
    // When they match (talk stations), the segment just mirrors the programme — use the
    // programme's own title/episode split instead.
    val isRealTrack = content.trackTitle != null &&
        (content.showTitle == null || !content.trackTitle.startsWith(content.showTitle))
    return NowPlayingInfo(
        stationId = stationId,
        title = if (isRealTrack) content.trackTitle else content.showTitle,
        subtitle = if (isRealTrack) content.showTitle else content.episodeTitle,
        artworkUrl = content.artworkUrl,
        artworkData = content.artworkData,
    )
}
