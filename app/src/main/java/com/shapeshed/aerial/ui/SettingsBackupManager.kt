package com.shapeshed.aerial.ui

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.shapeshed.aerial.SHOW_HOME_KEY
import com.shapeshed.aerial.SHOW_STREAM_BITRATE_KEY
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal sealed interface BackupOperationResult<out T> {
    data class Success<T>(val value: T) : BackupOperationResult<T>
    data class Failure(val error: Exception) : BackupOperationResult<Nothing>
}

internal interface SettingsBackupManager {
    suspend fun export(uri: Uri): BackupOperationResult<Unit>
    suspend fun import(uri: Uri): BackupOperationResult<Int>
}

internal class ZipSettingsBackupManager(
    context: Context,
    private val repository: StationRepository,
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SettingsBackupManager {
    private val context = context.applicationContext

    override suspend fun export(uri: Uri): BackupOperationResult<Unit> = execute {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            write(output)
        } ?: throw IllegalStateException("Could not open export file")
    }

    override suspend fun import(uri: Uri): BackupOperationResult<Int> = execute {
        context.contentResolver.openInputStream(uri)?.use { input ->
            read(input)
        } ?: throw IllegalStateException("Could not open import file")
    }

    internal suspend fun write(output: OutputStream) {
        val stations = repository.getAll().first()
        val preferences = dataStore.data.first()
        val stationArray = JSONArray()

        ZipOutputStream(output).use { zip ->
            stations.forEachIndexed { index, station ->
                val stationJson = JSONObject()
                    .put("name", station.name)
                    .put("streamUrl", station.streamUrl)
                    .put("isFavorite", station.isFavorite)
                    .put("provider", station.provider)
                    .put("providerId", station.providerId)
                    .put("tags", station.tags)
                    .put("description", station.description)
                    .put("country", station.country)
                    .put("countryCode", station.countryCode)
                    .put("playCount", station.playCount)
                    .put("lastPlayedAt", station.lastPlayedAt)

                val logoFile = station.logoPath
                    .takeIf { it.isNotBlank() && !it.startsWith("http") }
                    ?.let(::File)
                    ?.takeIf(File::isFile)

                if (logoFile != null) {
                    val entryName = "logos/${index}_${safeFileName(logoFile.name)}"
                    stationJson.put("logoFile", entryName)
                    zip.putNextEntry(ZipEntry(entryName))
                    logoFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                } else {
                    stationJson.put("logoPath", station.logoPath)
                }

                stationArray.put(stationJson)
            }

            val settings = JSONObject()
                .put("showStreamBitrate", preferences[SHOW_STREAM_BITRATE_KEY] ?: false)
                .put("showHome", preferences[SHOW_HOME_KEY] ?: true)
            val manifest = JSONObject()
                .put("version", BACKUP_VERSION)
                .put("app", "Aerial")
                .put("settings", settings)
                .put("stations", stationArray)

            zip.putNextEntry(ZipEntry(BACKUP_MANIFEST))
            zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    internal suspend fun read(input: InputStream): Int {
        val restoredLogos = mutableMapOf<String, String>()
        var manifestJson: String? = null
        val logoDir = File(context.filesDir, "logos").also { it.mkdirs() }

        ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                when {
                    entry.isDirectory -> Unit
                    entry.name == BACKUP_MANIFEST -> {
                        manifestJson = ByteArrayOutputStream().use { output ->
                            zip.copyTo(output)
                            output.toString(Charsets.UTF_8.name())
                        }
                    }
                    entry.name.startsWith("logos/") -> {
                        val file = File(
                            logoDir,
                            "${UUID.randomUUID()}_${safeFileName(entry.name.substringAfterLast('/'))}",
                        )
                        file.outputStream().use { zip.copyTo(it) }
                        ensureMediaArtworkForLogo(context, file)
                        restoredLogos[entry.name] = file.absolutePath
                    }
                }
                zip.closeEntry()
            }
        }

        val manifest = JSONObject(manifestJson ?: throw IllegalArgumentException("Backup manifest is missing"))
        if (manifest.optInt("version") != BACKUP_VERSION) {
            throw IllegalArgumentException("Unsupported backup version")
        }

        val stations = manifest.optJSONArray("stations") ?: JSONArray()
        for (index in 0 until stations.length()) {
            val item = stations.getJSONObject(index)
            val logoPath = item.optString("logoFile")
                .takeIf { it.isNotBlank() }
                ?.let(restoredLogos::get)
                ?: item.optString("logoPath")

            repository.upsertImported(
                Station(
                    name = item.getString("name").trim(),
                    streamUrl = item.getString("streamUrl").trim(),
                    logoPath = logoPath.trim(),
                    isFavorite = item.optBoolean("isFavorite", true),
                    provider = item.optString("provider").trim(),
                    providerId = item.optString("providerId").trim(),
                    tags = item.optString("tags").trim(),
                    description = item.optString("description").trim(),
                    country = item.optString("country").trim(),
                    countryCode = item.optString("countryCode").trim(),
                    playCount = item.optInt("playCount"),
                    lastPlayedAt = item.optLong("lastPlayedAt"),
                ),
            )
        }

        manifest.optJSONObject("settings")?.let { settings ->
            dataStore.edit { preferences ->
                if (settings.has("showStreamBitrate")) {
                    preferences[SHOW_STREAM_BITRATE_KEY] = settings.optBoolean("showStreamBitrate")
                }
                if (settings.has("showHome")) {
                    preferences[SHOW_HOME_KEY] = settings.optBoolean("showHome")
                }
            }
        }

        return stations.length()
    }

    private suspend fun <T> execute(block: suspend () -> T): BackupOperationResult<T> =
        try {
            BackupOperationResult.Success(withContext(ioDispatcher) { block() })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            BackupOperationResult.Failure(error)
        }

    private fun safeFileName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "logo" }

    private companion object {
        const val BACKUP_VERSION = 1
        const val BACKUP_MANIFEST = "backup.json"
    }
}
