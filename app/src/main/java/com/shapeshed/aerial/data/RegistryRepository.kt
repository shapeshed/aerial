package com.shapeshed.aerial.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

private const val REGISTRY_URL = "https://aerial.shapeshed.com/registry.json.gz"

private val FEATURED_PROVIDER_IDS = listOf(
    "bbc_world_service", // BBC World Service
    "FRANCEINFO",        // franceinfo
    "21818908",          // Deutschlandfunk
    "ki1",               // KISS
)

private val CURATED_TAG_ORDER = listOf(
    "News", "Sport", "Pop", "Rock", "Jazz", "Classical", "Dance", "Soul", "Country", "Electronic"
)

class RegistryRepository(private val dao: RegistryDao) {

    private val httpClient = OkHttpClient()

    fun countAsFlow(): Flow<Int> = dao.countAsFlow()

    suspend fun isEmpty(): Boolean = dao.count() == 0

    suspend fun randomByCategory(tag: String): RegistryStation? = dao.randomStationByTag(tag)

    suspend fun syncFromAssets(context: Context) {
        val json = try {
            context.assets.open("registry.json.gz").use { input -> input.readBytes().readRegistryJson() }
        } catch (e: Exception) {
            return
        }
        val stations = parseRegistry(json)
        if (stations.isEmpty()) return
        dao.clearAndInsertAll(stations)
    }

    suspend fun syncFromNetwork(): List<RegistryStation>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(REGISTRY_URL).build()
            val json = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                body.bytes().readRegistryJson()
            }
            val stations = parseRegistry(json)
            if (stations.isEmpty()) return@withContext null
            dao.clearAndInsertAll(stations)
            stations
        } catch (e: Exception) {
            null
        }
    }

    suspend fun availableCountryCodes(): List<String> = dao.distinctCountryCodes()

    suspend fun availableTags(): List<String> {
        val counts = mutableMapOf<String, Int>()
        dao.tagRows().forEach { row ->
            row.split(" ")
                .map { it.trim() }
                .filter { it.length >= 3 }
                .forEach { tag ->
                    val label = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    counts[label] = (counts[label] ?: 0) + 1
                }
        }
        val curated = CURATED_TAG_ORDER.filter { counts.containsKey(it) }
        val discovered = counts.keys
            .filterNot { it in curated }
            .sortedWith(compareByDescending<String> { counts[it] ?: 0 }.thenBy { it })
        return curated + discovered
    }

    suspend fun search(
        query: String,
        countryCodes: Set<String> = emptySet(),
        tags: Set<String> = emptySet(),
    ): List<RegistryStation> {
        val hasQuery = query.isNotBlank()
        val hasFilter = countryCodes.isNotEmpty() || tags.isNotEmpty()
        if (!hasQuery && !hasFilter) return emptyList()

        val candidates = if (hasQuery) {
            val normalized = NumberNormalizer.normalize(query.trim())
            val words = normalized.lowercase().split(" ").filter { it.isNotBlank() }
            val raw = dao.search(normalized)
            if (words.size > 1) {
                raw.filter { station ->
                    val haystack = " ${station.searchText.lowercase()} ${station.country.lowercase()} ${station.countryCode.lowercase()} "
                    words.all { word -> haystack.contains(" $word ") }
                }
            } else raw
        } else if (countryCodes.isNotEmpty() && tags.isEmpty()) {
            dao.filterByCountryCodes(countryCodes.map { it.lowercase() })
        } else {
            dao.all()
        }

        return candidates.filter { station ->
            val countryMatch = countryCodes.isEmpty() ||
                countryCodes.any { it.equals(station.countryCode, ignoreCase = true) }
            val stationTagWords = station.tags.lowercase().split(" ")
            val tagMatch = tags.isEmpty() ||
                tags.any { stationTagWords.contains(it.lowercase()) }
            countryMatch && tagMatch
        }
    }

    suspend fun featuredStations(): List<RegistryStation> {
        val all = dao.getByProviderIds(FEATURED_PROVIDER_IDS)
        return FEATURED_PROVIDER_IDS.mapNotNull { id -> all.find { it.providerId == id } }
    }

    private fun parseRegistry(json: String): List<RegistryStation> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val name = obj.optString("name").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val streamUrl = obj.optString("stream_url").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val tags = obj.optJSONArray("tags")?.let { tagsArr ->
                    (0 until tagsArr.length()).mapNotNull { tagsArr.optString(it).trim().takeIf { s -> s.isNotBlank() } }.joinToString(" ")
                } ?: ""
                RegistryStation(
                    name = name,
                    streamUrl = streamUrl,
                    logoUrl = obj.optString("logo_url").trim(),
                    country = obj.optString("country").trim(),
                    countryCode = obj.optString("country_code").trim(),
                    tags = tags,
                    provider = obj.optString("provider").trim(),
                    providerId = obj.optString("provider_id").trim(),
                    searchText = NumberNormalizer.normalize("$name $tags"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun ByteArray.readRegistryJson(): String {
        val isGzip = size >= 2 && this[0] == 0x1f.toByte() && this[1] == 0x8b.toByte()
        return if (isGzip) {
            GZIPInputStream(ByteArrayInputStream(this)).bufferedReader().use { it.readText() }
        } else {
            toString(Charsets.UTF_8)
        }
    }
}
