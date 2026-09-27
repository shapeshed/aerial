package com.shapeshed.aerial.playback

import androidx.compose.runtime.Immutable

/** Station name plus a second-line ICY/ID3 summary for the mini player and notifications. */
@Immutable
data class NowPlayingDisplay(val title: String, val subtitle: String)

/** Two-line compact-player text: track title first, artist second. */
@Immutable
data class TrackDisplay(val title: String, val artist: String)

fun computeTrackDisplay(
    stationName: String,
    trackTitle: String?,
    trackArtist: String?,
    liveRadio: String = "Live Radio",
): TrackDisplay {
    val title = trackTitle?.trim()?.takeIf {
        it.isNotEmpty() && it != stationName && it != liveRadio
    }
    val artist = trackArtist?.trim()?.takeIf {
        it.isNotEmpty() && it != stationName && it != liveRadio
    }
    val hasTrackMetadata = title != null || artist != null
    // With track metadata, the station name is the meaningful second line — the same value the
    // Media3 notification and quick-settings player show. Without it, use the "Live Radio"
    // placeholder rather than repeating the station name on both lines.
    return TrackDisplay(
        title = title ?: stationName,
        artist = artist ?: if (hasTrackMetadata) stationName else liveRadio,
    )
}

/** Derives stable station and ICY/ID3 display text shared by all playback surfaces. */
fun computeNowPlayingDisplay(
    stationName: String,
    icyTitle: String?,
    icyArtist: String? = null,
    liveRadio: String = "Live Radio",
): NowPlayingDisplay {
    val title = icyTitle?.trim()?.takeIf { it.isNotEmpty() && it != stationName }
    val artist = icyArtist?.trim()?.takeIf { it.isNotEmpty() && it != stationName }
    val icyInfo = when {
        artist != null && title != null -> "$artist — $title"
        title != null -> title
        artist != null -> artist
        else -> liveRadio
    }
    return NowPlayingDisplay(stationName, icyInfo)
}
