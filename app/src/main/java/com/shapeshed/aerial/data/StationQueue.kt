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
)

fun Station.toLastPlayedJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("name", name)
        .put("streamUrl", streamUrl)
        .put("logoPath", logoPath)
        .put("isFavorite", isFavorite)
        .put("provider", provider)
        .put("providerId", providerId)
        .put("tags", tags)
        .put("description", description)
        .put("country", country)
        .put("countryCode", countryCode)

fun lastPlayedStationSnapshot(json: String): LastPlayedStationSnapshot {
    val obj = JSONObject(json)
    return LastPlayedStationSnapshot(
        station = Station(
            id = obj.optLong("id"),
            name = obj.optString("name"),
            streamUrl = obj.optString("streamUrl"),
            logoPath = obj.optString("logoPath"),
            isFavorite = obj.optBoolean("isFavorite"),
            provider = obj.optString("provider"),
            providerId = obj.optString("providerId"),
            tags = obj.optString("tags"),
            description = obj.optString("description"),
            country = obj.optString("country"),
            countryCode = obj.optString("countryCode"),
        ),
    )
}

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
