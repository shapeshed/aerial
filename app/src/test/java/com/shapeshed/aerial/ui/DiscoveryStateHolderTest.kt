package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.PlayHistoryEntry
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryStateHolderTest {
    @Test
    fun registryContentLoadsOnceTheRegistryHasStations() = runTest {
        val registryRepository = mock<RegistryRepository>()
        whenever(registryRepository.countAsFlow()).thenReturn(MutableStateFlow(120))
        whenever(registryRepository.featuredStations()).thenReturn(listOf(station("Featured")))
        whenever(registryRepository.defaultStations()).thenReturn(listOf(station("Default")))
        whenever(registryRepository.curatedMoodStations()).thenReturn(mapOf("chill" to listOf(station("Chill"))))
        whenever(registryRepository.availableCountryCodes()).thenReturn(listOf("GB", "FR"))
        whenever(registryRepository.availableTags()).thenReturn(listOf("Rock", "Jazz"))
        val holder = holder(registryRepository = registryRepository)

        holder.start()
        advanceUntilIdle()

        assertEquals(listOf("Featured"), holder.featuredStations.value.map { it.name })
        assertEquals(listOf("Default"), holder.defaultStations.value.map { it.name })
        assertEquals(listOf("Chill"), holder.curatedMoodStations.value["chill"]?.map { it.name })
        assertEquals(listOf("GB", "FR"), holder.availableCountries.value)
        assertEquals(listOf("Rock", "Jazz"), holder.allTags.value)
        scope.cancel()
    }

    @Test
    fun anEmptyRegistryLoadsNothingRatherThanQuerying() = runTest {
        // The bundled registry asset may not be readable yet. countAsFlow reporting zero is the
        // signal not to query, so the fields must stay empty rather than throw.
        val registryRepository = mock<RegistryRepository>()
        whenever(registryRepository.countAsFlow()).thenReturn(MutableStateFlow(0))
        val holder = holder(registryRepository = registryRepository)

        holder.start()
        advanceUntilIdle()

        assertTrue(holder.featuredStations.value.isEmpty())
        assertTrue(holder.defaultStations.value.isEmpty())
        assertTrue(holder.allTags.value.isEmpty())
        assertTrue(holder.curatedMoodStations.value.isEmpty())
        assertTrue(holder.availableCountries.value.isEmpty())
        scope.cancel()
    }

    @Test
    fun forYouFollowsTheRequestedCountry() = runTest {
        val requested = mutableListOf<String>()
        val registryRepository = mock<RegistryRepository>()
        whenever(registryRepository.countAsFlow()).thenReturn(MutableStateFlow(10))
        whenever(registryRepository.forYouStations(any())).thenAnswer { invocation ->
            val country = invocation.getArgument<String>(0)
            requested += country
            listOf(station("ForYou-$country"))
        }
        val holder = holder(registryRepository = registryRepository)

        holder.start()
        advanceUntilIdle()
        assertEquals(listOf("ForYou-GB"), holder.forYouStations.value.map { it.name })

        holder.setForYouCountry("FR")
        advanceUntilIdle()

        assertEquals(listOf("ForYou-FR"), holder.forYouStations.value.map { it.name })
        // "GB" then "FR", and no third query: a blank country must not re-trigger the load.
        assertEquals(listOf("GB", "FR"), requested)
        scope.cancel()
    }

    @Test
    fun aBlankForYouCountryIsIgnored() = runTest {
        val requested = mutableListOf<String>()
        val registryRepository = mock<RegistryRepository>()
        whenever(registryRepository.countAsFlow()).thenReturn(MutableStateFlow(10))
        whenever(registryRepository.forYouStations(any())).thenAnswer { invocation ->
            val country = invocation.getArgument<String>(0)
            requested += country
            listOf(station("ForYou-$country"))
        }
        val holder = holder(registryRepository = registryRepository)

        holder.start()
        advanceUntilIdle()
        holder.setForYouCountry("   ")
        advanceUntilIdle()

        // The default stays "GB" and the list does not churn.
        assertEquals(listOf("GB"), requested)
        assertEquals(listOf("ForYou-GB"), holder.forYouStations.value.map { it.name })
        scope.cancel()
    }

    @Test
    fun recentlyPlayedSkipsHistoryEntriesNoLongerInTheRegistry() = runTest {
        // A station can leave the registry while it stays in play history. Those entries have
        // no registry row to resolve against, so they must not appear in the home list.
        val registryRepository = mock<RegistryRepository>()
        whenever(registryRepository.countAsFlow()).thenReturn(MutableStateFlow(10))
        whenever(registryRepository.getByProviderId("p", "kept")).thenReturn(station("Kept"))
        whenever(registryRepository.getByProviderId("p", "gone")).thenReturn(null)
        val repository = mock<StationRepository>()
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(
            MutableStateFlow(
                listOf(
                    PlayHistoryEntry(provider = "p", providerId = "kept", playedAt = 2L),
                    PlayHistoryEntry(provider = "p", providerId = "gone", playedAt = 1L),
                ),
            ),
        )
        whenever(repository.findMatching(any())).thenReturn(null)
        val holder = holder(repository = repository, registryRepository = registryRepository)

        holder.start()
        advanceUntilIdle()

        assertEquals(listOf("Kept"), holder.recentlyPlayedStations.value.map { it.name })
        scope.cancel()
    }

    @Test
    fun aLocallySavedStationSuppliesItsOwnArtworkWhenTheFileStillExists() = runTest {
        // The registry's copy may have no logo, or one the user replaced locally with their own
        // upload. The saved station's artwork is the one the user chose, so it wins — but only
        // while the file is actually there.
        val localArtwork = File.createTempFile("aerial-recent", ".svg").apply { writeText("<svg/>") }
        try {
            val registryRepository = mock<RegistryRepository>()
            whenever(registryRepository.countAsFlow()).thenReturn(MutableStateFlow(10))
            whenever(registryRepository.getByProviderId("p", "one"))
                .thenReturn(station("One", logo = "https://cdn/one.png"))
            val repository = mock<StationRepository>()
            whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(
                MutableStateFlow(listOf(PlayHistoryEntry(provider = "p", providerId = "one", playedAt = 1L))),
            )
            whenever(repository.findMatching(any())).thenReturn(
                Station(
                    id = 1,
                    name = "One",
                    streamUrl = "https://example.invalid/one",
                    logoPath = localArtwork.absolutePath,
                ),
            )
            val holder = holder(repository = repository, registryRepository = registryRepository)

            holder.start()
            advanceUntilIdle()

            assertEquals(localArtwork.absolutePath, holder.recentlyPlayedStations.value.single().logoUrl)
        } finally {
            localArtwork.delete()
        }
        scope.cancel()
    }

    @Test
    fun aStaleLocalArtworkPathFallsBackToTheRegistryUrl() = runTest {
        // The cached file is gone — cleared cache, or an uninstall that left the row behind.
        // Showing a path to a file that is not there is worse than showing the registry's own
        // logo, so the remote URL wins.
        val registryRepository = mock<RegistryRepository>()
        whenever(registryRepository.countAsFlow()).thenReturn(MutableStateFlow(10))
        whenever(registryRepository.getByProviderId("p", "one"))
            .thenReturn(station("One", logo = "https://cdn/one.png"))
        val repository = mock<StationRepository>()
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(
            MutableStateFlow(listOf(PlayHistoryEntry(provider = "p", providerId = "one", playedAt = 1L))),
        )
        whenever(repository.findMatching(any())).thenReturn(
            Station(
                id = 1,
                name = "One",
                streamUrl = "https://example.invalid/one",
                logoPath = "/nonexistent/aerial/gone.svg",
            ),
        )
        val holder = holder(repository = repository, registryRepository = registryRepository)

        holder.start()
        advanceUntilIdle()

        assertEquals("https://cdn/one.png", holder.recentlyPlayedStations.value.single().logoUrl)
        scope.cancel()
    }

    @Test
    fun theFirstRecentlyPlayedEmissionReleasesTheStartupGate() = runTest {
        val registryRepository = mock<RegistryRepository>()
        whenever(registryRepository.countAsFlow()).thenReturn(MutableStateFlow(10))
        whenever(registryRepository.getByProviderId("p", "one")).thenReturn(station("One"))
        val repository = mock<StationRepository>()
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(
            MutableStateFlow(listOf(PlayHistoryEntry(provider = "p", providerId = "one", playedAt = 1L))),
        )
        whenever(repository.findMatching(any())).thenReturn(null)
        val holder = holder(repository = repository, registryRepository = registryRepository)

        holder.start()
        runCurrent()
        // Resolves because the first emission already arrived; the caller wraps this in a
        // timeout, so a hang here would be invisible without this test.
        holder.awaitFirstRecentlyPlayed()

        assertEquals(listOf("One"), holder.recentlyPlayedStations.value.map { it.name })
        scope.cancel()
    }

    @Test
    fun aFailingHistoryQueryStillReleasesTheStartupGate() = runTest {
        // A broken play-history table must not hold the splash screen for the full startup
        // timeout: the failure is known immediately, so the gate should open immediately too.
        val registryRepository = mock<RegistryRepository>()
        whenever(registryRepository.countAsFlow()).thenReturn(MutableStateFlow(10))
        val repository = mock<StationRepository>()
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(
            flow<List<PlayHistoryEntry>> { throw IllegalStateException("history table missing") },
        )
        val holder = holder(repository = repository, registryRepository = registryRepository)

        holder.start()
        runCurrent()
        holder.awaitFirstRecentlyPlayed()

        assertTrue(holder.recentlyPlayedStations.value.isEmpty())
        scope.cancel()
    }

    private var scope: CoroutineScope = CoroutineScope(SupervisorJob())

    private fun TestScope.holder(
        repository: StationRepository = mock(),
        registryRepository: RegistryRepository = mock(),
    ): DiscoveryStateHolder {
        scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        return DiscoveryStateHolder(scope, repository, registryRepository)
    }

    private fun station(name: String, logo: String = "https://example.invalid/${name.lowercase()}.png") =
        RegistryStation(
            name = name,
            streamUrl = "https://example.invalid/${name.lowercase()}",
            logoUrl = logo,
            provider = "p",
            providerId = name.lowercase(),
        )
}
