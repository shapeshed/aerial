package com.shapeshed.aerial.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlayingInfo(
    val stationId: Long,
    val programmeTitle: String? = null,
    val programmeSubtitle: String? = null,
    val artworkUrl: String? = null,
    val artworkData: ByteArray? = null,
    val track: TrackInfo? = null,
)

data class TrackInfo(
    val artist: String,
    val title: String,
    val artworkUrl: String? = null,
    val artworkData: ByteArray? = null,
)

interface Provider {
    fun canEnrich(station: Station): Boolean
    fun start(station: Station, scope: CoroutineScope)
    fun pause()
    fun stop()
    fun notifyTransition()
    fun onIcyTitle(rawTitle: String) {}
}

object NowPlayingStore {
    private val _state = MutableStateFlow<NowPlayingInfo?>(null)
    val state: StateFlow<NowPlayingInfo?> = _state.asStateFlow()
    fun set(info: NowPlayingInfo?) { _state.value = info }
}
