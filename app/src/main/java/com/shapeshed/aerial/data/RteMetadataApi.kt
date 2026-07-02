package com.shapeshed.aerial.data

import android.util.LruCache
import org.json.JSONObject

private const val LIVE_STATIONS_URL = "https://www.rte.ie/radio/live_stations/json"

private val artworkCache = object : LruCache<String, ByteArray>(4 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ByteArray) = value.size
}

internal fun fetchRteBytes(url: String): ByteArray? = httpFetchBytes(url, artworkCache)

internal fun isRteStation(station: Station): Boolean = station.provider == "rte"

internal data class RteNowPlaying(
    val showName: String,
    val showDescription: String?,
    val artworkUrl: String?,
)

/**
 * Fetches RTÉ's live stations feed and returns the currently-airing show for
 * [stationSlug] (matches the aerial-registry provider_id for RTÉ stations, e.g.
 * "radio1", "2fm"). Unlike Rinse's shared schedule feed, RTÉ's response already
 * embeds each station's current show under `liveListing` — no time-window
 * matching needed.
 */
internal suspend fun fetchRteNowPlaying(stationSlug: String): RteNowPlaying? {
    val json = httpGetJson(LIVE_STATIONS_URL) ?: return null
    return parseRteLiveStations(json, stationSlug)
}

internal fun parseRteLiveStations(json: String, stationSlug: String): RteNowPlaying? {
    return try {
        val stations = JSONObject(json).optJSONArray("stations") ?: return null
        for (i in 0 until stations.length()) {
            val station = stations.optJSONObject(i) ?: continue
            if (station.optString("slug") != stationSlug) continue
            val listing = station.optJSONObject("liveListing") ?: return null
            val showName = listing.optNonBlankString("showName") ?: return null
            return RteNowPlaying(
                showName = showName,
                showDescription = listing.optNonBlankString("showDescription"),
                artworkUrl = listing.optNonBlankString("showImage1x1"),
            )
        }
        null
    } catch (_: Exception) {
        null
    }
}

/**
 * org.json's optString returns the literal string "null" (not blank/empty) for a key
 * whose value is explicit JSON null, since JSONObject.NULL is a non-null sentinel object.
 */
private fun JSONObject.optNonBlankString(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf { it.isNotBlank() }
