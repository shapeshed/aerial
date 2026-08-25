package com.shapeshed.aerial.data

/**
 * Split a raw ICY stream title into (artist, trackTitle).
 * Most streams use "Artist - Title"; some use "Artist = Title". Returns (null, raw) when no
 * separator is found.
 */
fun parseIcyTitle(raw: String): Pair<String?, String> {
    val separator = listOf(" - ", " = ")
        .mapNotNull { candidate -> raw.indexOf(candidate).takeIf { it > 0 }?.let { it to candidate } }
        .minByOrNull { it.first }
    return if (separator != null) {
        val (idx, delimiter) = separator
        Pair(raw.substring(0, idx).trim(), raw.substring(idx + delimiter.length).trim())
    }
    else Pair(null, raw.trim())
}

data class ParsedTrackMetadata(
    val artist: String?,
    val title: String?,
)

/** Normalizes metadata whether the source supplies separate fields or a combined ICY title. */
fun parseTrackMetadata(title: String?, artist: String? = null): ParsedTrackMetadata {
    val cleanTitle = title?.trim()?.takeIf { it.isNotEmpty() }
    val cleanArtist = artist?.trim()?.takeIf { it.isNotEmpty() }
    if (cleanArtist != null) return ParsedTrackMetadata(cleanArtist, cleanTitle)
    if (cleanTitle == null) return ParsedTrackMetadata(null, null)
    val (parsedArtist, parsedTitle) = parseIcyTitle(cleanTitle)
    return ParsedTrackMetadata(parsedArtist, parsedTitle)
}
