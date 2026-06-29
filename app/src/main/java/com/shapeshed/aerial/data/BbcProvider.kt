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
                val delayMs = Provider.REFRESH_STEPS_MS.getOrElse(stepIndex) { Provider.REFRESH_STEADY_MS }
                stepIndex = minOf(stepIndex + 1, Provider.REFRESH_STEPS_MS.size)
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
