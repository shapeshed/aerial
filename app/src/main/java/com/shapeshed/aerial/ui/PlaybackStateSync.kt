package com.shapeshed.aerial.ui

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
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

/** Track metadata that arrived for a station the player has not switched to yet. */
internal data class PendingPlaybackMetadata(val station: Station, val title: String?, val artist: String?)

/**
 * Whether incoming track metadata belongs to the current station (apply now) or
 * arrived ahead of a station transition and must wait for it.
 */
internal sealed interface MetadataArrival {
    data class Defer(val station: Station) : MetadataArrival

    data object ApplyNow : MetadataArrival
}

internal fun metadataArrival(incoming: Station?, current: Station?): MetadataArrival = when {
    incoming != null && (current == null || !current.matches(incoming)) -> MetadataArrival.Defer(incoming)
    else -> MetadataArrival.ApplyNow
}

/** The pending metadata when it belongs to [station], otherwise null. */
internal fun PendingPlaybackMetadata?.matching(station: Station): PendingPlaybackMetadata? =
    this?.takeIf { it.station.matches(station) }

/** Applies a player sync to the playback UI state (station, playing, buffering, queue). */
internal fun PlaybackUiState.reducePlaybackSync(
    station: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    queue: List<Station>,
): PlaybackUiState = copy(
    station = station ?: this.station,
    isPlaying = isPlaying,
    isBuffering = isBuffering,
    queue = queue.ifEmpty { this.queue },
)

/**
 * Shows only the bitrate the stream itself declares. Undeclared streams (HLS variant playlists,
 * raw ADTS) deliberately show nothing rather than a measured or estimated value, so video,
 * unselected and unspecified bitrates are ignored instead of being guessed.
 */
@OptIn(UnstableApi::class)
internal fun currentBitrateKbps(tracks: Tracks): Int? = tracks.getGroups()
    .asSequence()
    .filter { group -> group.type == C.TRACK_TYPE_AUDIO && group.isSelected }
    .flatMap { group ->
        (0 until group.length).asSequence()
            .filter { index -> group.isTrackSelected(index) }
            .map { index -> group.getTrackFormat(index) }
    }
    .mapNotNull { format -> format.bitrate.takeIf { it != Format.NO_VALUE && it > 0 } }
    .firstOrNull()
    ?.let { bitrate -> (bitrate / 1_000).coerceAtLeast(1) }
