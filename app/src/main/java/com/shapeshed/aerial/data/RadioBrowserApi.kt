package com.shapeshed.aerial.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder

object RadioBrowserApi {

    private val fallbackServers = listOf(
        "de1.api.radio-browser.info",
        "nl1.api.radio-browser.info",
        "at1.api.radio-browser.info",
    )

    suspend fun discoverServer(): String = withContext(Dispatchers.IO) {
        try {
            val addresses = InetAddress.getAllByName("all.api.radio-browser.info")
            val hosts = addresses.mapNotNull { addr ->
                val hostname = addr.canonicalHostName
                // filter out bare IP addresses — we need a resolvable hostname for TLS
                if (hostname != addr.hostAddress && hostname.contains('.')) hostname else null
            }.filter { it != "all.api.radio-browser.info" }
            hosts.randomOrNull() ?: fallbackServers.random()
        } catch (_: Exception) {
            fallbackServers.random()
        }
    }

    suspend fun search(server: String, query: String): List<RadioBrowserStation> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://$server/json/stations/search" +
            "?name=$encoded&limit=200&order=votes&reverse=true&hidebroken=true"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Aerial/1.0 (Android)")
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        try {
            val body = conn.inputStream.bufferedReader().readText()
            parseSearchResponse(body)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun registerClick(server: String, uuid: String) = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://$server/json/url/$uuid").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Aerial/1.0 (Android)")
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.inputStream.use { it.readBytes() }
            conn.disconnect()
        }
    }

    suspend fun vote(server: String, uuid: String) = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://$server/json/vote/$uuid").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Aerial/1.0 (Android)")
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.inputStream.use { it.readBytes() }
            conn.disconnect()
        }
    }

    internal fun parseSearchResponse(json: String): List<RadioBrowserStation> {
        val array = JSONArray(json)
        val raw = buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(RadioBrowserStation(
                    stationuuid = o.optString("stationuuid"),
                    name        = o.optString("name"),
                    urlResolved = o.optString("url_resolved").ifEmpty { o.optString("url") },
                    votes       = o.optInt("votes"),
                    clickcount  = o.optInt("clickcount"),
                    codec       = o.optString("codec"),
                    bitrate     = o.optInt("bitrate"),
                    country     = o.optString("country"),
                    tags        = o.optString("tags"),
                    favicon     = o.optString("favicon"),
                ))
            }
        }
        return deduplicate(raw)
    }

    private fun normalizeUrl(url: String): String =
        url.lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
            .substringBefore('?')

    fun deduplicate(stations: List<RadioBrowserStation>): List<RadioBrowserStation> =
        stations
            .filter { it.urlResolved.isNotEmpty() }
            .groupBy { normalizeUrl(it.urlResolved) }
            .values
            .mapNotNull { group ->
                val winner = group.maxByOrNull { it.score } ?: return@mapNotNull null
                val favicon = winner.favicon.ifEmpty {
                    group.firstOrNull { it.favicon.isNotEmpty() }?.favicon ?: ""
                }
                winner.copy(favicon = favicon)
            }
            .sortedByDescending { it.score }
}
