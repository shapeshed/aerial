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
) {
    // ByteArray uses reference equality in data classes, so override to compare by content.
    // Without this, a cache eviction + re-fetch with identical bytes triggers a spurious
    // NowPlayingStore update and downstream recomposition even though nothing changed visually.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NowPlayingInfo) return false
        return stationId == other.stationId &&
            programmeTitle == other.programmeTitle &&
            programmeSubtitle == other.programmeSubtitle &&
            artworkUrl == other.artworkUrl &&
            artworkData.contentEquals(other.artworkData) &&
            track == other.track
    }

    override fun hashCode(): Int {
        var result = stationId.hashCode()
        result = 31 * result + programmeTitle.hashCode()
        result = 31 * result + programmeSubtitle.hashCode()
        result = 31 * result + artworkUrl.hashCode()
        result = 31 * result + artworkData.contentHashCode()
        result = 31 * result + track.hashCode()
        return result
    }
}

data class TrackInfo(
    val artist: String,
    val title: String,
    val artworkUrl: String? = null,
    val artworkData: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackInfo) return false
        return artist == other.artist &&
            title == other.title &&
            artworkUrl == other.artworkUrl &&
            artworkData.contentEquals(other.artworkData)
    }

    override fun hashCode(): Int {
        var result = artist.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artworkUrl.hashCode()
        result = 31 * result + artworkData.contentHashCode()
        return result
    }
}

interface Provider {
    fun canEnrich(station: Station): Boolean
    fun start(station: Station, scope: CoroutineScope)
    fun pause()
    fun stop()
    fun notifyTransition()
    fun onIcyTitle(rawTitle: String) {}

    companion object {
        // Poll immediately, then after 5 s and 10 s, then every 30 s steady-state.
        // 30 s matches the hint returned by most enhanced-metadata endpoints.
        val REFRESH_STEPS_MS: LongArray = longArrayOf(5_000L, 10_000L)
        const val REFRESH_STEADY_MS: Long = 30_000L
    }
}

object NowPlayingStore {
    private val _state = MutableStateFlow<NowPlayingInfo?>(null)
    val state: StateFlow<NowPlayingInfo?> = _state.asStateFlow()
    fun set(info: NowPlayingInfo?) { _state.value = info }
}
