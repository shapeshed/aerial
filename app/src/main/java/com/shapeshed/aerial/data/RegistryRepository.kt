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
        return dao.search(query.trim())
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
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
