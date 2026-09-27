package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.Station

/**
 * The decision a player event implies, separated from performing it.
 *
 * [MainViewModel] used to compute and apply the new playback state in one function, which meant
 * the interesting half — "does this event change the station, does it clear the previous
 * station's metadata, should the snapshot be re-persisted" — could only be exercised through a
 * live `MediaController`. Returning the decision instead makes every transition a plain
 * input/output test, and leaves the ViewModel holding only the effects that genuinely touch
 * something outside itself.
 */
internal data class PlaybackTransition(
    /** The state the UI should show. */
    val state: PlaybackUiState,
    /** Persisted row id vs ephemeral station, for the identity the ViewModel tracks. */
    val identity: PlaybackStationIdentity,
    /** Whether this is a different station from the one playing, by identity not value. */
    val stationChanged: Boolean,
    /**
     * Track metadata that arrived ahead of this transition and belongs to the new station.
     * Non-null means the caller should normalise and apply it now, and clear the pending slot.
     */
    val applyPendingMetadata: PendingPlaybackMetadata?,
    /** Whether the last-played snapshot should be re-written. */
    val shouldPersist: Boolean,
)

/**
 * Reduces a player snapshot onto the current playback state.
 *
 * The ordering matters and matches the previous inline implementation: per-station state is
 * cleared *before* the new bitrate lands, so a station change clears the old bitrate and then
 * immediately reports the new one rather than clearing the new value.
 */
internal fun reducePlaybackTransition(
    current: PlaybackUiState,
    station: Station?,
    queue: List<Station>,
    isPlaying: Boolean,
    isBuffering: Boolean,
    bitrateKbps: Int? = null,
    pendingMetadata: PendingPlaybackMetadata? = null,
    suppressPersist: Boolean = false,
): PlaybackTransition {
    val changed = playbackStationChanged(current.station, station)
    val resolved = if (changed) current.clearedPerStationState() else current
    return PlaybackTransition(
        state = resolved
            .reducePlaybackSync(
                station = station,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                queue = queue,
            )
            // A null bitrate means "not reported by this event", not "unknown". Only a station
            // change clears the previous reading, which clearedPerStationState has already done.
            .copy(bitrateKbps = bitrateKbps ?: resolved.bitrateKbps),
        identity = PlaybackStationIdentity.of(station),
        stationChanged = changed,
        applyPendingMetadata = station?.let { pendingMetadata?.matching(it) },
        shouldPersist = station != null && !suppressPersist,
    )
}

/**
 * Reduces a player error. Playback has stopped, so the transport must not keep claiming to be
 * playing or buffering while an error is on screen.
 */
internal fun reducePlaybackError(current: PlaybackUiState, message: String): PlaybackUiState = current.copy(
    isPlaying = false,
    isBuffering = false,
    error = message,
)

/** Reduces normalised track metadata onto the state. */
internal fun reduceTrackMetadata(current: PlaybackUiState, title: String?, artist: String?): PlaybackUiState =
    current.copy(trackTitle = title, trackArtist = artist)
