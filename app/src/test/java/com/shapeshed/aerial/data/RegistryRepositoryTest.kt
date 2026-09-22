package com.shapeshed.aerial.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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

    @Test
    fun searchWithOnlyCountryFiltersQueriesTheCountryIndex() = runBlocking {
        val dao = mock<RegistryDao>()
        val british = registry(name = "British One", countryCode = "GB")
        val american = registry(name = "American One", countryCode = "US")
        whenever(dao.filterByCountryCodes(listOf("gb"))).thenReturn(listOf(british, american))
        val repository = RegistryRepository(dao)

        val result = repository.search(query = "", countryCodes = setOf("GB"))

        assertEquals(listOf(british), result)
    }

    @Test
    fun searchWithOnlyTagsKeepsExactWordMatches() = runBlocking {
        val dao = mock<RegistryDao>()
        val rock = registry(id = 1L, name = "Rock One", tags = "rock pop")
        val rockabilly = registry(id = 2L, name = "Rockabilly One", tags = "rockabilly")
        whenever(dao.byTagLike("rock")).thenReturn(listOf(rock, rockabilly))
        val repository = RegistryRepository(dao)

        val result = repository.search(query = "", tags = setOf("rock"))

        assertEquals(listOf(rock), result)
    }

    @Test
    fun availableTagsListsCuratedTagsFirstInTheirDeclaredOrder() = runBlocking {
        val dao = mock<RegistryDao>()
        whenever(dao.tagRows()).thenReturn(listOf("jazz", "jazz", "jazz", "jazz", "jazz", "news"))
        val repository = RegistryRepository(dao)

        // Curated order wins over frequency: News is declared before Jazz.
        assertEquals(listOf("News", "Jazz"), repository.availableTags())
    }

    @Test
    fun availableTagsRanksRemainingTagsByCountThenName() = runBlocking {
        val dao = mock<RegistryDao>()
        whenever(dao.tagRows()).thenReturn(listOf("ambient", "ambient", "ambient", "blues", "acid"))
        val repository = RegistryRepository(dao)

        assertEquals(listOf("Ambient", "Acid", "Blues"), repository.availableTags())
    }

    @Test
    fun availableTagsIgnoresTokensShorterThanThreeCharacters() = runBlocking {
        val dao = mock<RegistryDao>()
        whenever(dao.tagRows()).thenReturn(listOf("ab xyz pop"))
        val repository = RegistryRepository(dao)

        assertEquals(listOf("Pop", "Xyz"), repository.availableTags())
    }

    @Test
    fun featuredStationsMatchOnProviderAndProviderIdTogether() = runBlocking {
        val dao = mock<RegistryDao>()
        val bbcWorldService = registry(
            name = "BBC World Service",
            provider = "bbc",
            providerId = "bbc_world_service",
        )
        val franceInfo = registry(name = "franceinfo", provider = "radio-france", providerId = "2")
        // Same providerId as franceinfo, different provider — must not be mistaken for it.
        val providerIdClash = registry(name = "Upstream clash", provider = "other", providerId = "2")
        whenever(dao.getByProviderIds(any())).thenReturn(listOf(providerIdClash, bbcWorldService, franceInfo))
        val repository = RegistryRepository(dao)

        val result = repository.featuredStations()

        assertEquals(listOf(bbcWorldService, franceInfo), result)
    }

    @Test
    fun forYouPrefersCuratedStationsForTheUk(): Unit = runBlocking {
        val dao = mock<RegistryDao>()
        val smooth = registry(name = "Smooth Radio", provider = "curated")
        whenever(dao.getByNames(any())).thenReturn(listOf(smooth))
        val repository = RegistryRepository(dao)

        val result = repository.forYouStations("GB")

        assertEquals(listOf(smooth), result)
        verify(dao, never()).randomByCountryWithLogo(any(), any())
    }

    @Test
    fun forYouFallsBackToRandomLocalStations() = runBlocking {
        val dao = mock<RegistryDao>()
        val local = registry(name = "US Local", countryCode = "US")
        whenever(dao.randomByCountryWithLogo("US", 10)).thenReturn(listOf(local))
        val repository = RegistryRepository(dao)

        val result = repository.forYouStations("US")

        assertEquals(listOf(local), result)
    }

    private fun registry(
        name: String,
        id: Long = 0L,
        countryCode: String = "",
        tags: String = "",
        provider: String = "radio-browser",
        providerId: String = "",
    ) = RegistryStation(
        id = id,
        name = name,
        streamUrl = "https://example.test/$name",
        countryCode = countryCode,
        tags = tags,
        provider = provider,
        providerId = providerId,
    )
}
