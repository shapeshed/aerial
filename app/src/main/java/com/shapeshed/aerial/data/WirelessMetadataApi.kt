package com.shapeshed.aerial.data

import android.util.LruCache
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONArray
import org.json.JSONObject

private val WIRELESS_DOMAIN_REGEX = Regex("""broadcasting\.news|radio\.talksport\.com""")

fun isWirelessStation(station: Station): Boolean = WIRELESS_DOMAIN_REGEX.containsMatchIn(station.streamUrl)

internal data class WirelessMetadataContent(
    val showTitle: String?,
    val showArtworkUrl: String?,
    val showArtworkData: ByteArray?,
    val trackArtist: String? = null,
    val trackTitle: String? = null,
)

// Normalised stream URL → GraphQL stationId enum value (matches the "id" field from /play/api/stations)
private val stationIdCache = ConcurrentHashMap<String, String>()

// (token, expiresAtMs) — public token from thetimes.com/radio/token, valid ~24h
private val cachedToken = AtomicReference<Pair<String, Long>?>(null)

private val wirelessArtworkCache = object : LruCache<String, ByteArray>(4 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ByteArray) = value.size
}

internal fun resolveWirelessStationId(station: Station): String? {
    val normalised = normaliseWirelessUrl(station.streamUrl)
    stationIdCache[normalised]?.let { return it }
    populateStationIdCache()
    return stationIdCache[normalised]
}

private fun populateStationIdCache() {
    if (stationIdCache.isNotEmpty()) return
    val json = requestWirelessJson("https://talksport.com/play/api/stations") ?: return
    try {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id").trim().takeIf { it.isNotBlank() } ?: continue
            val streams = obj.optJSONObject("streams") ?: continue
            for (key in listOf("progressive", "hls")) {
                val url = streams.optString(key).trim().takeIf { it.isNotBlank() } ?: continue
                stationIdCache[normaliseWirelessUrl(url)] = id
            }
        }
    } catch (_: Exception) {}
}

private fun normaliseWirelessUrl(url: String): String = url.substringBefore('?').trimEnd('/')

private fun getToken(): String? {
    val cached = cachedToken.get()
    if (cached != null && System.currentTimeMillis() < cached.second) return cached.first
    val json = requestWirelessJson("https://www.thetimes.com/radio/token") ?: return null
    return try {
        val token = JSONObject(json).optString("access_token").trim().takeIf { it.isNotBlank() } ?: return null
        cachedToken.set(token to (System.currentTimeMillis() + 23 * 60 * 60 * 1000L))
        token
    } catch (_: Exception) { null }
}

internal fun fetchWirelessMetadata(stationId: String): WirelessMetadataContent? {
    val token = getToken() ?: return null
    // stationId is an opaque enum string from the Wireless stations API — no special chars.
    val body = """{"query":"query { onAirNow(stationId: $stationId) { title images { url width metadata } } recentlyPlayed(stationId: $stationId) { title artist endTime } }"}"""
    val json = postWirelessJson("https://api.news.co.uk/audio/v1/graph", body, token) ?: return null
    return try {
        val data = JSONObject(json).optJSONObject("data") ?: return null
        val onAirNow = data.optJSONObject("onAirNow") ?: return null
        val showTitle = onAirNow.optString("title").trim().takeIf { it.isNotBlank() } ?: return null
        val artworkUrl = pickBestArtworkUrl(onAirNow.optJSONArray("images"))
        val artworkData = artworkUrl?.let { fetchWirelessBytes(it) }

        val currentTrack = data.optJSONArray("recentlyPlayed")
            ?.optJSONObject(0)
            ?.takeIf { isCurrentlyPlaying(it.optString("endTime")) }
        val trackArtist = currentTrack?.optString("artist")?.trim()?.takeIf { it.isNotBlank() }
        val trackTitle = currentTrack?.optString("title")?.trim()?.takeIf { it.isNotBlank() }

        WirelessMetadataContent(
            showTitle = showTitle,
            showArtworkUrl = artworkUrl,
            showArtworkData = artworkData,
            trackArtist = trackArtist,
            trackTitle = trackTitle,
        )
    } catch (_: Exception) { null }
}

private fun isCurrentlyPlaying(endTimeStr: String): Boolean {
    if (endTimeStr.isBlank()) return false
    return runCatching { Instant.parse(endTimeStr).isAfter(Instant.now()) }.getOrDefault(false)
}

private fun pickBestArtworkUrl(images: JSONArray?): String? {
    if (images == null || images.length() == 0) return null
    // Prefer "thumbnail" metadata — headshot designed for square display.
    // Fall back to any image if no thumbnail is present.
    var thumbnailUrl: String? = null
    var thumbnailWidth = 0
    var fallbackUrl: String? = null
    var fallbackWidth = 0
    for (i in 0 until images.length()) {
        val img = images.optJSONObject(i) ?: continue
        val width = img.optInt("width", 0)
        val url = img.optString("url").trim().takeIf { it.isNotBlank() } ?: continue
        val isThumbnail = img.optJSONArray("metadata")
            ?.let { (0 until it.length()).any { j -> it.optString(j) == "thumbnail" } } == true
        if (isThumbnail && width > thumbnailWidth && width <= 720) {
            thumbnailUrl = url
            thumbnailWidth = width
        } else if (!isThumbnail && width > fallbackWidth && width <= 720) {
            fallbackUrl = url
            fallbackWidth = width
        }
    }
    return thumbnailUrl ?: fallbackUrl
}

private fun requestWirelessJson(url: String): String? = httpGetJson(url)

private fun postWirelessJson(url: String, body: String, token: String): String? =
    httpPostJson(url, body, extraHeaders = mapOf("Authorization" to "Bearer $token"))

internal fun fetchWirelessBytes(url: String): ByteArray? = httpFetchBytes(url, wirelessArtworkCache)
