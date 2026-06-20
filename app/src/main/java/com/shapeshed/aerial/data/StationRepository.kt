package com.shapeshed.aerial.data

import kotlinx.coroutines.flow.Flow

class StationRepository(private val dao: StationDao) {
    fun getAll(): Flow<List<Station>> = dao.getAll()
    suspend fun getById(id: Long): Station? = dao.getById(id)
    suspend fun insert(station: Station): Long = dao.insert(station)
    suspend fun update(station: Station) = dao.update(station)
    suspend fun delete(station: Station) = dao.delete(station)

    suspend fun insertOrGetExisting(station: Station): Long {
        val existing = findExisting(station)
        if (existing != null) {
            val updated = existing.copy(
                radioBrowserUuid = existing.radioBrowserUuid.ifBlank { station.radioBrowserUuid },
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

    private suspend fun findExisting(station: Station): Station? {
        return station.radioBrowserUuid
            .takeIf { it.isNotBlank() }
            ?.let { dao.getByRadioBrowserUuid(it) }
            ?: dao.getByStreamUrl(station.streamUrl)
    }
}
