package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.Station

/**
 * Identity of the station the player is currently on: a persisted row id, or an
 * ephemeral (unsaved) station held only in memory. Extracted from MainViewModel
 * so the transitions are covered by fast JVM tests.
 */
internal data class PlaybackStationIdentity(val stationId: Long?, val ephemeralStation: Station?) {
    companion object {
        fun of(station: Station?): PlaybackStationIdentity = when {
            station == null -> PlaybackStationIdentity(stationId = null, ephemeralStation = null)
            station.id == 0L -> PlaybackStationIdentity(stationId = null, ephemeralStation = station)
            else -> PlaybackStationIdentity(stationId = station.id, ephemeralStation = null)
        }
    }
}

/** True when [next] is a different station than [previous], by identity rather than value. */
internal fun playbackStationChanged(previous: Station?, next: Station?): Boolean = when {
    previous == null -> next != null
    next == null -> true
    else -> !previous.matches(next)
}

/**
 * Clears per-station transient playback state. Called when the playing station
 * changes; `onMediaMetadataChanged` repopulates it for the new station.
 */
internal fun PlaybackUiState.clearedPerStationState(): PlaybackUiState = copy(
    trackTitle = null,
    trackArtist = null,
    bitrateKbps = null,
    error = null,
)

/** Station names used to filter a station's own name out of live track metadata. */
internal fun PlaybackUiState.stationNamesForMetadataFilter(allStations: List<Station>): List<String> = buildList {
    station?.name?.let(::add)
    addAll(queue.map(Station::name))
    addAll(allStations.map(Station::name))
}
