package com.shapeshed.aerial.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BbcNowPlayingState(
    val stationId: Long,
    val programmeTitle: String? = null,
    val trackTitle: String? = null,
    val artworkUrl: String? = null,
    val artworkData: ByteArray? = null,
)

object BbcNowPlayingStore {
    private val _state = MutableStateFlow<BbcNowPlayingState?>(null)
    val state: StateFlow<BbcNowPlayingState?> = _state.asStateFlow()

    fun set(state: BbcNowPlayingState?) {
        _state.value = state
    }

    fun update(transform: (BbcNowPlayingState?) -> BbcNowPlayingState?) {
        _state.value = transform(_state.value)
    }
}
