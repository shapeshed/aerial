package com.shapeshed.aerial.data

import android.util.Log
import com.shapeshed.aerial.BuildConfig
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
    private const val CACHE_TTL_MS = 30 * 60 * 1000L

    @Volatile private var cachedServers: List<String> = emptyList()
    @Volatile private var cacheExpiry: Long = 0L

    suspend fun discoverServers(): List<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedServers.isNotEmpty() && now < cacheExpiry) return@withContext cachedServers

        val servers = runCatching { fetchServerListFromApi() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: runCatching { discoverViaDns() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: listOf(DISCOVERY_HOST)

        val result = servers.shuffled()
        cachedServers = result
        cacheExpiry = now + CACHE_TTL_MS
        result
    }

    // Primary: /json/servers endpoint — avoids reverse DNS which is unreliable on Android.
    private fun fetchServerListFromApi(): List<String> {
        val json = request("https://$DISCOVERY_HOST/json/servers", timeoutMillis = 5_000)
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val name = array.getJSONObject(i).optString("name")
                if (name.isNotEmpty() && name != DISCOVERY_HOST) add(name)
            }
        }.distinct()
    }

    // Fallback: DNS A record + reverse hostname lookup.
    private fun discoverViaDns(): List<String> {
        val addresses = InetAddress.getAllByName(DISCOVERY_HOST)
        return addresses.mapNotNull { addr ->
            val hostname = addr.canonicalHostName
            if (hostname != addr.hostAddress && hostname.contains('.')) hostname else null
        }
            .filter { it != DISCOVERY_HOST }
            .distinct()
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
        conn.setRequestProperty("User-Agent", "Aerial/${BuildConfig.VERSION_NAME} (Android)")
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

    private val TECH_SUFFIX_PATTERNS = listOf(
        // [MP3], [AAC], [128k] etc
        Regex("""\s*\[[^\]]*\]\s*$"""),
        // (MP3), (AAC), (128k), (HQ), (Medium Bitrate) etc
        Regex("""\s*\(\s*(mp3|aac\+?|flac|opus|ogg|hq|lq|hd|sd|\d+\s*k(?:bps?)?|high\s+quality|medium\s+bitrate|low\s+bitrate)\s*\)\s*$""", RegexOption.IGNORE_CASE),
        // Pipe-separated technical suffixes: " | DLF | MP3 128k"
        Regex("""\s*\|.*(mp3|aac\+?|flac|opus|ogg|\d+\s*k(?:bps?)?).*$""", RegexOption.IGNORE_CASE),
        // Dash-prefixed format: " - MP3", " - AAC HD 256k"
        Regex("""\s+-\s+(mp3|aac\+?|flac|opus|ogg).*$""", RegexOption.IGNORE_CASE),
        // Bare format + optional quality + optional bitrate: "AAC HD 256k", "MP3 128k"
        Regex("""\s+(mp3|aac\+?|flac|opus|ogg)(?:\s+(?:hd|hq))?(?:\s+\d+\s*k(?:bps?)?)?\s*$""", RegexOption.IGNORE_CASE),
        // Bare trailing quality word: "90s90s Dance HQ"
        Regex("""\s+(?:hq|hd)\s*$""", RegexOption.IGNORE_CASE),
        // Bare trailing bitrate: "128k", "256kbps", "320kb"
        Regex("""\s+\d+\s*k(?:b(?:ps?)?)?\s*$""", RegexOption.IGNORE_CASE),
    )

    private fun cleanStationName(name: String): String {
        var result = name.trim()
        repeat(3) {
            val before = result
            for (pattern in TECH_SUFFIX_PATTERNS) {
                result = result.replace(pattern, "")
            }
            result = result.replace(Regex("""\s{2,}"""), " ").trim()
            if (result == before) return result
        }
        return result
    }

    internal fun parseSearchResponse(json: String): List<RadioBrowserStation> {
        val array = JSONArray(json)
        val raw = buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(RadioBrowserStation(
                    stationuuid = o.optString("stationuuid"),
                    name        = cleanStationName(o.optString("name")),
                    urlResolved = o.optString("url_resolved").ifEmpty { o.optString("url") },
                    votes       = o.optInt("votes"),
                    clickcount  = o.optInt("clickcount"),
                    codec       = o.optString("codec"),
                    bitrate     = o.optInt("bitrate"),
                    country     = o.optString("country"),
                    countrycode = o.optString("countrycode"),
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
