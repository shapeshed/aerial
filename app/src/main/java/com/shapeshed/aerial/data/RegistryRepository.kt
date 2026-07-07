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

private const val FOR_YOU_RANDOM_COUNT = 10

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

// providerId pins a ref to one exact registry row so it survives a station being renamed
// upstream on a future registry refresh (name alone would silently stop resolving). Curated
// entries have no stable id (providerId is always "" in that bucket), so those refs still
// fall back to matching by name.
private data class MoodStationRef(val name: String, val provider: String? = null, val providerId: String? = null)

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
        MoodStationRef("freeCodeCamp Code Radio", "radio-browser", "60ede1ca-d7fa-4a36-a047-aec873b9be41"),
        MoodStationRef("Box Lofi Radio", "radio-browser", "a5213a32-d614-47bc-8d52-70a2b6eed8e1"),
        MoodStationRef("FluxFM Chillhop – Chill Beats and LoFi HipHop", "radio-browser", "58d3cce6-62b5-43f1-8f41-8d024998aabc"),
        MoodStationRef("SomaFM Groove Salad", "radio-browser", "960cf833-0601-11e8-ae97-52543be04c81"),
        MoodStationRef("SomaFM Beat Blender", "radio-browser", "960eb232-0601-11e8-ae97-52543be04c81"),
        // No stable id in the "curated" bucket (providerId is always ""), so this one still
        // resolves by name.
        MoodStationRef("A Strangely Isolated Place", "curated"),
        MoodStationRef("Systrum Sistum - SSR1", "radio-browser", "37e6772a-5ab7-429d-84bc-fedc606cc8c4"),
        MoodStationRef("Slow Focus | NTS", "radio-browser", "d5468df4-e6d0-11e9-a96c-52543be04c81"),
        MoodStationRef("Sheet Music | NTS", "radio-browser", "1e0ad463-0dbc-4913-b12d-7d0b7300882b"),
        MoodStationRef("Radio Swiss Jazz", "curated"),
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
        // International variant, not the UK-only feed.
        MoodStationRef("BBC Radio 1 Dance (International)", "bbc", "bbc_radio_one_dance_int"),
        MoodStationRef("Radio FG 98.2", "radio-browser", "4a1bbe28-0675-43bb-98dc-fae037b0b026"),
        MoodStationRef("54house.fm Clubstream", "radio-browser", "a20e7f55-661e-4f4c-b87f-a087c64633f8"),
        MoodStationRef("Technolovers - TECHNO", "radio-browser", "2100610c-13c2-4536-879f-6a88ccb07dc8"),
        // No stable id in the "curated" bucket (providerId is always ""), so this one still
        // resolves by name.
        MoodStationRef("Techno.FM", "curated"),
        MoodStationRef("Bassdrive", "radio-browser", "960cc332-0601-11e8-ae97-52543be04c81"),
        MoodStationRef("Kool FM", "rinse", "kool"),
        MoodStationRef("Pure Ibiza Radio", "radio-browser", "26edc6b6-d221-4814-a6a9-0dd5d5d365d2"),
        MoodStationRef("FIP Electro", "radio-browser", "ceba99fe-a1e5-4f2b-b9af-105d8eb7697d"),
        MoodStationRef("Point Blank FM", "radio-browser", "9957dc39-7c25-499c-8a9c-5ce871924316"),
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
            else -> emptyList()
        }
        if (refs.isNotEmpty()) {
            val candidates = dao.getByNames(refs.map { it.name })
            val curated = refs.mapNotNull { resolveMoodStation(it, candidates) }
            if (curated.isNotEmpty()) return curated
        }
        // No curated selection for this country: a random sample of its stations that
        // have artwork, so the row still feels local.
        return dao.randomByCountryWithLogo(countryCode, FOR_YOU_RANDOM_COUNT)
    }

    suspend fun curatedMoodStations(): Map<String, List<RegistryStation>> {
        val allRefs = CURATED_MOOD_STATIONS.values.flatten()
        val (idRefs, nameRefs) = allRefs.partition { !it.providerId.isNullOrBlank() }
        val candidates = dao.getByProviderIds(idRefs.map { it.providerId!! }.distinct()) +
            dao.getByNames(nameRefs.map { it.name }.distinct())
        return CURATED_MOOD_STATIONS.mapValues { (_, refs) ->
            refs.mapNotNull { target ->
                resolveMoodStation(target, candidates)
            }
        }
    }

    private fun resolveMoodStation(
        target: MoodStationRef,
        candidates: List<RegistryStation>,
    ): RegistryStation? {
        if (!target.providerId.isNullOrBlank()) {
            return candidates.firstOrNull { it.provider == target.provider && it.providerId == target.providerId }
        }
        return target.provider
            ?.let { provider -> candidates.firstOrNull { it.name == target.name && it.provider == provider } }
            ?: candidates.firstOrNull { it.name == target.name && it.provider == "curated" }
            ?: candidates.firstOrNull { it.name == target.name }
    }

    // A-Z subsection shown as browse suggestions before the user has typed anything.
    // Capped to match the search query limit.
    suspend fun defaultStations(limit: Int = 200): List<RegistryStation> = dao.browse(limit)
}
