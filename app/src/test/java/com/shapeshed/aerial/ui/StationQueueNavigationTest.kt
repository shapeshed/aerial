package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationQueueNavigationTest {
    private val queue = listOf(
        Station(id = 1, name = "One", streamUrl = "one"),
        Station(id = 2, name = "Two", streamUrl = "two"),
        Station(id = 3, name = "Three", streamUrl = "three"),
    )

    @Test
    fun neighboursWrapAroundTheQueue() {
        assertEquals(1L, queue.stationAtOffset(queue[1], -1)?.id)
        assertEquals(3L, queue.stationAtOffset(queue[1], 1)?.id)
        assertEquals(3L, queue.stationAtOffset(queue.first(), -1)?.id)
        assertEquals(1L, queue.stationAtOffset(queue.last(), 1)?.id)
    }

    @Test
    fun navigationIsUnavailableForMissingOrSingleStationQueues() {
        assertNull(queue.stationAtOffset(Station(id = 9, name = "Missing", streamUrl = "missing"), 1))
        assertNull(queue.take(1).stationAtOffset(queue.first(), 1))
    }
}
