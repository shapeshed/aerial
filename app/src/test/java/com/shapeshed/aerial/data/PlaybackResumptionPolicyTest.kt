package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackResumptionPolicyTest {
    @Test
    fun restoresPersistedQueueWhenItContainsNeighbours() {
        val saved = listOf(station(2, "Second"), station(1, "First"))

        assertEquals(saved, queueForResumption(saved, listOf(station(1, "First")), FavoritesSort.AZ))
    }

    @Test
    fun rebuildsLegacySnapshotUsingTheSavedSort() {
        val fallback = listOf(station(2, "Second"), station(1, "First"))

        assertEquals(listOf(1L, 2L), queueForResumption(emptyList(), fallback, FavoritesSort.AZ).map(Station::id))
    }

    private fun station(id: Long, name: String) = Station(
        id = id,
        name = name,
        streamUrl = "https://stream.example/$id",
    )
}
