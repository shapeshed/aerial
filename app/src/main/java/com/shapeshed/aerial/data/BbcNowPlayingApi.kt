package com.shapeshed.aerial.data

import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

fun isBbcStation(station: Station): Boolean = resolveBbcServiceId(station) != null

internal suspend fun fetchBbcNowPlaying(station: Station): BbcNowPlayingContent? = withContext(Dispatchers.IO) {
    val serviceId = resolveBbcServiceId(station) ?: return@withContext null
    val segment = fetchBbcSegments(serviceId)?.let(::parseBbcNowPlayingResponse)
    val broadcast = fetchBbcBroadcasts(serviceId)?.let(::parseBbcBroadcastResponse)
    val showTitle = broadcast?.showTitle
    val episodeTitle = broadcast?.episodeTitle
    val trackArtist = segment?.artistTitle
    val trackTitle = segment?.trackTitle
    val trackArtworkUrl = segment?.artworkUrl
    val trackArtworkData = segment?.artworkData
    val programmeArtworkUrl = broadcast?.artworkUrl
    val programmeArtworkData = broadcast?.artworkData
    if (showTitle == null && trackArtist == null && trackTitle == null) return@withContext null
    BbcNowPlayingContent(
        showTitle = showTitle,
        episodeTitle = episodeTitle,
        trackArtist = trackArtist,
        trackTitle = trackTitle,
        programmeArtworkUrl = programmeArtworkUrl,
        programmeArtworkData = programmeArtworkData,
        trackArtworkUrl = trackArtworkUrl,
        trackArtworkData = trackArtworkData,
    )
}

internal fun resolveBbcServiceId(station: Station): String? {
    extractBbcServiceIdFromUrl(station.streamUrl)?.let { return it }

    val haystack = station.name.lowercase(Locale.US)
    val candidates = listOf(
        "bbc_radio_one" to listOf("radio 1", "radio_one"),
        "bbc_radio_two" to listOf("radio 2", "radio_two"),
        "bbc_radio_three" to listOf("radio 3", "radio_three"),
        "bbc_radio_fourfm" to listOf("radio 4", "radio_fourfm", "radio four fm"),
        "bbc_radio_five_live" to listOf("five live", "radio_five_live", "radio five live"),
        "bbc_6music" to listOf("6 music", "6music"),
        "bbc_asian_network" to listOf("asian network", "asian_network"),
    )

    return candidates.firstOrNull { (_, needles) ->
        needles.any { needle -> haystack.contains(needle) }
    }?.first
}

internal fun extractBbcServiceIdFromUrl(streamUrl: String): String? {
    val path = runCatching { URL(streamUrl).path }.getOrNull() ?: return null
    val segments = path.split('/').filter { it.isNotBlank() }
    val liveIndex = segments.indexOf("live")
    if (liveIndex < 0 || liveIndex + 2 >= segments.size) return null
    val region = segments[liveIndex + 1]
    if (region != "uk" && region != "ww") return null
    return segments[liveIndex + 2].lowercase(Locale.US).takeIf { it.startsWith("bbc_") }
}

internal fun parseBbcNowPlayingResponse(json: String): BbcNowPlayingItem? {
    return parseBbcItemPayload(JSONObject(json))
}

internal fun parseBbcBroadcastResponse(json: String, now: Instant = Instant.now()): BbcBroadcastItem? {
    return parseBbcBroadcastItemPayload(JSONObject(json), now)
}

private fun fetchBbcSegments(serviceId: String): String? {
    return requestBbcJson("https://rms.api.bbc.co.uk/v2/services/$serviceId/segments/latest?experience=domestic&offset=0&limit=1")
}

private fun fetchBbcBroadcasts(serviceId: String): String? {
    return requestBbcJson("https://rms.api.bbc.co.uk/v2/broadcasts/latest?service=$serviceId&on_air=now")
}

internal data class BbcNowPlayingItem(
    val artistTitle: String,
    val trackTitle: String,
    val artworkUrl: String?,
    val artworkData: ByteArray?,
)

internal data class BbcBroadcastItem(
    val showTitle: String,
    val episodeTitle: String?,
    val artworkUrl: String?,
    val artworkData: ByteArray?,
)

internal data class BbcNowPlayingContent(
    val showTitle: String?,     // programme/show name (broadcasts.titles.primary)
    val episodeTitle: String?,  // episode title (broadcasts.titles.secondary)
    val trackArtist: String?,
    val trackTitle: String?,    // track title from segments (null for talk stations)
    val programmeArtworkUrl: String?,
    val programmeArtworkData: ByteArray?,
    val trackArtworkUrl: String?,
    val trackArtworkData: ByteArray?,
)

private fun parseBbcItemPayload(root: JSONObject): BbcNowPlayingItem? {
    val items = root.optJSONArray("data") ?: return null
    if (items.length() == 0) return null
    val chosen = (0 until items.length())
        .mapNotNull { items.optJSONObject(it) }
        .firstOrNull { it.optJSONObject("offset")?.optBoolean("now_playing") == true }
        ?: return null

    val titles = chosen.optJSONObject("titles") ?: return null
    val artistTitle = titles.optString("primary").trim().takeIf { it.isNotBlank() } ?: return null
    val trackTitle = titles.optString("secondary").trim().takeIf { it.isNotBlank() } ?: return null
    val artworkUrl = chosen.optString("image_url").trim().takeIf { it.isNotBlank() }?.replace("{recipe}", "640x640")
    val artworkData = artworkUrl?.let { fetchBbcBytes(it) }
    return BbcNowPlayingItem(
        artistTitle = artistTitle,
        trackTitle = trackTitle,
        artworkUrl = artworkUrl,
        artworkData = artworkData,
    )
}

private fun parseBbcBroadcastItemPayload(root: JSONObject, now: Instant): BbcBroadcastItem? {
    val items = root.optJSONArray("data") ?: return null
    val chosen = (0 until items.length())
        .mapNotNull { items.optJSONObject(it) }
        .firstOrNull { item ->
            item.optBoolean("on_air", false) ||
                run {
                    val start = item.optString("start").trim().takeIf { it.isNotBlank() }?.let(::parseInstantOrNull)
                    val end = item.optString("end").trim().takeIf { it.isNotBlank() }?.let(::parseInstantOrNull)
                    start != null && end != null && !now.isBefore(start) && now.isBefore(end)
                }
        }
        ?: (0 until items.length()).mapNotNull { items.optJSONObject(it) }.firstOrNull()
        ?: return null

    val programme = chosen.optJSONObject("programme") ?: chosen
    val titles = programme.optJSONObject("titles") ?: chosen.optJSONObject("titles") ?: return null
    val primary = titles.optString("primary").trim().takeIf { it.isNotBlank() } ?: return null
    val secondary = titles.optString("secondary").trim().takeIf { it.isNotBlank() }
    val artworkUrl = programme.optJSONArray("images")
        ?.optJSONObject(0)
        ?.optString("url")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.replace("{recipe}", "640x640")
        ?: chosen.optString("image_url").trim().takeIf { it.isNotBlank() }?.replace("{recipe}", "640x640")
    val artworkData = artworkUrl?.let { fetchBbcBytes(it) }
    return BbcBroadcastItem(
        showTitle = primary,
        episodeTitle = secondary,
        artworkUrl = artworkUrl,
        artworkData = artworkData,
    )
}

private fun parseInstantOrNull(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

private fun requestBbcJson(url: String): String? {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "Aerial/0.2.0")
        try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}

private fun fetchBbcBytes(url: String): ByteArray? {
    artworkCache[url]?.let { return it }
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "Aerial/0.2.0")
        try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { input ->
                input.readBytes().also { bytes -> artworkCache[url] = bytes }
            }
        } finally {
            conn.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}

private val artworkCache = ConcurrentHashMap<String, ByteArray>()
