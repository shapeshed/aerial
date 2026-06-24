package com.shapeshed.aerial.data

import android.util.LruCache
import com.shapeshed.aerial.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject

private val GLOBAL_DOMAIN_REGEX = Regex("""musicradio\.com|thisisdax\.com""")

fun isGlobalStation(station: Station): Boolean = GLOBAL_DOMAIN_REGEX.containsMatchIn(station.streamUrl)

internal data class GlobalMetadataContent(
    val showTitle: String?,
    val showArtworkUrl: String?,
    val showArtworkData: ByteArray?,
    val trackArtist: String?,
    val trackTitle: String?,
    val trackArtworkUrl: String?,
    val trackArtworkData: ByteArray?,
)

private val heraldIdCache = ConcurrentHashMap<String, Int>()
private val globalArtworkCache = object : LruCache<String, ByteArray>(4 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ByteArray) = value.size
}

internal fun resolveGlobalHeraldId(station: Station): Int? {
    val normalised = normaliseGlobalUrl(station.streamUrl)
    heraldIdCache[normalised]?.let { return it }
    populateHeraldIdCache()
    return heraldIdCache[normalised]
}

private fun populateHeraldIdCache() {
    if (heraldIdCache.isNotEmpty()) return
    val json = requestGlobalJson("https://bff-web-guacamole.musicradio.com/stations/") ?: return
    try {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val heraldId = obj.optInt("heraldId", -1).takeIf { it > 0 } ?: continue
            val primaryUrl = obj.optString("streamUrl").trim().takeIf { it.isNotBlank() } ?: continue
            heraldIdCache[normaliseGlobalUrl(primaryUrl)] = heraldId
            obj.optJSONObject("stream")?.let { streams ->
                for (key in listOf("icecastSd", "icecastHd", "hls")) {
                    val url = streams.optString(key).trim().takeIf { it.isNotBlank() } ?: continue
                    heraldIdCache[normaliseGlobalUrl(url)] = heraldId
                }
            }
        }
    } catch (_: Exception) {}
}

private fun normaliseGlobalUrl(url: String): String = url.substringBefore('?').trimEnd('/')

internal fun parseGlobalNowPlaying(json: String): GlobalMetadataContent? {
    return try {
        val obj = JSONObject(json)
        if (obj.optString("type") != "station") return null
        val nowPlaying = obj.optJSONObject("now_playing") ?: return null
        val currentShow = obj.optJSONObject("current_show")
        val npType = nowPlaying.optString("type")

        val showTitle = currentShow?.optString("name")?.trim()?.takeIf { it.isNotBlank() }
            ?: if (npType == "show") nowPlaying.optString("name").trim().takeIf { it.isNotBlank() } else null
        val showArtworkUrl = (currentShow ?: nowPlaying).optString("artwork").trim().takeIf { it.isNotBlank() }

        val trackTitle: String?
        val trackArtist: String?
        val trackArtworkUrl: String?
        if (npType == "track") {
            trackTitle = nowPlaying.optString("title").trim().takeIf { it.isNotBlank() }
            trackArtist = nowPlaying.optString("artist").trim().takeIf { it.isNotBlank() }
            trackArtworkUrl = nowPlaying.optString("artwork").trim().takeIf { it.isNotBlank() }
        } else {
            trackTitle = null
            trackArtist = null
            trackArtworkUrl = null
        }

        if (showTitle == null && trackTitle == null) return null

        val showArtworkData = showArtworkUrl?.let { fetchGlobalBytes(it) }
        val trackArtworkData = if (trackArtworkUrl != null && trackArtworkUrl != showArtworkUrl) {
            fetchGlobalBytes(trackArtworkUrl)
        } else null

        GlobalMetadataContent(
            showTitle = showTitle,
            showArtworkUrl = showArtworkUrl,
            showArtworkData = showArtworkData,
            trackArtist = trackArtist,
            trackTitle = trackTitle,
            trackArtworkUrl = trackArtworkUrl,
            trackArtworkData = trackArtworkData,
        )
    } catch (_: Exception) { null }
}

private fun requestGlobalJson(url: String): String? {
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
    } catch (_: Exception) { null }
}

internal fun fetchGlobalBytes(url: String): ByteArray? {
    globalArtworkCache[url]?.let { return it }
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "Aerial/${BuildConfig.VERSION_NAME} (Android)")
        try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { it.readBytes().also { bytes -> globalArtworkCache.put(url, bytes) } }
        } finally {
            conn.disconnect()
        }
    } catch (_: Exception) { null }
}
