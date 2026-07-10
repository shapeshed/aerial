package com.shapeshed.aerial.data

import android.util.LruCache
import com.shapeshed.aerial.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

internal val AERIAL_USER_AGENT = "Aerial/${BuildConfig.VERSION_NAME} (Android)"

/**
 * GET [url] and return the response body as a string, or null on any error or non-2xx status.
 * Caller is responsible for running this on a background thread (e.g. Dispatchers.IO).
 */
internal fun httpGetJson(url: String, extraHeaders: Map<String, String> = emptyMap()): String? {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", AERIAL_USER_AGENT)
        extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
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

/**
 * POST [body] (JSON) to [url] and return the response body, or null on any error or non-2xx status.
 * Caller is responsible for running this on a background thread (e.g. Dispatchers.IO).
 */
internal fun httpPostJson(url: String, body: String, extraHeaders: Map<String, String> = emptyMap()): String? {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("User-Agent", AERIAL_USER_AGENT)
        extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Fetch image bytes from [url], storing results in [cache]. Returns null on any error.
 * Caller is responsible for running this on a background thread (e.g. Dispatchers.IO).
 */
internal fun httpFetchBytes(url: String, cache: LruCache<String, ByteArray>): ByteArray? {
    cache[url]?.let { return it }
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", AERIAL_USER_AGENT)
        try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { input ->
                input.readBytes().also { bytes -> cache.put(url, bytes) }
            }
        } finally {
            conn.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}
