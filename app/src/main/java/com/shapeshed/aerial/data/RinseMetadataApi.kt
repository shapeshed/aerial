package com.shapeshed.aerial.data

import android.util.LruCache
import org.json.JSONArray
import org.json.JSONObject

private const val SCHEDULE_URL = "https://rinse.fm/api/query/v1/schedule"
private const val IMAGE_URL_TEMPLATE = "https://image.rinse.fm/_/%s?w=800&h=800"

private val artworkCache = object : LruCache<String, ByteArray>(4 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ByteArray) = value.size
}

internal fun fetchRinseBytes(url: String): ByteArray? = httpFetchBytes(url, artworkCache)

internal fun isRinseStation(station: Station): Boolean = station.provider == "rinse"

internal data class RinseNowPlaying(
    val showTitle: String,
    val episodeSubtitle: String?,
    val artworkUrl: String?,
)

/**
 * Fetches Rinse's shared schedule feed and returns the currently-airing episode for
 * [channelSlug] (one of "uk", "france", "kool", "swu" — matches the aerial-registry
 * provider_id for Rinse stations). The feed is not filterable server-side by channel
 * despite accepting a `channel` query param, so this always fetches everything and
 * filters client-side.
 */
internal suspend fun fetchRinseNowPlaying(channelSlug: String): RinseNowPlaying? {
    val json = httpGetJson(SCHEDULE_URL) ?: return null
    return parseRinseSchedule(json, channelSlug)
}

internal fun parseRinseSchedule(json: String, channelSlug: String): RinseNowPlaying? {
    return try {
        val episodes = JSONObject(json).optJSONArray("episodes") ?: return null
        val nowMs = System.currentTimeMillis()
        var best: JSONObject? = null
        var bestStart = Long.MIN_VALUE
        for (i in 0 until episodes.length()) {
            val episode = episodes.optJSONObject(i) ?: continue
            val channel = episode.optJSONArray("channel")?.firstSlug() ?: continue
            if (channel != channelSlug) continue
            val startMs = trueEpisodeStartMillis(episode) ?: continue
            val lengthMinutes = episode.optInt("episodeLength", 60)
            val endMs = startMs + lengthMinutes * 60_000L
            if (nowMs in startMs until endMs && startMs > bestStart) {
                best = episode
                bestStart = startMs
            }
        }
        best?.let { buildNowPlaying(it) }
    } catch (_: Exception) {
        null
    }
}

private val TITLE_DATE_REGEX = Regex("""(\d{2})/(\d{2})/(\d{4})""")

/**
 * Rinse's `episodeTime` field always carries TODAY's date with the slot's time-of-day —
 * even for episodes actually scheduled on a different day — so distinct episodes airing
 * on different days at the same hour collide on an identical `episodeTime` and can't be
 * told apart (e.g. "Fever AM - 01/07/2026 - 22:00" and "Pariah - 02/07/2026 - 22:00" both
 * report episodeTime "...T22:00:00+01:00", even though Pariah is scheduled for the next
 * day). The episode's `title` carries its real date (DD/MM/YYYY) — the `slug` looked like
 * a cleaner source but can be stale for reruns (e.g. a slug dated weeks ago for an episode
 * whose title/episodeTime correctly show it rebroadcasting today) — so splice title's date
 * onto `episodeTime`'s time-of-day + offset to get the true absolute start.
 */
private fun trueEpisodeStartMillis(episode: JSONObject): Long? {
    val episodeTime = episode.optString("episodeTime")
    val tIndex = episodeTime.indexOf('T')
    if (tIndex == -1) return null
    val match = TITLE_DATE_REGEX.find(episode.optString("title")) ?: return parseIso8601Millis(episodeTime)
    val (day, month, year) = match.destructured
    return parseIso8601Millis("$year-$month-$day${episodeTime.substring(tIndex)}")
}

private fun buildNowPlaying(episode: JSONObject): RinseNowPlaying {
    val parentShow = episode.optJSONArray("parentShow")?.optJSONObject(0)
    val showTitle = episode.optNonBlankString("artistTitle")
        ?: parentShow?.optNonBlankString("title")
        ?: episode.optNonBlankString("title")
        ?: "Rinse FM"
    val episodeSubtitle = episode.optNonBlankString("displayTitle")

    // Prefer the episode's own image; Rinse doesn't always set one, so fall back to the
    // parent show's image (a stable artist/show photo).
    val filename = episode.optJSONArray("featuredImage")?.optJSONObject(0)?.optNonBlankString("filename")
        ?: parentShow?.optJSONArray("featuredImage")?.optJSONObject(0)?.optNonBlankString("filename")
    val artworkUrl = filename?.let { IMAGE_URL_TEMPLATE.format(it) }

    return RinseNowPlaying(
        showTitle = showTitle,
        episodeSubtitle = episodeSubtitle,
        artworkUrl = artworkUrl,
    )
}

private fun JSONArray.firstSlug(): String? = optJSONObject(0)?.optString("slug")?.takeIf { it.isNotBlank() }

/**
 * org.json's optString returns the literal string "null" (not blank/empty) for a key whose
 * value is explicit JSON null, since JSONObject.NULL is a non-null sentinel object. Rinse's
 * schedule feed sets fields like displayTitle to JSON null when unset, so a plain
 * optString().takeIf { isNotBlank() } lets "null" leak through as real content.
 */
private fun JSONObject.optNonBlankString(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf { it.isNotBlank() }

/** Parses an ISO-8601 timestamp with a numeric zone offset (e.g. "2026-07-01T21:00:00+01:00"). */
private fun parseIso8601Millis(value: String): Long? {
    if (value.isBlank()) return null
    return try {
        java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }
}
