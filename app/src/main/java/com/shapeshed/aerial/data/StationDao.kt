package com.shapeshed.aerial.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY name ASC")
    fun getAll(): Flow<List<Station>>

    @Query("SELECT * FROM stations WHERE id = :id")
    suspend fun getById(id: Long): Station?

    @Query("SELECT * FROM stations WHERE radioBrowserUuid = :uuid AND radioBrowserUuid != '' LIMIT 1")
    suspend fun getByRadioBrowserUuid(uuid: String): Station?

    @Query("SELECT * FROM stations WHERE streamUrl = :streamUrl LIMIT 1")
    suspend fun getByStreamUrl(streamUrl: String): Station?

    @Insert
    suspend fun insert(station: Station)

    @Update
    suspend fun update(station: Station)

    @Delete
    suspend fun delete(station: Station)
}
