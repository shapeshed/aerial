package com.shapeshed.aerial.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

class RegistryRepository(private val dao: RegistryDao) {

    fun countAsFlow(): Flow<Int> = dao.countAsFlow()

    suspend fun randomStations(count: Int): List<RegistryStation> = dao.randomStations(count)

    suspend fun randomByCategory(tag: String): RegistryStation? = dao.randomStationByTag(tag)

    suspend fun sync(context: Context) {
        val json = try {
            context.assets.open("registry.json").bufferedReader().readText()
        } catch (e: Exception) {
            return
        }
        val stations = parseRegistry(json)
        if (stations.isEmpty()) return
        dao.clearAndInsertAll(stations)
    }

    suspend fun search(query: String): List<RegistryStation> {
        if (query.isBlank()) return emptyList()
        val normalized = NumberNormalizer.normalize(query.trim())
        val candidates = dao.search(normalized)
        val words = normalized.lowercase().split(" ").filter { it.isNotBlank() }
        if (words.size <= 1) return candidates
        // Multi-word query: require each word to match at a word boundary to avoid
        // "radio 1" matching "Absolute Radio 10s" via the substring "radio 1" in "radio 10s"
        return candidates.filter { station ->
            val haystack = " ${station.searchText.lowercase()} ${station.country.lowercase()} ${station.countryCode.lowercase()} "
            words.all { word -> haystack.contains(" $word ") }
        }
    }

    suspend fun popularTags(count: Int = 4): List<String> =
        dao.allTagStrings()
            .flatMap { it.split(" ") }
            .filter { it.length > 1 }
            .groupingBy { it.lowercase() }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(count)
            .map { it.key.replaceFirstChar { c -> c.uppercaseChar() } }

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
                    searchText = NumberNormalizer.normalize("$name $tags"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
