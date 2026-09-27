package com.shapeshed.aerial.ui

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.testing.MemoryDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
