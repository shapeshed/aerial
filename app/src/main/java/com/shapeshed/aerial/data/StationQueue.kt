package com.shapeshed.aerial.data

import androidx.datastore.preferences.core.stringPreferencesKey
import org.json.JSONObject

// Shared between MainViewModel (UI-driven restore) and PlayerService (system-driven
// onPlaybackResumption) so both rebuild the same favourites queue from the same persisted state.

val LAST_PLAYED_STATION_KEY = stringPreferencesKey("last_played_station")
val FAVORITES_SORT_KEY = stringPreferencesKey("favorites_sort")

enum class FavoritesSort {
    AZ,
    LAST_PLAYED,
    MOST_PLAYED,
}

data class LastPlayedStationSnapshot(
    val station: Station,
    val queue: List<Station> = emptyList(),
)

fun Station.toLastPlayedJson(queue: List<Station> = emptyList()): JSONObject =
    stationJson(this)
        .put("queue", org.json.JSONArray().apply {
            queue.forEach { put(stationJson(it)) }
        })

private fun stationJson(station: Station): JSONObject =
    JSONObject()
        .put("id", station.id)
        .put("name", station.name)
        .put("streamUrl", station.streamUrl)
        .put("logoPath", station.logoPath)
        .put("isFavorite", station.isFavorite)
        .put("provider", station.provider)
        .put("providerId", station.providerId)
        .put("tags", station.tags)
        .put("description", station.description)
        .put("country", station.country)
        .put("countryCode", station.countryCode)

fun lastPlayedStationSnapshot(json: String): LastPlayedStationSnapshot {
    val obj = JSONObject(json)
    return LastPlayedStationSnapshot(
        station = obj.toStation(),
        queue = obj.optJSONArray("queue")?.let { array ->
            (0 until array.length()).map { index -> array.getJSONObject(index).toStation() }
        }.orEmpty(),
    )
}

private fun JSONObject.toStation(): Station =
    Station(
        id = optLong("id"),
        name = optString("name"),
        streamUrl = optString("streamUrl"),
        logoPath = optString("logoPath"),
        isFavorite = optBoolean("isFavorite"),
        provider = optString("provider"),
        providerId = optString("providerId"),
        tags = optString("tags"),
        description = optString("description"),
        country = optString("country"),
        countryCode = optString("countryCode"),
    )

fun sortStations(stations: List<Station>, sort: FavoritesSort): List<Station> = when (sort) {
    FavoritesSort.AZ -> stations.sortedWith(compareBy { stationSortKey(it.name) })
    FavoritesSort.LAST_PLAYED -> stations.sortedWith(
        compareByDescending<Station> { it.lastPlayedAt }.thenBy { stationSortKey(it.name) },
    )
    FavoritesSort.MOST_PLAYED -> stations.sortedWith(
        compareByDescending<Station> { it.playCount }.thenBy { stationSortKey(it.name) },
    )
}

// Maps English number words and digit strings to zero-padded numbers so that
// "BBC Radio One", "BBC Radio Two" … sort in numeric order rather than alphabetically.
private val NUMBER_WORDS = mapOf(
    "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
    "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
    "ten" to 10, "eleven" to 11, "twelve" to 12,
)

private fun stationSortKey(name: String): String =
    name.split(Regex("\\s+")).joinToString(" ") { token ->
        NUMBER_WORDS[token.lowercase()]?.let { "%03d".format(it) }
            ?: token.toIntOrNull()?.let { "%03d".format(it) }
            ?: token.lowercase()
    }
