package com.shapeshed.aerial.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import java.io.File
import java.util.concurrent.CancellationException
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Station list published as a release asset by the release workflow. Updates are manual only:
 * nothing here runs unless the user asks for it from Settings.
 */
private const val STATION_LIST_URL =
    "https://github.com/shapeshed/aerial/releases/download/registry-latest/registry.db.compressed"

private const val IMPORT_BATCH = 1_000
private const val IDENTITY_HASH_QUERY = "SELECT identity_hash FROM room_master_table"

interface StationListUpdater {
    /** Station count after a successful update, or null if the list was left untouched. */
    suspend fun update(): Int?
}

/**
 * Replaces the bundled station registry with the published one. The download is only a source of
 * rows: the live registry is swapped inside a single transaction, so a failed or partial download
 * leaves the bundled list untouched. The bundled asset itself is never modified.
 */
class ReleaseStationListUpdater(
    private val database: RegistryDatabase,
    private val cacheDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StationListUpdater {
    override suspend fun update(): Int? = withContext(ioDispatcher) {
        // A per-run file: two updates must never write through the same handle.
        val source = File.createTempFile("registry-update", ".db", cacheDir)
        try {
            httpGetStream(STATION_LIST_URL) { input ->
                GZIPInputStream(input).use { gzip ->
                    source.outputStream().use { gzip.copyTo(it) }
                }
            } ?: return@withContext null
            try {
                importStationList(database, source)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Nothing was written, so the bundled list still stands; the caller reports it.
                null
            }
        } finally {
            source.delete()
        }
    }
}

/**
 * Replaces every row in [database] with the contents of [source], a registry database built from
 * the same Room schema. Returns the station count, or null if [source] was refused.
 *
 * Ids are carried over for stations that still exist: the generator assigns them in snapshot
 * order, so an updated snapshot would otherwise hand an existing id to a different station, and
 * Android Auto resolves its cached `reg:<id>` browse items by id when the user presses play.
 * Ids of stations that disappeared are never reused.
 */
internal suspend fun importStationList(database: RegistryDatabase, source: File): Int? =
    runCatching { SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READONLY) }
        .getOrNull()
        ?.use { published ->
            // A database built from another Room schema has the same table but different columns,
            // and an empty one would wipe the list, so refuse both rather than destroy the data.
            val matchesSchema = published.identityHash() == database.identityHash()
            if (!matchesSchema || published.stationCount() == 0) return null
            // Freed ids are always <= the local max, so numbering above it never reuses one.
            val idsByIdentity = database.stationIdsByIdentity()
            var nextFreeId = (idsByIdentity.values.maxOrNull() ?: 0L) + 1
            database.withTransaction {
                val dao = database.registryDao()
                dao.deleteAll()
                published.rawQuery("SELECT * FROM registry_stations", null).use { cursor ->
                    val batch = ArrayList<RegistryStation>(IMPORT_BATCH)
                    while (cursor.moveToNext()) {
                        val station = cursor.registryStation()
                        val id = idsByIdentity.remove(station.identityKey()) ?: nextFreeId++
                        batch += station.copy(id = id)
                        if (batch.size == IMPORT_BATCH) {
                            dao.insertAll(batch)
                            batch.clear()
                        }
                    }
                    if (batch.isNotEmpty()) dao.insertAll(batch)
                }
                dao.count()
            }
        }

/** Stable identity of a station across registry snapshots. */
private fun RegistryStation.identityKey(): String =
    if (providerId.isNotBlank()) "id:$provider/$providerId" else "url:$streamUrl"

/** Ids of the registry's current rows, keyed by [identityKey]. */
private fun RegistryDatabase.stationIdsByIdentity(): MutableMap<String, Long> {
    val ids = HashMap<String, Long>()
    openHelper.readableDatabase
        .query("SELECT id, provider, providerId, streamUrl FROM registry_stations")
        .use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow("id")
            while (cursor.moveToNext()) {
                val providerId = cursor.string("providerId")
                val key = if (providerId.isNotBlank()) {
                    "id:${cursor.string("provider")}/$providerId"
                } else {
                    "url:${cursor.string("streamUrl")}"
                }
                // Duplicate identities in the snapshot keep the first id, so the mapping is stable.
                ids.putIfAbsent(key, cursor.getLong(idColumn))
            }
        }
    return ids
}

private fun Cursor.string(column: String): String = getString(getColumnIndexOrThrow(column)).orEmpty()

private fun RegistryDatabase.identityHash(): String? =
    openHelper.readableDatabase.query(IDENTITY_HASH_QUERY).use { it.firstString() }

private fun SQLiteDatabase.identityHash(): String? =
    runCatching { rawQuery(IDENTITY_HASH_QUERY, null).use { it.firstString() } }.getOrNull()

private fun SQLiteDatabase.stationCount(): Int =
    runCatching { rawQuery("SELECT COUNT(*) FROM registry_stations", null).use { it.firstInt() } }.getOrDefault(0)

private fun Cursor.firstString(): String? = if (moveToFirst()) getString(0) else null

private fun Cursor.firstInt(): Int = if (moveToFirst()) getInt(0) else 0

private fun Cursor.registryStation(): RegistryStation {
    fun text(column: String): String = string(column)
    return RegistryStation(
        id = getLong(getColumnIndexOrThrow("id")),
        name = text("name"),
        streamUrl = text("streamUrl"),
        logoUrl = text("logoUrl"),
        country = text("country"),
        countryCode = text("countryCode"),
        tags = text("tags"),
        provider = text("provider"),
        providerId = text("providerId"),
        description = text("description"),
        searchText = text("searchText"),
    )
}
