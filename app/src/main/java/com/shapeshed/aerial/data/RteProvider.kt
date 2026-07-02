package com.shapeshed.aerial.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class RteProvider : Provider {
    private var job: Job? = null
    private var transitionSignal = Channel<Unit>(Channel.CONFLATED)
    private var lastPublishedInfo: NowPlayingInfo? = null

    override fun canEnrich(station: Station): Boolean = isRteStation(station)

    override fun notifyTransition() {
        transitionSignal.trySend(Unit)
    }

    override fun start(station: Station, scope: CoroutineScope) {
        job?.cancel()
        transitionSignal = Channel(Channel.CONFLATED)
        job = scope.launch(Dispatchers.IO) {
            val stationSlug = station.providerId.takeIf { it.isNotBlank() }
            if (stationSlug == null) {
                Log.w(TAG, "No providerId (station slug) for ${station.name}")
                return@launch
            }
            var stepIndex = 0
            while (isActive) {
                val nowPlaying = fetchRteNowPlaying(stationSlug)
                if (nowPlaying != null) {
                    val info = buildNowPlayingInfo(station.id, nowPlaying)
                    if (info != lastPublishedInfo) {
                        lastPublishedInfo = info
                        NowPlayingStore.set(info)
                    }
                }
                val delayMs = Provider.REFRESH_STEPS_MS.getOrElse(stepIndex) { Provider.REFRESH_STEADY_MS }
                stepIndex = minOf(stepIndex + 1, Provider.REFRESH_STEPS_MS.size)
                Log.d(TAG, "RTE polling delay=${delayMs}ms station=${station.name}")
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
        private const val TAG = "RteProvider"
    }
}

private fun buildNowPlayingInfo(stationId: Long, nowPlaying: RteNowPlaying): NowPlayingInfo {
    val artworkData = nowPlaying.artworkUrl?.let { fetchRteBytes(it) }
    return NowPlayingInfo(
        stationId = stationId,
        programmeTitle = nowPlaying.showName,
        programmeSubtitle = nowPlaying.showDescription,
        artworkUrl = nowPlaying.artworkUrl,
        artworkData = artworkData,
        track = null,
    )
}
