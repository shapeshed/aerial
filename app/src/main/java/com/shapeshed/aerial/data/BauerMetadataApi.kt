package com.shapeshed.aerial.data

import android.util.LruCache
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val BAUER_DOMAIN_REGEX = Regex("""hellorayo\.co\.uk|sharp-stream\.com|bauerradio\.com|planetradio\.co\.uk|audioxi\.(com|ie)""")


fun isBauerStation(station: Station): Boolean = BAUER_DOMAIN_REGEX.containsMatchIn(station.streamUrl)

fun bauerStreamUrl(station: Station): String {
    if (!isBauerStation(station)) return station.streamUrl
    val skey = System.currentTimeMillis() / 1000
    val sep = if ('?' in station.streamUrl) '&' else '?'
    return "${station.streamUrl}${sep}aw_0_1st.skey=$skey"
}

internal data class BauerMetadataContent(
    val showTitle: String?,
    val showArtworkUrl: String?,
    val showArtworkData: ByteArray?,
    val trackArtist: String?,
    val trackTitle: String?,
    val trackArtworkUrl: String?,
    val trackArtworkData: ByteArray?,
)

internal suspend fun fetchBauerMetadata(station: Station): BauerMetadataContent? = withContext(Dispatchers.IO) {
    val (stationCode, country) = resolveBauerStationCode(station) ?: return@withContext null
    val url = "https://listenapi.planetradio.co.uk/api9.2/stations_nowplaying/$country" +
        "?StationCode%5B%5D=$stationCode&premium=1"
    val json = requestBauerJson(url) ?: return@withContext null
    val entry = parseNowPlayingEntry(json, stationCode) ?: return@withContext null
    if (entry.trackArtist == null && entry.trackTitle == null && entry.showTitle == null) return@withContext null

    val trackArtworkUrl = entry.trackImageUrl
    val trackArtworkData = trackArtworkUrl?.let { fetchBauerBytes(it) }
    val showArtworkUrl = entry.showImageUrl
    val showArtworkData = showArtworkUrl?.let { fetchBauerBytes(it) }

    BauerMetadataContent(
        showTitle = entry.showTitle,
        showArtworkUrl = showArtworkUrl,
        showArtworkData = showArtworkData,
        trackArtist = entry.trackArtist,
        trackTitle = entry.trackTitle,
        trackArtworkUrl = trackArtworkUrl,
        trackArtworkData = trackArtworkData,
    )
}

// Cache: country → (normalised stream URL → station code)
// Populated lazily per country; only cached on a successful non-empty fetch.
private val countryStationMap = ConcurrentHashMap<String, Map<String, String>>()
// Per-country gate: prevents concurrent duplicate fetches for the same country.
private val countryFetching = ConcurrentHashMap<String, AtomicBoolean>()

private val BAUER_COUNTRIES = listOf("GB", "IE", "AU")

internal fun resolveBauerStationCode(station: Station): Pair<String, String>? {
    val normalised = normaliseStreamUrl(station.streamUrl)
    val filename = normalised.substringAfterLast('/')
    for (country in BAUER_COUNTRIES) {
        val stations = getStationsForCountry(country)
        // Exact URL match first.
        stations[normalised]?.let { code -> return code to country }
        // Filename fallback: "stream-kiss.hellorayo.co.uk/kissdance.aac" matches
        // "live-bauerkiss.sharp-stream.com/kissdance.aac" from the stations list.
        if (filename.isNotBlank()) {
            stations.entries.firstOrNull { (url, _) ->
                url.substringAfterLast('/') == filename
            }?.let { (_, code) -> return code to country }
        }
    }
    return null
}

private fun getStationsForCountry(country: String): Map<String, String> {
    countryStationMap[country]?.let { return it }
    val gate = countryFetching.computeIfAbsent(country) { AtomicBoolean(false) }
    if (!gate.compareAndSet(false, true)) return countryStationMap[country] ?: emptyMap()
    try {
        val json = requestBauerJson("https://listenapi.planetradio.co.uk/api9.2/stations/$country")
            ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val code = obj.optString("stationCode").trim().takeIf { it.isNotBlank() } ?: continue
                val streams = obj.optJSONArray("stationStreams") ?: continue
                for (j in 0 until streams.length()) {
                    val streamUrl = streams.optJSONObject(j)
                        ?.optString("streamUrl")?.trim()?.takeIf { it.isNotBlank() } ?: continue
                    result[normaliseStreamUrl(streamUrl)] = code
                }
            }
        } catch (_: Exception) {
            return emptyMap()
        }
        if (result.isNotEmpty()) countryStationMap[country] = result
        return result
    } finally {
        if (!countryStationMap.containsKey(country)) gate.set(false) // allow retry on failure
    }
}

// Strips query string so "https://stream.foo.com/radio.aac?direct=true&skey=123"
// matches "https://stream.foo.com/radio.aac" from the stations list.
private fun normaliseStreamUrl(url: String): String = url.substringBefore('?').trimEnd('/')

private data class BauerNowPlayingEntry(
    val showTitle: String?,
    val showImageUrl: String?,
    val trackArtist: String?,
    val trackTitle: String?,
    val trackImageUrl: String?,
)

private fun parseNowPlayingEntry(json: String, stationCode: String): BauerNowPlayingEntry? {
    return try {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("stationCode") != stationCode) continue
            val nowPlaying = obj.optJSONObject("stationNowPlaying")
            val onAir = obj.optJSONObject("stationOnAir")
            return BauerNowPlayingEntry(
                showTitle = onAir?.optString("episodeTitle")?.trim()?.takeIf { it.isNotBlank() },
                showImageUrl = onAir?.optString("episodeImageUrl")?.trim()?.takeIf { it.isNotBlank() },
                trackArtist = nowPlaying?.optString("nowPlayingArtist")?.trim()?.takeIf { it.isNotBlank() },
                trackTitle = nowPlaying?.optString("nowPlayingTrack")?.trim()?.takeIf { it.isNotBlank() },
                trackImageUrl = nowPlaying?.optString("nowPlayingImage")?.trim()?.takeIf { it.isNotBlank() },
            )
        }
        null
    } catch (_: Exception) {
        null
    }
}

private fun requestBauerJson(url: String): String? = httpGetJson(url)

private fun fetchBauerBytes(url: String): ByteArray? = httpFetchBytes(url, bauerArtworkCache)

private val bauerArtworkCache = object : LruCache<String, ByteArray>(4 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ByteArray) = value.size
}
