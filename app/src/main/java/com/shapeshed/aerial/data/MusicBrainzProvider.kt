package com.shapeshed.aerial.data

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "MusicBrainzProvider"

class MusicBrainzProvider : Provider {
    private var station: Station? = null
    private var scope: CoroutineScope? = null
    private var lookupJob: Job? = null
    private var lastRawTitle: String? = null

    override fun canEnrich(station: Station): Boolean = true

    override fun start(station: Station, scope: CoroutineScope) {
        this.station = station
        this.scope = scope
    }

    override fun pause() {
        lookupJob?.cancel()
        lookupJob = null
    }

    override fun stop() {
        lookupJob?.cancel()
        lookupJob = null
        station = null
        scope = null
        lastRawTitle = null
        NowPlayingStore.set(null)
    }

    override fun notifyTransition() {}

    override fun onIcyTitle(rawTitle: String) {
        if (rawTitle == lastRawTitle) return
        lastRawTitle = rawTitle
        val stationId = station?.id ?: return
        val (icyArtist, icyTrackTitle) = parseIcyTitle(rawTitle)

        if (icyArtist == null) {
            NowPlayingStore.set(null)
            return
        }

        NowPlayingStore.set(
            NowPlayingInfo(
                stationId = stationId,
                track = TrackInfo(artist = icyArtist, title = icyTrackTitle),
            )
        )

        lookupJob?.cancel()
        lookupJob = scope?.launch(Dispatchers.IO) {
            enrichWithMusicBrainz(stationId, icyArtist, icyTrackTitle)
        }
    }

    private fun enrichWithMusicBrainz(stationId: Long, artist: String, title: String) {
        val recording = searchMusicBrainzRecording(artist, title) ?: return
        val releaseId = recording.optJSONArray("releases")?.optJSONObject(0)?.optString("id")
            ?.takeIf { it.isNotBlank() }
        val artworkUrl = releaseId?.let { "https://coverartarchive.org/release/$it/front-250" }
        val artworkData = artworkUrl?.let { fetchMbBytes(it) }
        if (artworkData == null && artworkUrl == null) return
        Log.d(TAG, "artwork fetched for $artist – $title")
        NowPlayingStore.set(
            NowPlayingInfo(
                stationId = stationId,
                track = TrackInfo(
                    artist = artist,
                    title = title,
                    artworkUrl = if (artworkData == null) artworkUrl else null,
                    artworkData = artworkData,
                ),
            )
        )
    }

    private fun searchMusicBrainzRecording(artist: String, title: String): JSONObject? {
        val enc = "UTF-8"
        val query = "recording:%22${URLEncoder.encode(title, enc)}%22+artist:%22${URLEncoder.encode(artist, enc)}%22"
        val url = "https://musicbrainz.org/ws/2/recording/?query=$query&limit=1&fmt=json"
        val json = httpGetJson(url, extraHeaders = mapOf("Accept" to "application/json")) ?: return null
        return try {
            JSONObject(json).optJSONArray("recordings")?.optJSONObject(0)
        } catch (_: Exception) {
            null
        }
    }
}

private fun fetchMbBytes(url: String): ByteArray? {
    var currentUrl = url
    for (i in 0 until 5) {
        try {
            val conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", AERIAL_USER_AGENT)
            try {
                val code = conn.responseCode
                when {
                    code in 200..299 -> return conn.inputStream.use { it.readBytes() }
                    code in 300..399 -> currentUrl = conn.getHeaderField("Location") ?: return null
                    else -> return null
                }
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            return null
        }
    }
    return null
}
