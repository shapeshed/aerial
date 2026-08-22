package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StationQueueTest {

    @Test
    fun snapshotRoundTripPreservesCurrentStationAndQueueOrder() {
        val left = station(1, "Left")
        val middle = station(2, "Middle")
        val right = station(3, "Right")

        val restored = lastPlayedStationSnapshot(
            middle.toLastPlayedJson(listOf(right, left, middle)).toString(),
        )

        assertEquals(middle, restored.station)
        assertEquals(listOf(right, left, middle), restored.queue)
        assertEquals(2, resolveQueueStart(restored.queue, restored.station))
    }

    @Test
    fun legacySnapshotWithoutQueueStillRestoresStation() {
        val restored = lastPlayedStationSnapshot(station(7, "Legacy").toLastPlayedJson().toString())

        assertEquals(7L, restored.station.id)
        assertTrue(restored.queue.isEmpty())
    }

    @Test
    fun persistedQueueDoesNotChangeWhenLastPlayedSortChanges() {
        val left = station(1, "Left", lastPlayedAt = 1_000)
        val right = station(2, "Right", lastPlayedAt = 2_000)
        val activeQueue = sortStations(listOf(left, right), FavoritesSort.LAST_PLAYED)
        val snapshot = lastPlayedStationSnapshot(
            right.toLastPlayedJson(activeQueue).toString(),
        )

        val updated = listOf(
            left.copy(lastPlayedAt = 3_000),
            right.copy(lastPlayedAt = 2_000),
        )

        assertEquals(listOf(1L, 2L), sortStations(updated, FavoritesSort.LAST_PLAYED).map { it.id })
        assertEquals(listOf(2L, 1L), snapshot.queue.map { it.id })
    }

    private fun station(id: Long, name: String, lastPlayedAt: Long = 0) = Station(
        id = id,
        name = name,
        streamUrl = "https://stream.example/$id",
        isFavorite = true,
        lastPlayedAt = lastPlayedAt,
    )
}
