package com.shapeshed.aerial.data

import kotlinx.coroutines.flow.Flow

class StationRepository(private val dao: StationDao) {
    fun getAll(): Flow<List<Station>> = dao.getAll()
    suspend fun getById(id: Long): Station? = dao.getById(id)
    suspend fun getByStreamUrl(streamUrl: String): Station? = dao.getByStreamUrl(streamUrl)
    suspend fun insert(station: Station): Long = dao.insert(station)
    suspend fun update(station: Station) = dao.update(station)
    suspend fun delete(station: Station) = dao.delete(station)

    suspend fun insertOrGetExisting(station: Station): Long {
        val existing = findExisting(station)
        if (existing != null) {
            val updated = existing.copy(
                logoPath = existing.logoPath.ifBlank { station.logoPath },
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
            dao.update(station.copy(id = existing.id))
        }
    }

    suspend fun updateStreamUrlsFromRegistry(stations: List<RegistryStation>) {
        stations.forEach { s ->
            if (s.provider.isNotBlank() && s.providerId.isNotBlank()) {
                dao.updateStreamUrlByProviderId(s.provider, s.providerId, s.streamUrl)
            }
        }
    }

    suspend fun saveAsFavorite(station: Station): Long {
        val existing = findExisting(station)
        return if (existing != null) {
            if (!existing.isFavorite) dao.update(existing.copy(isFavorite = true))
            existing.id
        } else {
            dao.insert(station.copy(id = 0, isFavorite = true))
        }
    }

    private suspend fun findExisting(station: Station): Station? {
        return dao.getByStreamUrl(station.streamUrl)
    }
}
