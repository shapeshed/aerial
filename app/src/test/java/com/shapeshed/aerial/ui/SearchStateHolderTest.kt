package com.shapeshed.aerial.ui

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.testing.MemoryDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SearchStateHolderTest {
    @Test
    fun restoringFiltersAppliesThemToAnAlreadyPendingQuery() = runTest {
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.searchFavorites(any())).thenReturn(emptyList())
        whenever(registryRepository.search(any(), any(), any())).thenReturn(emptyList())
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val holder = SearchStateHolder(scope, repository, registryRepository, MemoryDataStore())
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("search_countries") to "GB",
            stringPreferencesKey("search_tags") to "Rock",
        )

        holder.search("mango")
        holder.restoreFilters(preferences)
        advanceTimeBy(250)
        advanceUntilIdle()

        verify(registryRepository).search("mango", setOf("GB"), setOf("Rock"))
        scope.cancel()
    }

    @Test
    fun lateRestoreDoesNotOverwriteAUserFilterChange() = runTest {
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val holder = SearchStateHolder(scope, repository, registryRepository, MemoryDataStore())

        holder.setCountry("FR")
        holder.restoreFilters(
            mutablePreferencesOf(stringPreferencesKey("search_countries") to "GB"),
        )

        assertEquals(setOf("FR"), holder.selectedCountries.value)
        scope.cancel()
    }

    @Test
    fun aQueryIsInFlightFromTheMomentItIsPublished() = runTest {
        val holder = holder()

        holder.search("r")
        // Let the request pipeline observe it. The flag is delivered like every other
        // StateFlow here — asynchronously — so it is set well before the next frame is drawn.
        runCurrent()

        // Before the debounce even elapses. Claiming "no stations found" this early tells the
        // user a station does not exist when the search has not run yet.
        assertTrue(holder.isSearching.value)
        scope.cancel()
    }

    @Test
    fun searchingClearsOnceResultsLand() = runTest {
        val holder = holder(registryResults = listOf(registryStation("Rock Radio")))

        holder.search("r")
        advanceTimeBy(250)
        runCurrent()

        assertFalse(holder.isSearching.value)
        assertEquals(listOf("Rock Radio"), holder.registryResults.value.map { it.name })
        scope.cancel()
    }

    @Test
    fun aCompletedSearchWithNoHitsIsNotSearching() = runTest {
        val holder = holder()

        holder.search("zzzz")
        advanceTimeBy(250)
        runCurrent()

        assertFalse("a finished search that matched nothing is not searching", holder.isSearching.value)
        assertEquals(emptyList<RegistryStation>(), holder.registryResults.value)
        scope.cancel()
    }

    @Test
    fun aBlankQueryIsNotSearching() = runTest {
        // A blank query shows recent searches rather than an empty result set, so it must not
        // put the UI into the searching state.
        val holder = holder()

        holder.search("   ")

        assertFalse(holder.isSearching.value)
        scope.cancel()
    }

    @Test
    fun typingAgainReturnsToSearchingBeforeTheNextResult() = runTest {
        val holder = holder(registryResults = listOf(registryStation("Rock Radio")))

        holder.search("r")
        advanceTimeBy(250)
        runCurrent()
        assertFalse(holder.isSearching.value)

        holder.search("ro")
        runCurrent()
        assertTrue(holder.isSearching.value)

        advanceTimeBy(250)
        runCurrent()
        assertFalse(holder.isSearching.value)
        scope.cancel()
    }

    @Test
    fun aSupersededQueryDoesNotReportItselfSettled() = runTest {
        // mapLatest cancels the in-flight query. The flag must stay set for the request that
        // replaced it rather than being cleared by the cancelled one.
        val holder = holder()

        holder.search("r")
        advanceTimeBy(125)
        holder.search("ro")

        assertTrue(holder.isSearching.value)
        advanceUntilIdle()
        assertFalse(holder.isSearching.value)
        scope.cancel()
    }

    private var scope: CoroutineScope = CoroutineScope(SupervisorJob())

    private suspend fun kotlinx.coroutines.test.TestScope.holder(
        registryResults: List<RegistryStation> = emptyList(),
    ): SearchStateHolder {
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.searchFavorites(any())).thenReturn(emptyList())
        whenever(registryRepository.search(any(), any(), any())).thenReturn(registryResults)
        scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        return SearchStateHolder(scope, repository, registryRepository, MemoryDataStore())
    }

    private fun registryStation(name: String) = RegistryStation(
        name = name,
        streamUrl = "https://example.invalid/${name.lowercase()}",
        provider = "test",
        providerId = name.lowercase(),
    )
}
