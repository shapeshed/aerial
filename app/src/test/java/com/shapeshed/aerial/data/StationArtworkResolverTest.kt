package com.shapeshed.aerial.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class StationArtworkResolverTest {
    private val registryRepository = mock<RegistryRepository>()
    private val resolver = StationArtworkResolver(registryRepository)

    @Test
    fun prefersProviderIdLookupAndReplacesLogoPath() = runBlocking {
        val station = station(provider = "bbc", providerId = "bbc_world_service")
        whenever(registryRepository.getByProviderId("bbc", "bbc_world_service"))
            .thenReturn(registry(logoUrl = "https://cdn.example/bbc.png"))

        val result = resolver.recover(station)

        assertEquals("https://cdn.example/bbc.png", result.logoPath)
    }

    @Test
    fun fallsBackToStreamUrlWhenProviderIdIsBlank() = runBlocking {
        val station = station(provider = "bbc", providerId = "")
        whenever(registryRepository.getByStreamUrl(station.streamUrl))
            .thenReturn(registry(logoUrl = "https://cdn.example/by-url.png"))

        val result = resolver.recover(station)

        assertEquals("https://cdn.example/by-url.png", result.logoPath)
    }

    @Test
    fun leavesStationUntouchedWhenRegistryHasNoMatch() = runBlocking {
        val station = station(logoPath = "file:///cache/logo.png")
        whenever(registryRepository.getByStreamUrl(station.streamUrl)).thenReturn(null)

        val result = resolver.recover(station)

        assertSame(station, result)
    }

    @Test
    fun leavesStationUntouchedWhenRegistryLogoIsBlank() = runBlocking {
        val station = station(logoPath = "")
        whenever(registryRepository.getByStreamUrl(station.streamUrl))
            .thenReturn(registry(logoUrl = "   "))

        val result = resolver.recover(station)

        assertSame(station, result)
    }

    private fun station(provider: String = "", providerId: String = "", logoPath: String = ""): Station = Station(
        name = "Station",
        streamUrl = "https://stream.example/live",
        logoPath = logoPath,
        provider = provider,
        providerId = providerId,
    )

    private fun registry(logoUrl: String): RegistryStation = RegistryStation(
        name = "Station",
        streamUrl = "https://stream.example/live",
        logoUrl = logoUrl,
    )
}
