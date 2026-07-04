package com.shapeshed.aerial.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.ENRICH_METADATA_KEY
import com.shapeshed.aerial.REGISTRY_LAST_SYNC_KEY
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SettingsViewModel(
    application: Application,
    private val repository: StationRepository,
    private val dataStore: DataStore<Preferences>,
) : AndroidViewModel(application) {

    val enrichMetadata = dataStore.data
        .map { it[ENRICH_METADATA_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun setEnrichMetadata(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[ENRICH_METADATA_KEY] = enabled }
        }
    }

    fun refreshRegistry() {
        viewModelScope.launch {
            val app = getApplication<AerialApp>()
            val stations = runCatching {
                withContext(Dispatchers.IO) { app.registryRepository.syncFromNetwork() }
            }.getOrNull()
            if (stations != null) {
                withContext(Dispatchers.IO) { app.repository.updateStreamUrlsFromRegistry(stations) }
                dataStore.edit { it[REGISTRY_LAST_SYNC_KEY] = System.currentTimeMillis() }
            }
            _messages.emit(if (stations != null) "Registry refreshed" else "Registry refresh failed")
        }
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        writeBackup(context, output)
                    } ?: error("Could not open export file")
                }
            }
            _messages.emit(
                result.fold(
                    onSuccess = { "Backup exported" },
                    onFailure = { "Export failed: ${it.message ?: "unknown error"}" },
                )
            )
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        readBackup(context, input)
                    } ?: error("Could not open import file")
                }
            }
            _messages.emit(
                result.fold(
                    onSuccess = { count -> "Imported $count stations and settings" },
                    onFailure = { "Import failed: ${it.message ?: "unknown error"}" },
                )
            )
        }
    }

    private suspend fun writeBackup(context: Context, output: OutputStream) {
        val stations = repository.getAll().first()
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

                val logoFile = station.logoPath
                    .takeIf { it.isNotBlank() && !it.startsWith("http") }
                    ?.let { File(it) }
                    ?.takeIf { it.isFile }

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

            val manifest = JSONObject()
                .put("version", 1)
                .put("app", "Aerial")
                .put("settings", JSONObject())
                .put("stations", stationArray)

            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    private suspend fun readBackup(context: Context, input: InputStream): Int {
        val restoredLogos = mutableMapOf<String, String>()
        var manifestJson: String? = null
        val logoDir = File(context.filesDir, "logos").also { it.mkdirs() }

        ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                when {
                    entry.isDirectory -> Unit
                    entry.name == "backup.json" -> {
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

        val manifest = JSONObject(manifestJson ?: error("Backup manifest is missing"))
        if (manifest.optInt("version") != 1) {
            error("Unsupported backup version")
        }

        val stations = manifest.optJSONArray("stations") ?: JSONArray()
        for (index in 0 until stations.length()) {
            val item = stations.getJSONObject(index)
            val logoPath = item.optString("logoFile")
                .takeIf { it.isNotBlank() }
                ?.let { restoredLogos[it] }
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
                )
            )
        }

        return stations.length()
    }

    private fun safeFileName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "logo" }
}

class SettingsViewModelFactory(
    private val application: Application,
    private val repository: StationRepository,
    private val dataStore: DataStore<Preferences>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(application, repository, dataStore) as T
    }
}
