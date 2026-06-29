package com.shapeshed.aerial.data

/**
 * Split a raw ICY stream title into (artist, trackTitle).
 * Most streams use "Artist - Title"; returns (null, raw) when no separator is found.
 */
fun parseIcyTitle(raw: String): Pair<String?, String> {
    val idx = raw.indexOf(" - ")
    return if (idx > 0) Pair(raw.substring(0, idx).trim(), raw.substring(idx + 3).trim())
    else Pair(null, raw.trim())
}
