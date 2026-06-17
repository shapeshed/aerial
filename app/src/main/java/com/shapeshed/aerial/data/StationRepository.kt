package com.shapeshed.aerial.data

import kotlinx.coroutines.flow.Flow

class StationRepository(private val dao: StationDao) {
    fun getAll(): Flow<List<Station>> = dao.getAll()
    suspend fun getById(id: Long): Station? = dao.getById(id)
    suspend fun insert(station: Station) = dao.insert(station)
    suspend fun update(station: Station) = dao.update(station)
    suspend fun delete(station: Station) = dao.delete(station)

    suspend fun upsertImported(station: Station) {
        val existing = station.radioBrowserUuid
            .takeIf { it.isNotBlank() }
            ?.let { dao.getByRadioBrowserUuid(it) }
            ?: dao.getByStreamUrl(station.streamUrl)

        if (existing == null) {
            dao.insert(station.copy(id = 0))
        } else {
            dao.update(station.copy(id = existing.id))
        }
    }
}
