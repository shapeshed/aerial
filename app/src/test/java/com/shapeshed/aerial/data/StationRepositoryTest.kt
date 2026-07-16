package com.shapeshed.aerial.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class StationRepositoryTest {

    @Test
    fun insertOrGetExistingInsertsNewStation() = runBlocking {
        val dao = FakeStationDao()
        val repository = StationRepository(dao, FakePlayHistoryDao())

        val id = repository.insertOrGetExisting(station())

        assertEquals(1L, id)
        assertEquals(1, dao.insertCount)
        assertEquals(listOf(station(id = 1L)), dao.stations)
    }

    @Test
    fun insertOrGetExistingReturnsExistingStreamUrlStation() = runBlocking {
        val existing = station(id = 7L, logoPath = "/logos/existing.png")
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao, FakePlayHistoryDao())

        val id = repository.insertOrGetExisting(
            station(
                name = "Duplicate",
                logoPath = "/logos/new.png",
            )
        )

        assertEquals(7L, id)
        assertEquals(0, dao.insertCount)
        assertEquals(existing.copy(isFavorite = true), dao.stations.single())
    }

    @Test
    fun insertOrGetExistingMatchesByStreamUrl() = runBlocking {
        val existing = station(id = 4L)
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao, FakePlayHistoryDao())

        val id = repository.insertOrGetExisting(station())

        assertEquals(4L, id)
        assertEquals(0, dao.insertCount)
        assertEquals(existing.copy(isFavorite = true), dao.stations.single())
    }

    @Test
    fun insertOrGetExistingMatchesByProviderIdWhenStreamUrlChanged() = runBlocking {
        val existing = station(
            id = 5L,
            streamUrl = "https://stream.example.com/local-correction",
            provider = "aerial",
            providerId = "mango",
        )
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao, FakePlayHistoryDao())

        val id = repository.insertOrGetExisting(
            station(
                streamUrl = "https://stream.example.com/registry",
                provider = "aerial",
                providerId = "mango",
            )
        )

        assertEquals(5L, id)
        assertEquals(0, dao.insertCount)
        assertEquals(existing.copy(isFavorite = true), dao.stations.single())
    }

    @Test
    fun insertOrGetExistingKeepsExistingLogoWhenPresent() = runBlocking {
        val existing = station(id = 3L, logoPath = "/logos/existing.png")
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao, FakePlayHistoryDao())

        repository.insertOrGetExisting(station(logoPath = "/logos/new.png"))

        assertEquals("/logos/existing.png", dao.stations.single().logoPath)
    }

    @Test
    fun insertOrGetExistingMarksExistingStationAsFavorite() = runBlocking {
        val existing = station(id = 9L, isFavorite = false)
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao, FakePlayHistoryDao())

        repository.insertOrGetExisting(station(isFavorite = true))

        assertEquals(true, dao.stations.single().isFavorite)
    }

    @Test
    fun findMatchingFindsRegistryStationByProviderIdBeforeStreamUrl() = runBlocking {
        val providerMatch = station(
            id = 10L,
            streamUrl = "https://stream.example.com/local-correction",
            provider = "aerial",
            providerId = "mango",
        )
        val streamMatch = station(id = 11L, streamUrl = "https://stream.example.com/registry")
        val dao = FakeStationDao(providerMatch, streamMatch)
        val repository = StationRepository(dao, FakePlayHistoryDao())

        val result = repository.findMatching(
            RegistryStation(
                name = "Mango",
                streamUrl = "https://stream.example.com/registry",
                provider = "aerial",
                providerId = "mango",
            )
        )

        assertEquals(providerMatch, result)
    }

    @Test
    fun findMatchingFallsBackToStreamUrl() = runBlocking {
        val existing = station(id = 12L, streamUrl = "https://stream.example.com/registry")
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao, FakePlayHistoryDao())

        val result = repository.findMatching(
            RegistryStation(
                name = "Mango",
                streamUrl = "https://stream.example.com/registry",
                provider = "aerial",
                providerId = "missing",
            )
        )

        assertEquals(existing, result)
    }

    @Test
    fun saveAsFavoriteMatchesProviderIdAndKeepsLocalEdits() = runBlocking {
        val existing = station(
            id = 8L,
            name = "Mango Local",
            streamUrl = "https://stream.example.com/local-correction",
            logoPath = "/logos/local.png",
            isFavorite = true,
            provider = "aerial",
            providerId = "mango",
        )
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao, FakePlayHistoryDao())

        val id = repository.saveAsFavorite(
            station(
                name = "Mango Registry",
                streamUrl = "https://stream.example.com/registry",
                logoPath = "/logos/registry.png",
                provider = "aerial",
                providerId = "mango",
            )
        )

        assertEquals(8L, id)
        assertEquals(existing, dao.stations.single())
    }

    @Test
    fun upsertImportedKeepsNewerLocalPlayStats() = runBlocking {
        val existing = station(id = 6L, playCount = 12, lastPlayedAt = 2_000L)
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao, FakePlayHistoryDao())

        repository.upsertImported(station(playCount = 3, lastPlayedAt = 1_000L))

        assertEquals(12, dao.stations.single().playCount)
        assertEquals(2_000L, dao.stations.single().lastPlayedAt)
    }

    @Test
    fun upsertImportedRestoresPlayStatsOntoFreshInstall() = runBlocking {
        val dao = FakeStationDao()
        val repository = StationRepository(dao, FakePlayHistoryDao())

        repository.upsertImported(station(playCount = 7, lastPlayedAt = 3_000L))

        assertEquals(7, dao.stations.single().playCount)
        assertEquals(3_000L, dao.stations.single().lastPlayedAt)
    }

    @Test
    fun searchFavoritesIncludesSavedStationsImportedWithoutFavoriteFlag() = runBlocking {
        val dao = FakeStationDao(
            station(id = 2L, name = "dublab", isFavorite = false),
        )
        val repository = StationRepository(dao, FakePlayHistoryDao())

        val results = repository.searchFavorites("dublab")

        assertEquals(listOf(dao.stations.single()), results)
    }

    @Test
    fun searchFavoritesMatchesTagsAndDescription() = runBlocking {
        val dao = FakeStationDao(
            station(
                id = 3L,
                name = "Kool FM",
                tags = "drum bass jungle",
                description = "Underground radio",
                country = "United Kingdom",
            ),
        )
        val repository = StationRepository(dao, FakePlayHistoryDao())

        assertEquals(listOf(dao.stations.single()), repository.searchFavorites("jungle"))
        assertEquals(listOf(dao.stations.single()), repository.searchFavorites("underground"))
        assertEquals(listOf(dao.stations.single()), repository.searchFavorites("kingdom"))
    }

    private fun station(
        id: Long = 0,
        name: String = "Mango",
        streamUrl: String = "https://stream.example.com/mango",
        logoPath: String = "",
        isFavorite: Boolean = false,
        provider: String = "",
        providerId: String = "",
        tags: String = "",
        description: String = "",
        country: String = "",
        countryCode: String = "",
        playCount: Int = 0,
        lastPlayedAt: Long = 0,
    ) = Station(
        id = id,
        name = name,
        streamUrl = streamUrl,
        logoPath = logoPath,
        isFavorite = isFavorite,
        provider = provider,
        providerId = providerId,
        tags = tags,
        description = description,
        country = country,
        countryCode = countryCode,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
    )

    private class FakeStationDao(vararg initialStations: Station) : StationDao {
        val stations = initialStations.toMutableList()
        var insertCount = 0
        private var nextId = (stations.maxOfOrNull { it.id } ?: 0L) + 1L

        override fun getAll(): Flow<List<Station>> = flowOf(stations)

        override suspend fun getById(id: Long): Station? {
            return stations.firstOrNull { it.id == id }
        }

        override suspend fun getByStreamUrl(streamUrl: String): Station? {
            return stations.firstOrNull { it.streamUrl == streamUrl }
        }

        override suspend fun getByProviderId(provider: String, providerId: String): Station? {
            return stations.firstOrNull { it.provider == provider && it.providerId == providerId }
        }

        override suspend fun searchStationFts(match: String): List<Station> {
            val terms = match.split(" ").map { it.removeSuffix("*").lowercase() }
            return stations
                .filter { station ->
                    val text = "${station.name} ${station.streamUrl} ${station.provider} ${station.providerId} " +
                        "${station.tags} ${station.description} ${station.country}"
                    val searchable = text.lowercase()
                    terms.all { searchable.contains(it) }
                }
                .sortedBy { it.name.lowercase() }
                .take(20)
        }

        override suspend fun updateStreamUrlByProviderId(provider: String, providerId: String, streamUrl: String) {
            stations.replaceAll {
                if (it.provider == provider && it.providerId == providerId && it.streamUrl != streamUrl) {
                    it.copy(streamUrl = streamUrl)
                } else {
                    it
                }
            }
        }

        override suspend fun recordPlay(id: Long, playedAt: Long) {
            stations.replaceAll {
                if (it.id == id) it.copy(playCount = it.playCount + 1, lastPlayedAt = playedAt) else it
            }
        }

        override suspend fun insert(station: Station): Long {
            insertCount += 1
            val id = station.id.takeIf { it != 0L } ?: nextId++
            stations += station.copy(id = id)
            return id
        }

        override suspend fun update(station: Station) {
            val index = stations.indexOfFirst { it.id == station.id }
            if (index != -1) {
                stations[index] = station
            }
        }

        override suspend fun delete(station: Station) {
            stations.removeAll { it.id == station.id }
        }
    }

    private class FakePlayHistoryDao(vararg initialEntries: PlayHistoryEntry) : PlayHistoryDao {
        val entries = initialEntries.toMutableList()

        override suspend fun recordPlay(entry: PlayHistoryEntry) {
            entries.removeAll { it.provider == entry.provider && it.providerId == entry.providerId }
            entries += entry
        }

        override suspend fun recent(limit: Int): List<PlayHistoryEntry> =
            entries.sortedByDescending { it.playedAt }.take(limit)

        override fun recentAsFlow(limit: Int): Flow<List<PlayHistoryEntry>> =
            flowOf(entries.sortedByDescending { it.playedAt }.take(limit))
    }
}
