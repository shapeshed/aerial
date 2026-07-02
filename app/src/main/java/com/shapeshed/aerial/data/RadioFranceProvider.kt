package com.shapeshed.aerial.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class RadioFranceProvider : Provider {
    private var job: Job? = null
    private var transitionSignal = Channel<Unit>(Channel.CONFLATED)
    private var lastPublishedInfo: NowPlayingInfo? = null

    override fun canEnrich(station: Station): Boolean =
        station.provider == "radio-france" && station.providerId.toIntOrNull() != null

    override fun notifyTransition() {
        transitionSignal.trySend(Unit)
    }

    override fun start(station: Station, scope: CoroutineScope) {
        job?.cancel()
        transitionSignal = Channel(Channel.CONFLATED)
        job = scope.launch(Dispatchers.IO) {
            val stationId = station.providerId.toIntOrNull() ?: return@launch
            var stepIndex = 0
            while (isActive) {
                val nowPlaying = fetchRadioFranceNowPlaying(stationId)
                if (nowPlaying != null) {
                    val info = buildNowPlayingInfo(station.id, nowPlaying)
                    if (info != lastPublishedInfo) {
                        lastPublishedInfo = info
                        NowPlayingStore.set(info)
                    }
                }
                val delayMs = Provider.REFRESH_STEPS_MS.getOrElse(stepIndex) { nowPlaying?.delayToRefreshMs ?: Provider.REFRESH_STEADY_MS }
                stepIndex = minOf(stepIndex + 1, Provider.REFRESH_STEPS_MS.size)
                Log.d(TAG, "Radio France polling delay=${delayMs}ms station=${station.name}")
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
        private const val TAG = "RadioFranceProvider"
    }
}

private fun buildNowPlayingInfo(stationId: Long, nowPlaying: RadioFranceNowPlaying): NowPlayingInfo {
    val artworkData = nowPlaying.visualUrl?.let { fetchRadioFranceBytes(it) }
    val track = if (nowPlaying.isSong) {
        TrackInfo(
            artist = nowPlaying.artist ?: "",
            title = nowPlaying.title,
            artworkUrl = nowPlaying.visualUrl,
            artworkData = artworkData,
        )
    } else null
    return NowPlayingInfo(
        stationId = stationId,
        programmeTitle = if (track == null) nowPlaying.title else nowPlaying.programmeTitle,
        programmeSubtitle = if (track == null) nowPlaying.description else null,
        artworkUrl = if (track == null) nowPlaying.visualUrl else null,
        artworkData = if (track == null) artworkData else null,
        track = track,
    )
}
