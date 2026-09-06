package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQueuePlanTest {
    @Test
    fun recoversSavedStationAndKeepsItsQueueStartIndex() {
        val saved = station(7, "Saved", "https://saved")
        val requested = station(7, "Registry copy", "https://registry")
        val neighbour = station(8, "Neighbour", "https://neighbour")

        val plan = buildPlaybackQueuePlan(requested, listOf(requested, neighbour), listOf(saved, neighbour))

        assertEquals(saved, plan.station)
        assertEquals(listOf(saved, neighbour), plan.requestedQueue)
        assertEquals(listOf(saved, neighbour), plan.playerQueue)
        assertEquals(0, plan.startIndex)
    }

    @Test
    fun usesSingleStationPlayerQueueWhenNoNavigableQueueWasRequested() {
        val station = station(7, "Saved", "https://saved")

        val plan = buildPlaybackQueuePlan(station, emptyList(), listOf(station))

        assertEquals(listOf(station), plan.playerQueue)
        assertEquals(null, plan.startIndex)
    }

    private fun station(id: Long, name: String, url: String) = Station(id = id, name = name, streamUrl = url)
}
