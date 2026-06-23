package com.shapeshed.aerial.data

import android.util.Log
import com.shapeshed.aerial.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "StationDiscovery"

data class DiscoveredStation(
    val name: String,
    val streamUrl: String,
    val logoUrl: String = "",
)

interface StationImporter {
    fun discoverStations(): List<DiscoveredStation>
}

fun discoverBbcStations(): List<DiscoveredStation> {
    val json = requestDiscoveryJson("https://rms.api.bbc.co.uk/v2/networks?limit=100") ?: return emptyList()
    val data = runCatching { JSONObject(json).optJSONArray("data") }.getOrNull() ?: return emptyList()
    val result = mutableListOf<DiscoveredStation>()
    for (i in 0 until data.length()) {
        val obj = data.optJSONObject(i) ?: continue
        val serviceId = obj.optString("default_service_id").trim().takeIf { it.isNotBlank() } ?: continue
        val name = obj.optString("title").trim().takeIf { it.isNotBlank() }
            ?: obj.optJSONObject("titles")?.optString("primary")?.trim()?.takeIf { it.isNotBlank() }
            ?: bbcServiceIdToName(serviceId)
        val logoUrl = "https://sounds.files.bbci.co.uk/3.9.4/networks/$serviceId/colour_default.svg"
        val streamUrl = resolveBbcStreamUrl(serviceId) ?: continue
        result.add(DiscoveredStation(name = name, streamUrl = streamUrl, logoUrl = logoUrl))
    }
    Log.d(TAG, "BBC: discovered ${result.size} stations")
    return result
}

// Fetches the BBC master HLS manifest for a service and extracts the first variant stream URL.
// The manifest UUID is a stable BBC infrastructure identifier for the UK HLS endpoint.
private fun resolveBbcStreamUrl(serviceId: String): String? {
    val manifest = requestDiscoveryJson(
        "https://a.files.bbci.co.uk/ms6/live/3441A116-B12E-4D2F-ACA8-C1984642FA4B/audio/simulcast/hls/uk/pc_hd_abr_v2/ak/$serviceId.m3u8"
    ) ?: return null
    return manifest.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("http") }
}

fun discoverBauerStations(): List<DiscoveredStation> {
    val result = mutableListOf<DiscoveredStation>()
    for (country in listOf("GB", "IE")) {
        val json = requestDiscoveryJson("https://listenapi.planetradio.co.uk/api9.2/stations/$country") ?: continue
        val arr = runCatching { JSONArray(json) }.getOrNull() ?: continue
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val name = obj.optString("stationName").trim().takeIf { it.isNotBlank() } ?: continue
            val logoUrl = obj.optString("stationListenBarLogo").trim().takeIf { it.isNotBlank() } ?: ""
            val streamUrl = obj.optJSONArray("stationStreams")
                ?.optJSONObject(0)?.optString("streamUrl")?.trim()?.takeIf { it.isNotBlank() } ?: continue
            result.add(DiscoveredStation(name = name, streamUrl = streamUrl, logoUrl = logoUrl))
        }
    }
    Log.d(TAG, "Bauer: discovered ${result.size} stations")
    return result
}

fun discoverGlobalStations(): List<DiscoveredStation> {
    val json = requestDiscoveryJson("https://bff-web-guacamole.musicradio.com/stations/") ?: return emptyList()
    val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
    // Build brand slug → logo URL map with one RadioBrowser lookup per unique brand.
    val brandLogoCache = mutableMapOf<String, String>()
    val result = mutableListOf<DiscoveredStation>()
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val name = obj.optString("name").trim().takeIf { it.isNotBlank() } ?: continue
        val streamUrl = obj.optString("streamUrl").trim().takeIf { it.isNotBlank() }
            ?: obj.optJSONObject("stream")?.optString("icecastSd")?.trim()?.takeIf { it.isNotBlank() }
            ?: continue
        val brandSlug = obj.optJSONObject("brand")?.optString("slug")?.trim()?.takeIf { it.isNotBlank() } ?: ""
        val logoUrl = if (brandSlug.isNotBlank()) {
            brandLogoCache.getOrPut(brandSlug) { lookupRadioBrowserFavicon(brandSlug, countryCode = "GB") }
        } else ""
        result.add(DiscoveredStation(name = name, streamUrl = streamUrl, logoUrl = logoUrl))
    }
    Log.d(TAG, "Global: discovered ${result.size} stations")
    return result
}

private fun lookupRadioBrowserFavicon(name: String, countryCode: String = ""): String {
    val encoded = URLEncoder.encode(name, "UTF-8")
    val cc = if (countryCode.isNotBlank()) "&countrycode=$countryCode" else ""
    val json = requestDiscoveryJson(
        "https://de1.api.radio-browser.info/json/stations/search?name=$encoded$cc&limit=5&hidebroken=true"
    ) ?: return ""
    return try {
        val arr = JSONArray(json)
        (0 until arr.length())
            .mapNotNull { arr.optJSONObject(it)?.optString("favicon")?.trim()?.takeIf { s -> s.isNotBlank() } }
            .firstOrNull() ?: ""
    } catch (_: Exception) { "" }
}

fun discoverWirelessStations(): List<DiscoveredStation> {
    val json = requestDiscoveryJson("https://talksport.com/play/api/stations") ?: return emptyList()
    val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
    val result = mutableListOf<DiscoveredStation>()
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val name = obj.optString("name").trim().takeIf { it.isNotBlank() } ?: continue
        val streams = obj.optJSONObject("streams") ?: continue
        val streamUrl = streams.optString("progressive").trim().takeIf { it.isNotBlank() }
            ?: streams.optString("hls").trim().takeIf { it.isNotBlank() }
            ?: continue
        val logoUrl = obj.optString("thumbnail").trim().takeIf { it.isNotBlank() }
            ?: obj.optString("logo").trim().takeIf { it.isNotBlank() }
            ?: ""
        result.add(DiscoveredStation(name = name, streamUrl = streamUrl, logoUrl = logoUrl))
    }
    Log.d(TAG, "Wireless: discovered ${result.size} stations")
    return result
}

private fun requestDiscoveryJson(url: String): String? {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "Aerial/${BuildConfig.VERSION_NAME} (Android)")
        try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to fetch $url: ${e.message}")
        null
    }
}

// Derives a human-readable name from a BBC service ID when the API doesn't supply one.
private fun bbcServiceIdToName(serviceId: String): String {
    return serviceId
        .removePrefix("bbc_")
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { word ->
            when (word.lowercase()) {
                "bbc" -> "BBC"
                "fm" -> "FM"
                "mw" -> "MW"
                "lw" -> "LW"
                "am" -> "AM"
                "1xtra" -> "1Xtra"
                "6music" -> "6 Music"
                "wm" -> "WM"
                else -> word.replaceFirstChar { it.uppercase() }
            }
        }
        .let { if (!it.startsWith("BBC")) "BBC $it" else it }
}
