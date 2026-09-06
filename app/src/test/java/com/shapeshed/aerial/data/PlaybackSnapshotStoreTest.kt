package com.shapeshed.aerial.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSnapshotStoreTest {

    @Test
    fun writeAndReadPreservesStationAndQueue() = runBlocking {
        val current = station(2, "Current")
        val queue = listOf(station(1, "First"), current, station(3, "Last"))
        val store = PlaybackSnapshotStore(MemoryDataStore())

        store.write(current, queue)

        assertEquals(current, store.read()?.station)
        assertEquals(queue, store.read()?.queue)
    }

    @Test
    fun writeWithoutQueuePreservesExistingQueue() = runBlocking {
        val current = station(2, "Current")
        val queue = listOf(station(1, "First"), current)
        val store = PlaybackSnapshotStore(MemoryDataStore())
        store.write(current, queue)

        store.write(current.copy(name = "Renamed"))

        assertEquals(queue, store.read()?.queue)
        assertEquals("Renamed", store.read()?.station?.name)
    }

    @Test
    fun clearIfMatchingOnlyClearsTheMatchingStation() = runBlocking {
        val current = station(2, "Current")
        val store = PlaybackSnapshotStore(MemoryDataStore())
        store.write(current)

        assertFalse(store.clearIfMatching(station(9, "Other")))
        assertEquals(current, store.read()?.station)
        assertTrue(store.clearIfMatching(current.copy(name = "Updated")))
        assertEquals(null, store.read())
    }

    @Test
    fun favoriteSortDefaultsToAzAndReadsPersistedValue() = runBlocking {
        val dataStore = MemoryDataStore()
        val store = PlaybackSnapshotStore(dataStore)

        assertEquals(FavoritesSort.AZ, store.favoriteSort())
        dataStore.edit { it[FAVORITES_SORT_KEY] = FavoritesSort.LAST_PLAYED.name }

        assertEquals(FavoritesSort.LAST_PLAYED, store.favoriteSort())
    }

    private fun station(id: Long, name: String) = Station(
        id = id,
        name = name,
        streamUrl = "https://stream.example/$id",
    )

    private class MemoryDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
