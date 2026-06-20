package com.shapeshed.aerial.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException

class RadioBrowserServerException(val code: Int) : Exception("HTTP $code")

object RadioBrowserApi {

    private const val TAG = "RadioBrowserApi"
    private const val DISCOVERY_HOST = "all.api.radio-browser.info"

    suspend fun discoverServers(): List<String> = withContext(Dispatchers.IO) {
        val addresses = InetAddress.getAllByName(DISCOVERY_HOST)
        val servers = addresses.mapNotNull { addr ->
            val hostname = addr.canonicalHostName
            // Filter out bare IP addresses because HTTPS requests need a hostname for TLS.
            if (hostname != addr.hostAddress && hostname.contains('.')) hostname else null
        }
            .filter { it != DISCOVERY_HOST }
            .distinct()
            .shuffled()

        servers.ifEmpty {
            // Android devices may not return reverse DNS names. The discovery host
            // is still a live DNS target, unlike a hardcoded regional server.
            listOf(DISCOVERY_HOST)
        }
    }

    suspend fun search(query: String): List<RadioBrowserStation> {
        return withServerFailover { server ->
            search(server, query)
        }
    }

    suspend fun registerClick(uuid: String) {
        runCatching {
            withServerFailover { server ->
                request("https://$server/json/url/$uuid", timeoutMillis = 5_000)
            }
        }
    }

    suspend fun vote(uuid: String) {
        runCatching {
            withServerFailover { server ->
                request("https://$server/json/vote/$uuid", timeoutMillis = 5_000)
            }
        }
    }

    private suspend fun search(server: String, query: String): List<RadioBrowserStation> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://$server/json/stations/search" +
            "?name=$encoded&limit=200&order=votes&reverse=true&hidebroken=true"
        parseSearchResponse(request(url, timeoutMillis = 10_000))
    }

    private suspend fun <T> withServerFailover(block: suspend (String) -> T): T {
        val servers = discoverServers()
        var lastError: Exception? = null
        for (server in servers) {
            try {
                return block(server)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Radio Browser request failed for $server", e)
                lastError = e
            }
        }
        throw lastError ?: IOException("No Radio Browser servers available")
    }

    private fun request(url: String, timeoutMillis: Int): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Aerial/0.1.1 (Android)")
        conn.connectTimeout = timeoutMillis
        conn.readTimeout = timeoutMillis
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw RadioBrowserServerException(code)
            return conn.inputStream.bufferedReader().readText()
        } finally {
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
