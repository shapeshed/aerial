package com.shapeshed.aerial.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RegistryDao {
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

    @Query("SELECT * FROM registry_stations WHERE LOWER(countryCode) = LOWER(:countryCode) AND logoUrl != '' ORDER BY RANDOM() LIMIT :limit")
    abstract suspend fun randomByCountryWithLogo(countryCode: String, limit: Int): List<RegistryStation>

    @Query("SELECT * FROM registry_stations WHERE providerId IN (:ids)")
    abstract suspend fun getByProviderIds(ids: List<String>): List<RegistryStation>

    @Query("SELECT * FROM registry_stations WHERE id = :id")
    abstract suspend fun getById(id: Long): RegistryStation?

    @Query("SELECT * FROM registry_stations WHERE name IN (:names)")
    abstract suspend fun getByNames(names: List<String>): List<RegistryStation>

    @Query("SELECT DISTINCT countryCode FROM registry_stations WHERE countryCode != '' ORDER BY countryCode")
    abstract suspend fun distinctCountryCodes(): List<String>

    @Query("SELECT tags FROM registry_stations WHERE tags != ''")
    abstract suspend fun tagRows(): List<String>

    @Query("SELECT * FROM registry_stations WHERE LOWER(countryCode) IN (:countryCodes) ORDER BY name")
    abstract suspend fun filterByCountryCodes(countryCodes: List<String>): List<RegistryStation>

    // Loose substring prefilter for a single tag — narrows the ~44k-row table down to a small
    // candidate set before the exact word-boundary match is applied in RegistryRepository, so a
    // tag-only filter (no search text) doesn't have to hydrate every row into a RegistryStation.
    @Query("SELECT * FROM registry_stations WHERE LOWER(tags) LIKE '%' || LOWER(:tag) || '%'")
    abstract suspend fun byTagLike(tag: String): List<RegistryStation>

    @Query("SELECT * FROM registry_stations ORDER BY name")
    abstract suspend fun all(): List<RegistryStation>

    @Query("SELECT * FROM registry_stations ORDER BY name LIMIT :limit")
    abstract suspend fun browse(limit: Int): List<RegistryStation>
}
