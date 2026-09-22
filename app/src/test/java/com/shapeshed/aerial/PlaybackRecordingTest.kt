package com.shapeshed.aerial

import com.shapeshed.aerial.data.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaybackRecordingTest {
    @Test
    fun identityIsStableAcrossSavingAndFavouriting() {
        val station = station(provider = "radio-paradise", providerId = "main")

        assertEquals(
            stationPlaybackKey(station),
            stationPlaybackKey(station.copy(id = 42L, isFavorite = true)),
        )
    }

    @Test
    fun unsavedStationsThatShareTheMediaIdStayDistinct() {
        val first = station(provider = "radio-paradise", providerId = "first")
        val second = station(provider = "radio-paradise", providerId = "second")

        assertNotEquals(stationPlaybackKey(first), stationPlaybackKey(second))
    }

    private fun station(provider: String, providerId: String) = Station(
        name = "$provider $providerId",
        streamUrl = "https://stream.example/$providerId",
        provider = provider,
        providerId = providerId,
    )
}
