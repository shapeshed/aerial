package com.shapeshed.aerial.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Generic now-playing metadata for any enriched station.
 * [title] / [subtitle] remain the primary display pair for backward compatibility.
 * BBC enrichers also populate programme and track-specific fields so the UI can
 * show them separately when both are available.
 */
data class NowPlayingInfo(
    val stationId: Long,
    val title: String? = null,
    val subtitle: String? = null,
    val programmeTitle: String? = null,
    val programmeSubtitle: String? = null,
    val programmeArtworkUrl: String? = null,
    val programmeArtworkData: ByteArray? = null,
    val trackArtist: String? = null,
    val trackTitle: String? = null,
    val trackSubtitle: String? = null,
    val trackArtworkUrl: String? = null,
    val trackArtworkData: ByteArray? = null,
    val artworkUrl: String? = null,
    val artworkData: ByteArray? = null,
)

interface NowPlayingEnricher {
    fun canEnrich(station: Station): Boolean
    /** Start (or restart) polling. Safe to call multiple times. */
    fun start(station: Station, scope: CoroutineScope)
    /** Pause polling but keep the current state visible. */
    fun pause()
    /** Stop polling and clear state. */
    fun stop()
    /** Signal that a track transition may have occurred — triggers an immediate refresh. */
    fun notifyTransition()
}

object NowPlayingStore {
    private val _state = MutableStateFlow<NowPlayingInfo?>(null)
    val state: StateFlow<NowPlayingInfo?> = _state.asStateFlow()
    fun set(info: NowPlayingInfo?) { _state.value = info }
}
