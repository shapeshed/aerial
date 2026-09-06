package com.shapeshed.aerial.data

/** Pure playback inputs derived before any Media3 or coroutine side effects occur. */
internal data class PlaybackQueuePlan(
    val station: Station,
    val requestedQueue: List<Station>,
    val playerQueue: List<Station>,
    val startIndex: Int?,
)

internal fun buildPlaybackQueuePlan(
    station: Station,
    queue: List<Station>,
    availableStations: List<Station>,
): PlaybackQueuePlan {
    val playbackStation = availableStations.firstOrNull { it.matches(station) } ?: station
    val playbackQueue = queue.map { queued ->
        availableStations.firstOrNull { it.matches(queued) } ?: queued
    }
    val startIndex = resolveQueueStart(playbackQueue, playbackStation)
    return PlaybackQueuePlan(
        station = playbackStation,
        requestedQueue = playbackQueue,
        playerQueue = playbackQueue.takeIf { startIndex != null } ?: listOf(playbackStation),
        startIndex = startIndex,
    )
}
