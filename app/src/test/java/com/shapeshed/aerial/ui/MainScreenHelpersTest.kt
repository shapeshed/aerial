package com.shapeshed.aerial.ui

import androidx.compose.ui.graphics.vector.ImageVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure helpers shared by the main screen and the now-playing pager.
 */
class MainScreenHelpersTest {
    @Test
    fun countryNameResolvesKnownRegionCodes() {
        assertEquals("United Kingdom", countryName("GB", java.util.Locale.forLanguageTag("en")))
    }

    @Test
    fun savedKeyRequiresBothProviderFields() {
        val station = station(provider = "bbc", providerId = "bbc_world_service")

        assertEquals(RegistryStationKey("bbc", "bbc_world_service"), station.savedKey())
        assertNull(station(provider = "bbc", providerId = "").savedKey())
        assertNull(station(provider = "", providerId = "bbc_world_service").savedKey())
        assertNull(station(provider = " ", providerId = " ").savedKey())
    }

    @Test
    fun circularPageIndexWrapsInBothDirections() {
        assertEquals(0, circularPageIndex(0, 3))
        assertEquals(2, circularPageIndex(2, 3))
        assertEquals(0, circularPageIndex(3, 3))
        assertEquals(2, circularPageIndex(-1, 3))
        assertEquals(1, circularPageIndex(-5, 3))
        assertEquals(0, circularPageIndex(-3, 3))
    }

    private fun station(provider: String, providerId: String) = com.shapeshed.aerial.data.Station(
        name = "Station",
        streamUrl = "https://stream.example/live",
        provider = provider,
        providerId = providerId,
    )
}
