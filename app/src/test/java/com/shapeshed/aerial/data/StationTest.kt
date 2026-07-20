package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationTest {

    @Test
    fun matchesById() {
        val a = station(id = 1L, streamUrl = "https://a")
        val b = station(id = 1L, streamUrl = "https://b")

        assertTrue(a.matches(b))
    }

    @Test
    fun matchesByStreamUrlWhenEitherIdIsEphemeral() {
        val saved = station(id = 5L, streamUrl = "https://same")
        val ephemeral = station(id = 0L, streamUrl = "https://same")

        assertTrue(saved.matches(ephemeral))
        assertTrue(ephemeral.matches(saved))
    }

    @Test
    fun matchesByProviderAndProviderIdForEphemeralStations() {
        val a = station(id = 0L, streamUrl = "https://a", provider = "bauer", providerId = "42")
        val b = station(id = 0L, streamUrl = "https://b", provider = "bauer", providerId = "42")

        assertTrue(a.matches(b))
    }

    @Test
    fun doesNotMatchDifferentStations() {
        val a = station(id = 0L, streamUrl = "https://a")
        val b = station(id = 0L, streamUrl = "https://b")

        assertFalse(a.matches(b))
    }

    @Test
    fun resolveQueueStartFindsIndexInMultiStationQueue() {
        val queue = listOf(station(id = 1L), station(id = 2L), station(id = 3L))

        assertEquals(1, resolveQueueStart(queue, station(id = 2L)))
    }

    @Test
    fun resolveQueueStartReturnsNullForSingleStationQueue() {
        val queue = listOf(station(id = 1L))

        assertNull(resolveQueueStart(queue, station(id = 1L)))
    }

    @Test
    fun resolveQueueStartReturnsNullWhenTargetNotInQueue() {
        val queue = listOf(station(id = 1L), station(id = 2L))

        assertNull(resolveQueueStart(queue, station(id = 99L)))
    }

    private fun station(
        id: Long = 0L,
        streamUrl: String = "https://stream.example/$id",
        provider: String = "",
        providerId: String = "",
    ) = Station(
        id = id,
        name = "Station $id",
        streamUrl = streamUrl,
        provider = provider,
        providerId = providerId,
    )
}
