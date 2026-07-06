package com.shapeshed.aerial.data

import kotlinx.coroutines.flow.Flow

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

private val UK_FOR_YOU_STATIONS = listOf(
    MoodStationRef("Smooth Radio"),
    MoodStationRef("Heart 80s", "global"),
    MoodStationRef("Heart UK", "global"),
    MoodStationRef("Capital FM"),
    MoodStationRef("Heart"),
    MoodStationRef("Greatest Hits Radio", "bauer"),
    MoodStationRef("BBC Radio 2", "bbc"),
    MoodStationRef("KISS", "bauer"),
    MoodStationRef("Absolute 80s", "bauer"),
    MoodStationRef("BBC Radio 1", "bbc"),
)

private val CURATED_TAG_ORDER = listOf(
    "News", "Sport", "Pop", "Rock", "Jazz", "Classical", "Dance", "Soul", "Country", "Electronic"
)

private data class MoodStationRef(val name: String, val provider: String? = null)

private val CURATED_MOOD_STATIONS = mapOf(
    "relax" to listOf(
        MoodStationRef("Café del Mar"),
        MoodStationRef("Radio Paradise Mellow Mix", "curated"),
        MoodStationRef("Soma FM Groove Salad", "curated"),
        MoodStationRef("FIP", "curated"),
        MoodStationRef("Bossa Jazz Brasil", "curated"),
        MoodStationRef("Radio Schizoid - Chillout / Ambient"),
        MoodStationRef("Jazz Sakura (asia dream radio)"),
        MoodStationRef("KLCBS Tropical"),
        MoodStationRef("Radio Samui Online"),
        MoodStationRef("Cape Town Classic"),
    ),
    "focus" to listOf(
        MoodStationRef("BBC Radio 3 Unwind", "bbc"),
        MoodStationRef("Classic FM Calm", "global"),
        MoodStationRef("Radio Schizoid - Chillout / Ambient"),
        MoodStationRef("Ottava", "curated"),
        MoodStationRef("KBS Classic FM"),
        MoodStationRef("Radio Swiss Classic", "curated"),
        MoodStationRef("MDR KLASSIK Livestream", "ard"),
        MoodStationRef("Ambient Sleeping Pill"),
        MoodStationRef("SomaFM Drone Zone"),
        MoodStationRef("WDR 3 Klassik", "ard"),
    ),
    "morning" to listOf(
        MoodStationRef("BBC Radio 6 Music", "bbc"),
        MoodStationRef("KEXP 90.3 Seattle", "curated"),
        MoodStationRef("FIP", "curated"),
        MoodStationRef("Radio Nova", "curated"),
        MoodStationRef("Antena 1 FM 94.7 São Paulo", "curated"),
        MoodStationRef("Shonan Beach FM 78.9", "curated"),
        MoodStationRef("Cool Fahrenheit", "curated"),
        MoodStationRef("ABC Jazz", "abc"),
        MoodStationRef("Worldwide FM"),
        MoodStationRef("1LIVE", "ard"),
    ),
    "driving" to listOf(
        MoodStationRef("Radio Paradise Main Mix", "curated"),
        MoodStationRef("BBC Radio 6 Music", "bbc"),
        MoodStationRef("KEXP 90.3 Seattle", "curated"),
        MoodStationRef("NTS Radio 1", "curated"),
        MoodStationRef("Radio Nova", "curated"),
        MoodStationRef("Triple J", "abc"),
        MoodStationRef("Rinse FM", "rinse"),
        MoodStationRef("KISS", "bauer"),
        MoodStationRef("1LIVE", "ard"),
        MoodStationRef("FIP", "curated"),
    ),
    "late_night" to listOf(
        MoodStationRef("NTS Radio 2", "curated"),
        MoodStationRef("SomaFM Drone Zone"),
        MoodStationRef("Nightwave Plaza", "curated"),
        MoodStationRef("Colombia Lounge"),
        MoodStationRef("Radio Schizoid - Chillout / Ambient"),
        MoodStationRef("Ambient Sleeping Pill"),
        MoodStationRef("FIP Jazz"),
        MoodStationRef("KLCBS New Age"),
        MoodStationRef("BBC Radio 3 Unwind", "bbc"),
        MoodStationRef("Radio Samui Online"),
    ),
    "workout" to listOf(
        MoodStationRef("BBC Radio 1 Dance", "bbc"),
        MoodStationRef("Capital Dance", "global"),
        MoodStationRef("Rinse FM", "rinse"),
        MoodStationRef("FIP Electro"),
        MoodStationRef("1LIVE DIGGI", "ard"),
        MoodStationRef("98.8 Kiss FM Clubsets", "curated"),
        MoodStationRef("Allzic Radio Dance Floor", "curated"),
        MoodStationRef("Bollywood Dance"),
        MoodStationRef("CLUB DANCE CHILE"),
        MoodStationRef("ABC Dance"),
    ),
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

    suspend fun forYouStations(countryCode: String): List<RegistryStation> {
        val refs = when (countryCode.uppercase()) {
            "GB", "UK" -> UK_FOR_YOU_STATIONS
            else -> return emptyList()
        }
        val candidates = dao.getByNames(refs.map { it.name })
        return refs.mapNotNull { resolveMoodStation(it, candidates) }
    }

    suspend fun curatedMoodStations(): Map<String, List<RegistryStation>> {
        val allRefs = CURATED_MOOD_STATIONS.values.flatten()
        val candidates = dao.getByNames(allRefs.map { it.name }.distinct())
        return CURATED_MOOD_STATIONS.mapValues { (_, refs) ->
            refs.mapNotNull { target ->
                resolveMoodStation(target, candidates)
            }
        }
    }

    private fun resolveMoodStation(
        target: MoodStationRef,
        candidates: List<RegistryStation>,
    ): RegistryStation? =
        target.provider
            ?.let { provider -> candidates.firstOrNull { it.name == target.name && it.provider == provider } }
            ?: candidates.firstOrNull { it.name == target.name && it.provider == "curated" }
            ?: candidates.firstOrNull { it.name == target.name }

    // A-Z subsection shown as browse suggestions before the user has typed anything.
    // Capped to match the search query limit.
    suspend fun defaultStations(limit: Int = 200): List<RegistryStation> = dao.browse(limit)
}
