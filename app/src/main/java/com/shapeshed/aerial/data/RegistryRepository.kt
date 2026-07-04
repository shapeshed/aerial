package com.shapeshed.aerial.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

// provider_id is only unique within a single provider's own namespace — e.g. NRK and DR
// both use "p1"/"p2"/"p3" — so featured lookups must match on (provider, providerId)
// together, not providerId alone.
private data class FeaturedStation(val provider: String, val providerId: String)

private val FEATURED_STATIONS = listOf(
    FeaturedStation("bbc", "bbc_world_service"), // BBC World Service
    FeaturedStation("radio-france", "2"),        // franceinfo
    FeaturedStation("ard", "21818908"),          // Deutschlandfunk
    FeaturedStation("bauer", "ki1"),             // KISS
)

private val CURATED_TAG_ORDER = listOf(
    "News", "Sport", "Pop", "Rock", "Jazz", "Classical", "Dance", "Soul", "Country", "Electronic"
)

// Turn a normalised query into an FTS4 MATCH expression: split into tokens (which also drops FTS
// operator characters, avoiding syntax errors) and prefix-match each so search-as-you-type works.
// e.g. "radio jazz" -> "radio* jazz*". Accent folding is handled by the tokenizer, not here.
internal fun toFtsMatchQuery(normalized: String): String =
    normalized.lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { "$it*" }

class RegistryRepository(private val dao: RegistryDao) {

    fun countAsFlow(): Flow<Int> = dao.countAsFlow()

    suspend fun isEmpty(): Boolean = dao.count() == 0

    suspend fun randomByCategory(tag: String): RegistryStation? = dao.randomStationByTag(tag)

    suspend fun syncFromAssets(context: Context) {
        val json = try {
            val name = if ("registry.json.gz" in context.assets.list("").orEmpty()) "registry.json.gz" else "registry.json"
            context.assets.open(name).use { input -> input.readBytes().readRegistryJson() }
        } catch (e: Exception) {
            return
        }
        val stations = parseRegistry(json)
        if (stations.isEmpty()) return
        dao.clearAndInsertAll(stations)
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
            // Tokenised, accent-folded FTS4 match (see RegistryStationFts). Number-word
            // normalisation is applied first so "radio 1" still matches "Radio One".
            val match = toFtsMatchQuery(NumberNormalizer.normalize(query.trim()))
            if (match.isBlank()) emptyList() else dao.searchFts(match)
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
        val all = dao.getByProviderIds(FEATURED_STATIONS.map { it.providerId })
        return FEATURED_STATIONS.mapNotNull { featured ->
            all.find { it.provider == featured.provider && it.providerId == featured.providerId }
        }
    }

    // A-Z subsection shown as browse suggestions before the user has typed anything.
    // Capped to match the search query limit.
    suspend fun defaultStations(limit: Int = 200): List<RegistryStation> = dao.browse(limit)

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
                    // Optional: absent in the current registry, so it defaults to "".
                    description = obj.optString("description").trim(),
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
