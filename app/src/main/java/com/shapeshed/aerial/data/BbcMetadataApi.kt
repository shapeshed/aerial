package com.shapeshed.aerial.data

import android.util.LruCache
import com.shapeshed.aerial.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

fun isBbcStation(station: Station): Boolean = BBC_SERVICE_ID_REGEX.containsMatchIn(station.streamUrl)

internal suspend fun fetchBbcMetadata(station: Station): BbcMetadataContent? = withContext(Dispatchers.IO) {
    val serviceId = resolveBbcServiceId(station) ?: return@withContext null
    val segment = fetchBbcSegments(serviceId)?.let(::parseBbcTrackResponse)
    val broadcast = fetchBbcBroadcasts(serviceId)?.let(::parseBbcBroadcastResponse)
    val showTitle = broadcast?.showTitle
    val episodeTitle = broadcast?.episodeTitle
    val trackArtist = segment?.artistTitle
    val trackTitle = segment?.trackTitle
    val trackArtworkUrl = segment?.artworkUrl
    val trackArtworkData = trackArtworkUrl?.let { fetchBbcBytes(it) }
    val programmeArtworkUrl = broadcast?.artworkUrl
    val programmeArtworkData = programmeArtworkUrl?.let { fetchBbcBytes(it) }
    if (showTitle == null && trackArtist == null && trackTitle == null) return@withContext null
    BbcMetadataContent(
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

private val BBC_SERVICE_ID_REGEX = Regex("bbc_[a-z0-9_]+")

internal fun resolveBbcServiceId(station: Station): String? {
    val serviceIds = getBbcServiceIds()
    return BBC_SERVICE_ID_REGEX.findAll(station.streamUrl)
        .map { it.value }
        .firstOrNull { it in serviceIds }
}

private val cachedServiceIds = AtomicReference<Set<String>?>(null)

private fun getBbcServiceIds(): Set<String> {
    cachedServiceIds.get()?.let { return it }
    val fetched = fetchBbcServiceIds()
    // Only cache on success — a failed fetch (empty set) should be retried next time.
    if (fetched.isNotEmpty()) cachedServiceIds.compareAndSet(null, fetched)
    return fetched
}

private fun fetchBbcServiceIds(): Set<String> {
    val json = requestBbcJson("https://rms.api.bbc.co.uk/v2/networks?limit=100") ?: return emptySet()
    return try {
        val data = JSONObject(json).optJSONArray("data") ?: return emptySet()
        (0 until data.length())
            .mapNotNull { data.optJSONObject(it)?.optString("default_service_id")?.trim()?.takeIf { s -> s.isNotBlank() } }
            .toSet()
    } catch (_: Exception) {
        emptySet()
    }
}

internal fun parseBbcTrackResponse(json: String): BbcTrackItem? {
    return parseBbcTrackPayload(JSONObject(json))
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

internal data class BbcTrackItem(
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

internal data class BbcMetadataContent(
    val showTitle: String?,     // programme/show name (broadcasts.titles.primary)
    val episodeTitle: String?,  // episode title (broadcasts.titles.secondary)
    val trackArtist: String?,
    val trackTitle: String?,    // track title from segments (null for talk stations)
    val programmeArtworkUrl: String?,
    val programmeArtworkData: ByteArray?,
    val trackArtworkUrl: String?,
    val trackArtworkData: ByteArray?,
)

private fun parseBbcTrackPayload(root: JSONObject): BbcTrackItem? {
    val items = root.optJSONArray("data") ?: return null
    if (items.length() == 0) return null
    // now_playing lives at offset.now_playing, not the item root.
    // When talking, the API returns the last played track with offset.now_playing=false —
    // returning null here clears the track block and shows the station image again.
    val chosen = (0 until items.length())
        .mapNotNull { items.optJSONObject(it) }
        .firstOrNull { it.optJSONObject("offset")?.optBoolean("now_playing", false) == true }
        ?: return null

    val titles = chosen.optJSONObject("titles") ?: return null
    val artistTitle = titles.optString("primary").trim().takeIf { it.isNotBlank() } ?: return null
    val trackTitle = titles.optString("secondary").trim().takeIf { it.isNotBlank() } ?: return null
    val artworkUrl = chosen.optString("image_url").trim().takeIf { it.isNotBlank() }?.replace("{recipe}", "640x640")
    return BbcTrackItem(
        artistTitle = artistTitle,
        trackTitle = trackTitle,
        artworkUrl = artworkUrl,
        artworkData = null,
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
    return BbcBroadcastItem(
        showTitle = primary,
        episodeTitle = secondary,
        artworkUrl = artworkUrl,
        artworkData = null,
    )
}

private fun parseInstantOrNull(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

private fun requestBbcJson(url: String): String? {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "Aerial/${BuildConfig.VERSION_NAME} (Android)")
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
        conn.setRequestProperty("User-Agent", "Aerial/${BuildConfig.VERSION_NAME} (Android)")
        try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { input ->
                input.readBytes().also { bytes -> artworkCache.put(url, bytes) }
            }
        } finally {
            conn.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}

private val artworkCache = object : LruCache<String, ByteArray>(4 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ByteArray) = value.size
}
