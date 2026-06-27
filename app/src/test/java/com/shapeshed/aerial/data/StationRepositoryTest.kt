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
        val repository = StationRepository(dao)

        val id = repository.insertOrGetExisting(station())

        assertEquals(1L, id)
        assertEquals(1, dao.insertCount)
        assertEquals(listOf(station(id = 1L)), dao.stations)
    }

    @Test
    fun insertOrGetExistingReturnsExistingStreamUrlStation() = runBlocking {
        val existing = station(id = 7L, logoPath = "/logos/existing.png")
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao)

        val id = repository.insertOrGetExisting(
            station(
                name = "Duplicate",
                logoPath = "/logos/new.png",
            )
        )

        assertEquals(7L, id)
        assertEquals(0, dao.insertCount)
        assertEquals(existing, dao.stations.single())
    }

    @Test
    fun insertOrGetExistingMatchesByStreamUrl() = runBlocking {
        val existing = station(id = 4L)
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao)

        val id = repository.insertOrGetExisting(station())

        assertEquals(4L, id)
        assertEquals(0, dao.insertCount)
        assertEquals(existing, dao.stations.single())
    }

    @Test
    fun insertOrGetExistingKeepsExistingLogoWhenPresent() = runBlocking {
        val existing = station(id = 3L, logoPath = "/logos/existing.png")
        val dao = FakeStationDao(existing)
        val repository = StationRepository(dao)

        repository.insertOrGetExisting(station(logoPath = "/logos/new.png"))

        assertEquals("/logos/existing.png", dao.stations.single().logoPath)
    }

    private fun station(
        id: Long = 0,
        name: String = "Mango",
        streamUrl: String = "https://stream.example.com/mango",
        logoPath: String = "",
    ) = Station(
        id = id,
        name = name,
        streamUrl = streamUrl,
        logoPath = logoPath,
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

        override suspend fun updateStreamUrlByProviderId(provider: String, providerId: String, streamUrl: String) {
            stations.replaceAll {
                if (it.provider == provider && it.providerId == providerId && it.streamUrl != streamUrl) {
                    it.copy(streamUrl = streamUrl)
                } else {
                    it
                }
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
}
