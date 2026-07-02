package com.shapeshed.aerial.data

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val PULL_URL = "https://api.radiofrance.fr/livemeta/pull/%d"
private const val WEBRF_URL = "https://api.radiofrance.fr/livemeta/live/%d/webrf_fip_player"

private val artworkCache = object : LruCache<String, ByteArray>(4 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ByteArray) = value.size
}

internal fun fetchRadioFranceBytes(url: String): ByteArray? = httpFetchBytes(url, artworkCache)

internal data class RadioFranceNowPlaying(
    val title: String,
    val artist: String?,
    val description: String?,
    val isSong: Boolean,
    val programmeTitle: String?,
    val visualUrl: String?,
    val delayToRefreshMs: Long,
)

internal suspend fun fetchRadioFranceNowPlaying(stationId: Int): RadioFranceNowPlaying? =
    withContext(Dispatchers.IO) {
        val json = httpGetJson(PULL_URL.format(stationId)) ?: return@withContext null
        val result = parseRadioFrancePullResponse(json)
        if (result == null) {
            // Pull endpoint has no active steps (stale data) — fall back to webrf entirely.
            val webRFJson = httpGetJson(WEBRF_URL.format(stationId)) ?: return@withContext null
            return@withContext parseWebRFFull(webRFJson)
        }
        // Expression/blank steps have no visual in the pull response; fall back to the webrf
        // endpoint whose `cover` UUID resolves via pikapi.
        if (!result.isSong && result.visualUrl == null) {
            val webRFJson = httpGetJson(WEBRF_URL.format(stationId))
            val coverUrl = webRFJson?.let { parseWebRFCover(it) }
            if (coverUrl != null) return@withContext result.copy(visualUrl = coverUrl)
        }
        result
    }

private fun parseWebRFCover(json: String): String? {
    return try {
        val now = JSONObject(json).optJSONObject("now") ?: return null
        // Skip if a song is currently playing — pull already has song visuals.
        if (!now.isNull("songUuid")) return null
        val cover = now.optString("cover").trim().takeIf { it.isNotBlank() } ?: return null
        if (cover.isUuid()) "https://www.radiofrance.fr/pikapi/images/$cover/400x400" else null
    } catch (_: Exception) {
        null
    }
}

private fun parseWebRFFull(json: String): RadioFranceNowPlaying? {
    return try {
        val now = JSONObject(json).optJSONObject("now") ?: return null
        val isSong = !now.isNull("songUuid")
        val firstLine = now.optString("firstLine").trim().takeIf { it.isNotBlank() } ?: return null
        val secondLine = now.optString("secondLine").trim().takeIf { it.isNotBlank() }
        val cover = now.optString("cover").trim().takeIf { it.isNotBlank() }
        val visualUrl = cover?.let {
            if (it.isUuid()) "https://www.radiofrance.fr/pikapi/images/$it/400x400" else null
        }
        val endTime = now.optLong("endTime", 0L)
        val nowSec = System.currentTimeMillis() / 1000L
        val delayMs = if (endTime > nowSec) ((endTime - nowSec) * 1000L).coerceIn(5_000L, 30_000L) else 10_000L
        RadioFranceNowPlaying(
            title = if (isSong) secondLine ?: firstLine else firstLine,
            artist = if (isSong) firstLine else null,
            description = if (!isSong) secondLine else null,
            isSong = isSong,
            programmeTitle = null,
            visualUrl = visualUrl,
            delayToRefreshMs = delayMs,
        )
    } catch (_: Exception) {
        null
    }
}

private val UUID_REGEX = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
private fun String.isUuid() = UUID_REGEX.matches(this)

internal fun parseRadioFrancePullResponse(json: String): RadioFranceNowPlaying? {
    return try {
        val root = JSONObject(json)
        val steps = root.optJSONObject("steps") ?: return null
        val now = System.currentTimeMillis() / 1000L

        // Title/context: deepest active step wins — songs beat blank parents, named sub-programmes
        // (Music Queer, Le journal de 07h30) beat the top-level show. Same-depth ties broken by
        // longest duration so short transient segments don't override the main programme.
        // Artwork: tracked separately — deepest step that has a visual. A sub-segment with no
        // visual should not cause the webrf fallback to fire when a parent step has one.
        val stepById = mutableMapOf<String, JSONObject>()
        var current: JSONObject? = null
        var currentDepth = -1
        var currentDuration = -1L
        var visualStep: JSONObject? = null
        var visualDepth = -1
        val keys = steps.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val step = steps.optJSONObject(key) ?: continue
            val stepId = step.optString("stepId").takeIf { it.isNotBlank() } ?: key
            stepById[stepId] = step
            val start = step.optLong("start", 0L)
            val end = step.optLong("end", 0L)
            if (now in start..end) {
                val depth = step.optInt("depth", 0)
                val duration = end - start
                if (depth > currentDepth || (depth == currentDepth && duration > currentDuration)) {
                    current = step
                    currentDepth = depth
                    currentDuration = duration
                }
                if (step.optString("visual").isNotBlank() && depth > visualDepth) {
                    visualStep = step
                    visualDepth = depth
                }
            }
        }
        current ?: return null

        val episodeTitle = current.optString("title").trim().takeIf { it.isNotBlank() } ?: return null
        val isSong = current.optString("embedType").trim() == "song"
        val artist = if (isSong) {
            current.optJSONArray("highlightedArtists")?.optString(0)?.trim()?.takeIf { it.isNotBlank() }
                ?: current.optString("authors").trim().takeIf { it.isNotBlank() }
                ?: current.optString("performers").trim().takeIf { it.isNotBlank() }
        } else null
        // For shows: titleConcept is the programme name ("Le 5/7"), title is the episode title.
        // For songs: title is the track title, titleConcept is absent.
        val titleConcept = if (!isSong) current.optString("titleConcept").trim() else ""
        val title = titleConcept.takeIf { it.isNotBlank() } ?: episodeTitle
        // Only use the episode title as subtitle when it's distinct from the programme name.
        val description = episodeTitle.takeIf { !isSong && titleConcept.isNotBlank() && it != title }
        // For songs, look up the parent step to get the programme name (e.g. "La Playlist").
        val programmeTitle = if (isSong) {
            val fatherId = current.optString("fatherStepId").takeIf { it.isNotBlank() }
            fatherId?.let { stepById[it] }?.let { parent ->
                parent.optString("titleConcept").trim().takeIf { it.isNotBlank() }
                    ?: parent.optString("title").trim().takeIf { it.isNotBlank() }
            }
        } else null
        val rawVisual = (visualStep ?: current).optString("visual").trim()
        val visual = when {
            rawVisual.startsWith("https://") -> rawVisual
            rawVisual.isUuid() -> "https://www.radiofrance.fr/pikapi/images/$rawVisual/400x400"
            else -> null
        }

        val end = current.optLong("end", 0L)
        val delayMs = if (end > now) ((end - now) * 1000L).coerceIn(5_000L, 30_000L) else 10_000L

        RadioFranceNowPlaying(
            title = title,
            artist = artist?.takeIf { it.isNotBlank() },
            description = description,
            isSong = isSong,
            programmeTitle = programmeTitle,
            visualUrl = visual,
            delayToRefreshMs = delayMs,
        )
    } catch (_: Exception) {
        null
    }
}
