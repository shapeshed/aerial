package com.shapeshed.aerial.data

import java.net.URI

/**
 * Playlist container formats Aerial unwraps to a direct stream URL at play time. A station's
 * stream URL is sometimes a link to one of these small text files rather than the audio itself
 * (common for SHOUTcast/Icecast and older Windows Media stations), and ExoPlayer can't play the
 * container — it has to be fetched and the real stream URL extracted first.
 */
private enum class PlaylistFormat { PLS, M3U, ASX }

/**
 * True if [url]'s path ends in a playlist container extension we can unwrap (.pls, .m3u, .asx).
 *
 * `.m3u8` is deliberately NOT a playlist here — that's an HLS manifest ExoPlayer streams
 * natively, so it must pass straight through untouched. Detection is by extension only (no
 * network, no Content-Type): the check runs before every stream open, so the direct-audio
 * majority must stay free, and radio servers routinely mislabel playlist Content-Types anyway.
 */
internal fun isPlaylistUrl(url: String): Boolean = playlistFormatFromExtension(url) != null

private fun playlistFormatFromExtension(url: String): PlaylistFormat? {
    val path = url.substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.endsWith(".pls") -> PlaylistFormat.PLS
        path.endsWith(".m3u8") -> null
        path.endsWith(".m3u") -> PlaylistFormat.M3U
        path.endsWith(".asx") -> PlaylistFormat.ASX
        else -> null
    }
}

/**
 * Resolve [url] to a direct stream URL by fetching and unwrapping playlist containers, following
 * nested playlists up to [maxHops] deep. [fetch] returns a URL's body or null on any failure.
 *
 * Returns the original [url] unchanged when it isn't a playlist, can't be fetched, or can't be
 * parsed — callers hand that straight to ExoPlayer, which surfaces the normal playback error.
 * [fetch] is injected so this stays a pure function (unit-testable without a network).
 */
internal fun resolveStreamUrl(url: String, maxHops: Int = 3, fetch: (String) -> String?): String {
    var current = url
    repeat(maxHops) {
        if (!isPlaylistUrl(current)) return current
        val body = fetch(current) ?: return current
        val next = parsePlaylist(body, current) ?: return current
        if (next == current) return current
        current = next
    }
    return current
}

/**
 * Parse a fetched playlist [body] and return the first stream URL, or null if none is found.
 * [sourceUrl] is the playlist's own URL — its extension picks the format, and it resolves any
 * relative entry to an absolute URL.
 *
 * The extension chooses the parser, but a differing body sniff is tried as a fallback so a
 * mislabelled file (e.g. a `.pls` URL whose body is actually m3u) still resolves.
 */
internal fun parsePlaylist(body: String, sourceUrl: String): String? {
    val clean = body.trimStart('\uFEFF')
    val candidates = listOfNotNull(playlistFormatFromExtension(sourceUrl), sniffFormat(clean)).distinct()
    for (format in candidates) {
        val entry = when (format) {
            PlaylistFormat.PLS -> parsePls(clean)
            PlaylistFormat.M3U -> parseM3u(clean)
            PlaylistFormat.ASX -> parseAsx(clean)
        }
        if (entry != null) return absolutize(entry, sourceUrl)
    }
    return null
}

private fun sniffFormat(body: String): PlaylistFormat? {
    val head = body.trimStart().take(256).lowercase()
    return when {
        head.startsWith("[playlist]") -> PlaylistFormat.PLS
        head.startsWith("<asx") -> PlaylistFormat.ASX
        head.startsWith("#extm3u") -> PlaylistFormat.M3U
        else -> null
    }
}

// FileN=<url>: capture N so the lowest-numbered entry (the primary stream) wins.
private val PLS_FILE_REGEX = Regex("(?i)File(\\d+)\\s*=\\s*(.+)")

private fun parsePls(body: String): String? =
    body.lineSequence()
        .mapNotNull { line ->
            val match = PLS_FILE_REGEX.matchEntire(line.trim()) ?: return@mapNotNull null
            val index = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val url = match.groupValues[2].trim()
            if (url.isEmpty()) null else index to url
        }
        .minByOrNull { it.first }
        ?.second

private fun parseM3u(body: String): String? =
    body.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() && !it.startsWith("#") }

// ASX is frequently not well-formed XML (uppercase tags, unquoted or unclosed elements), so a
// tolerant match for the first <ref href="..."> beats a strict XML parser — and keeps this file
// free of Android framework XML APIs so it stays unit-testable on the plain JVM.
private val ASX_REF_REGEX = Regex("(?i)<ref\\b[^>]*\\bhref\\s*=\\s*[\"']([^\"']+)[\"']")

private fun parseAsx(body: String): String? =
    ASX_REF_REGEX.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

private fun absolutize(entry: String, baseUrl: String): String {
    return try {
        if (URI(entry).isAbsolute) entry else URI(baseUrl).resolve(entry).toString()
    } catch (_: Exception) {
        // Not parseable as a URI (unusual entry) — hand it back as-is and let ExoPlayer judge it.
        entry
    }
}
