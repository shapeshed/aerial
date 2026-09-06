package com.shapeshed.aerial.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

/** DataStore boundary for the station and queue used by playback restoration. */
class PlaybackSnapshotStore(private val dataStore: DataStore<Preferences>) {
    suspend fun read(): LastPlayedStationSnapshot? =
        dataStore.data.first()[LAST_PLAYED_STATION_KEY]?.let(::lastPlayedStationSnapshot)

    suspend fun write(station: Station, queue: List<Station> = emptyList()) {
        dataStore.edit { preferences ->
            val existingQueue = preferences[LAST_PLAYED_STATION_KEY]
                ?.let(::lastPlayedStationSnapshot)
                ?.queue
                .orEmpty()
            preferences[LAST_PLAYED_STATION_KEY] = station
                .toLastPlayedJson(queue.ifEmpty { existingQueue })
                .toString()
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(LAST_PLAYED_STATION_KEY) }
    }

    suspend fun clearIfMatching(station: Station): Boolean {
        val snapshot = read() ?: return false
        if (snapshot.station.id != station.id && snapshot.station.streamUrl != station.streamUrl) return false
        clear()
        return true
    }

    suspend fun favoriteSort(): FavoritesSort =
        dataStore.data.first()[FAVORITES_SORT_KEY]
            ?.let { saved -> FavoritesSort.entries.firstOrNull { it.name == saved } }
            ?: FavoritesSort.AZ
}
