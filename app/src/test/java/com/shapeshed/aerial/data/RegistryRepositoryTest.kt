package com.shapeshed.aerial.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RegistryRepositoryTest {
    @Test
    fun searchBuildsPrefixQueryAndAppliesCountryAndTagFilters() = runBlocking {
        val dao = mock<RegistryDao>()
        val matching = registry(name = "Radio Mango", countryCode = "GB", tags = "rock pop")
        val wrongCountry = registry(name = "Radio Mango US", countryCode = "US", tags = "rock")
        whenever(dao.searchFts("radio* mango*")).thenReturn(listOf(matching, wrongCountry))
        val repository = RegistryRepository(dao)

        val result = repository.search("  Radio Mango ", countryCodes = setOf("gb"), tags = setOf("rock"))

        assertEquals(listOf(matching), result)
    }

    @Test
    fun blankSearchWithoutFiltersDoesNotQueryDao() = runBlocking {
        val dao = mock<RegistryDao>()
        val repository = RegistryRepository(dao)

        assertEquals(emptyList<RegistryStation>(), repository.search("   "))
    }

    private fun registry(
        name: String,
        countryCode: String,
        tags: String,
    ) = RegistryStation(
        name = name,
        streamUrl = "https://example.test/$name",
        countryCode = countryCode,
        tags = tags,
    )
}
