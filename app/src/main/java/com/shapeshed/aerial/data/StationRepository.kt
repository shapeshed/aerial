package com.shapeshed.aerial.data

import kotlinx.coroutines.flow.Flow

class StationRepository(private val dao: StationDao) {
    fun getAll(): Flow<List<Station>> = dao.getAll()
    suspend fun getById(id: Long): Station? = dao.getById(id)
    suspend fun getByStreamUrl(streamUrl: String): Station? = dao.getByStreamUrl(streamUrl)
    suspend fun searchFavorites(query: String): List<Station> {
        val match = toFtsMatchQuery(NumberNormalizer.normalize(query.trim()))
        return if (match.isBlank()) emptyList() else dao.searchStationFts(match)
    }
    suspend fun insert(station: Station): Long = dao.insert(station)
    suspend fun update(station: Station) = dao.update(station)
    suspend fun delete(station: Station) = dao.delete(station)
    suspend fun recordPlay(id: Long, playedAt: Long) = dao.recordPlay(id, playedAt)

    suspend fun insertOrGetExisting(station: Station): Long {
        val existing = findExisting(station)
        if (existing != null) {
            val updated = existing.copy(
                isFavorite = true,
                logoPath = existing.logoPath.ifBlank { station.logoPath },
                tags = existing.tags.ifBlank { station.tags },
                description = existing.description.ifBlank { station.description },
                country = existing.country.ifBlank { station.country },
                countryCode = existing.countryCode.ifBlank { station.countryCode },
            )
            if (updated != existing) dao.update(updated)
            return existing.id
        }
        return dao.insert(station.copy(id = 0))
    }

    suspend fun upsertImported(station: Station) {
        val existing = findExisting(station)

        if (existing == null) {
            dao.insert(station.copy(id = 0))
        } else {
            // Merge play stats rather than overwrite: a restored backup may be older than
            // the local row, and importing must never regress listening history.
            dao.update(
                station.copy(
                    id = existing.id,
                    playCount = maxOf(existing.playCount, station.playCount),
                    lastPlayedAt = maxOf(existing.lastPlayedAt, station.lastPlayedAt),
                ),
            )
        }
    }

    suspend fun updateStreamUrlsFromRegistry(stations: List<RegistryStation>) {
        stations.forEach { s ->
            if (s.provider.isNotBlank() && s.providerId.isNotBlank()) {
                dao.updateStreamUrlByProviderId(s.provider, s.providerId, s.streamUrl)
            }
        }
    }

    suspend fun findMatching(registryStation: RegistryStation): Station? {
        return if (registryStation.provider.isNotBlank() && registryStation.providerId.isNotBlank()) {
            dao.getByProviderId(registryStation.provider, registryStation.providerId)
        } else {
            null
        } ?: dao.getByStreamUrl(registryStation.streamUrl)
    }

    suspend fun saveAsFavorite(station: Station): Long {
        val existing = findExisting(station)
        return if (existing != null) {
            val updated = existing.copy(
                isFavorite = true,
                logoPath = existing.logoPath.ifBlank { station.logoPath },
                tags = existing.tags.ifBlank { station.tags },
                description = existing.description.ifBlank { station.description },
                country = existing.country.ifBlank { station.country },
                countryCode = existing.countryCode.ifBlank { station.countryCode },
            )
            if (updated != existing) dao.update(updated)
            existing.id
        } else {
            dao.insert(station.copy(id = 0, isFavorite = true))
        }
    }

    private suspend fun findExisting(station: Station): Station? {
        return if (station.provider.isNotBlank() && station.providerId.isNotBlank()) {
            dao.getByProviderId(station.provider, station.providerId)
        } else {
            null
        } ?: dao.getByStreamUrl(station.streamUrl)
    }
}
