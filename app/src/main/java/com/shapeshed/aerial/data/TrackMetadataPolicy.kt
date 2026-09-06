package com.shapeshed.aerial.data

data class NormalizedTrackMetadata(
    val title: String?,
    val artist: String?,
)

/** Removes station-name/live-radio echoes from metadata before displaying it. */
fun normalizeTrackMetadata(
    title: String?,
    artist: String?,
    liveRadioLabel: String,
    stationNames: Iterable<String>,
): NormalizedTrackMetadata {
    val parsed = parseTrackMetadata(title, artist)
    fun String.isDisplayable(): Boolean =
        this != liveRadioLabel && stationNames.none { it.equals(this, ignoreCase = true) }

    val normalizedTitle = parsed.title?.takeIf(String::isDisplayable)
    val normalizedArtist = parsed.artist?.takeIf(String::isDisplayable)
    return NormalizedTrackMetadata(
        title = normalizedTitle ?: normalizedArtist,
        artist = normalizedArtist,
    )
}
