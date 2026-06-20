package com.shapeshed.aerial.data

import android.util.Log
import com.shapeshed.aerial.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder

class RadioBrowserServerException(val code: Int) : Exception("HTTP $code")

object RadioBrowserApi {

    private const val TAG = "RadioBrowserApi"
    private const val DISCOVERY_HOST = "all.api.radio-browser.info"
    private const val CACHE_TTL_MS = 30 * 60 * 1000L
    private const val FALLBACK_TTL_MS = 60 * 1000L  // short TTL when falling back to round-robin
    private const val RESPONSE_SIZE_LIMIT = 1 * 1024 * 1024  // 1 MB

    private val cacheMutex = Mutex()
    private var cachedServers: List<String> = emptyList()
    private var cacheExpiry: Long = 0L

    // Soft optimisation: deprioritise servers that failed this session.
    // Uses copy-on-write semantics; benign races are acceptable here.
    @Volatile private var recentlyFailed: Set<String> = emptySet()

    suspend fun discoverServers(): List<String> {
        // Fast path: return cached list if still fresh.
        cacheMutex.withLock {
            val now = System.currentTimeMillis()
            if (cachedServers.isNotEmpty() && now < cacheExpiry) return cachedServers
        }

        // Slow path: discover without holding the mutex so other callers are not blocked
        // during I/O. Cap total discovery time to 5 s.
        val startedAt = System.currentTimeMillis()
        val discovered = withContext(Dispatchers.IO) {
            withTimeoutOrNull(5_000) {
                runCatching { fetchServerListFromApi() }.getOrNull()?.takeIf { it.isNotEmpty() }
                    ?: runCatching { discoverViaDns() }.getOrNull()?.takeIf { it.isNotEmpty() }
            }
        }

        // If both discovery methods fail, fall back to the round-robin host itself.
        // It distributes requests across whatever servers are currently alive,
        // so combined with retry logic it gives us the best chance of success.
        val (servers, ttl) = if (!discovered.isNullOrEmpty()) {
            discovered.shuffled() to CACHE_TTL_MS
        } else {
            Log.w(TAG, "Server discovery failed; falling back to round-robin host")
            listOf(DISCOVERY_HOST) to FALLBACK_TTL_MS
        }

        return cacheMutex.withLock {
            recentlyFailed = emptySet()
            cachedServers = servers
            cacheExpiry = startedAt + ttl
            servers
        }
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
        if (query.isBlank()) return emptyList()
        return withServerFailover { server -> search(server, query) }
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
        // Capture failed set once so the sort is stable for this attempt.
        val failed = recentlyFailed
        val servers = discoverServers().sortedBy { if (it in failed) 1 else 0 }
        var lastError: Exception? = null
        for (server in servers) {
            // Retry transient 5xx errors on the same server before moving on.
            // nl1/at1 are currently unreachable so de1 is the only live server;
            // its 503s are intermittent and succeed on a quick retry.
            for (attempt in 1..3) {
                try {
                    return block(server)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: RadioBrowserServerException) {
                    if (e.code in 500..599 && attempt < 3) {
                        Log.w(TAG, "HTTP ${e.code} from $server, retry $attempt/3")
                        delay(500L * attempt)
                    } else {
                        Log.w(TAG, "Radio Browser request failed for $server: HTTP ${e.code}")
                        recentlyFailed = recentlyFailed + server
                        lastError = e
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Radio Browser request failed for $server", e)
                    recentlyFailed = recentlyFailed + server
                    lastError = e
                    break
                }
            }
        }
        // All servers exhausted — expire the cache so the next call re-discovers fresh.
        cacheMutex.withLock { cacheExpiry = 0L }
        throw lastError ?: IOException("No Radio Browser servers available")
    }

    private fun request(url: String, timeoutMillis: Int): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Aerial/${BuildConfig.VERSION_NAME} (Android)")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = timeoutMillis
        conn.readTimeout = timeoutMillis
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw RadioBrowserServerException(code)
            return conn.inputStream.bufferedReader().use { reader ->
                val sb = StringBuilder()
                val buf = CharArray(8192)
                var total = 0
                while (true) {
                    val n = reader.read(buf)
                    if (n == -1) break
                    total += n
                    if (total > RESPONSE_SIZE_LIMIT) throw IOException("Response too large (>1 MB)")
                    sb.append(buf, 0, n)
                }
                sb.toString()
            }
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
