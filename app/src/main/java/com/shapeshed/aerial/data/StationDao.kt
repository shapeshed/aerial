package com.shapeshed.aerial.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<Station>>

    @Query("SELECT * FROM stations WHERE id = :id")
    suspend fun getById(id: Long): Station?

    @Query("SELECT * FROM stations WHERE streamUrl = :streamUrl LIMIT 1")
    suspend fun getByStreamUrl(streamUrl: String): Station?

    @Query("SELECT * FROM stations WHERE provider = :provider AND providerId = :providerId LIMIT 1")
    suspend fun getByProviderId(provider: String, providerId: String): Station?

    @Query(
        "SELECT s.* FROM stations s " +
            "JOIN stations_fts fts ON s.id = fts.rowid " +
            "WHERE stations_fts MATCH :match ORDER BY s.name COLLATE NOCASE ASC LIMIT 20",
    )
    suspend fun searchStationFts(match: String): List<Station>

    @Query("UPDATE stations SET streamUrl = :streamUrl WHERE provider = :provider AND providerId = :providerId AND streamUrl != :streamUrl")
    suspend fun updateStreamUrlByProviderId(provider: String, providerId: String, streamUrl: String)

    @Query("UPDATE stations SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE id = :id")
    suspend fun recordPlay(id: Long, playedAt: Long)

    @Insert
    suspend fun insert(station: Station): Long

    @Update
    suspend fun update(station: Station)

    @Delete
    suspend fun delete(station: Station)
}
