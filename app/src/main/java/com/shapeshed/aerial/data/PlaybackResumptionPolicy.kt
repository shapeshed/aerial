package com.shapeshed.aerial.data

/** Chooses the queue to restore for a saved playback snapshot. */
internal fun queueForResumption(
    snapshotQueue: List<Station>,
    fallbackStations: List<Station>,
    sort: FavoritesSort,
): List<Station> = snapshotQueue.takeIf { it.size > 1 } ?: sortStations(fallbackStations, sort)
