package com.shapeshed.aerial.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RegistryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(stations: List<RegistryStation>)

    @Query("DELETE FROM registry_stations")
    abstract suspend fun clear()

    @Transaction
    open suspend fun clearAndInsertAll(stations: List<RegistryStation>) {
        clear()
        insertAll(stations)
        // External-content FTS isn't auto-maintained; rebuild the index after a bulk import.
        rebuildFts()
    }

    @Query("INSERT INTO registry_stations_fts(registry_stations_fts) VALUES('rebuild')")
    abstract suspend fun rebuildFts()

    // Full-text search over name+tags (via searchText), description, and country. The join maps
    // FTS rowids back to the content rows. MATCH does the accent-folded, tokenized matching.
    @Query(
        "SELECT rs.* FROM registry_stations rs " +
            "JOIN registry_stations_fts fts ON rs.id = fts.rowid " +
            "WHERE registry_stations_fts MATCH :match ORDER BY rs.name LIMIT 200",
    )
    abstract suspend fun searchFts(match: String): List<RegistryStation>

    @Query("SELECT COUNT(*) FROM registry_stations")
    abstract fun countAsFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM registry_stations")
    abstract suspend fun count(): Int

    @Query("SELECT * FROM registry_stations WHERE LOWER(tags) LIKE '%' || LOWER(:tag) || '%' ORDER BY RANDOM() LIMIT 1")
    abstract suspend fun randomStationByTag(tag: String): RegistryStation?

    @Query("SELECT * FROM registry_stations WHERE providerId IN (:ids)")
    abstract suspend fun getByProviderIds(ids: List<String>): List<RegistryStation>

    @Query("SELECT DISTINCT countryCode FROM registry_stations WHERE countryCode != '' ORDER BY countryCode")
    abstract suspend fun distinctCountryCodes(): List<String>

    @Query("SELECT tags FROM registry_stations WHERE tags != ''")
    abstract suspend fun tagRows(): List<String>

    @Query("SELECT * FROM registry_stations WHERE LOWER(countryCode) IN (:countryCodes) ORDER BY name")
    abstract suspend fun filterByCountryCodes(countryCodes: List<String>): List<RegistryStation>

    @Query("SELECT * FROM registry_stations ORDER BY name")
    abstract suspend fun all(): List<RegistryStation>

    @Query("SELECT * FROM registry_stations ORDER BY name LIMIT :limit")
    abstract suspend fun browse(limit: Int): List<RegistryStation>
}
