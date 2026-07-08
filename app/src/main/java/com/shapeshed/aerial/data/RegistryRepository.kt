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
//
// displayName overrides the station's name for this mood list only (e.g. dropping a
// disambiguator like "(International)" that's useful in search but redundant in a themed
// list) — it does not touch the registry, so search results still show the real name.
private data class MoodStationRef(
    val name: String,
    val provider: String? = null,
    val providerId: String? = null,
    val displayName: String? = null,
)

private val CURATED_MOOD_STATIONS = mapOf(
    // Ordered so logo colours flow rather than clash: navy/blue -> white/orange -> red/gold ->
    // dark/pink -> monochrome, with Café del Mar fixed first as requested.
    "relax" to listOf(
        MoodStationRef("Café del Mar", "radio-browser", "3fd18c3f-8157-11e9-aa30-52543be04c81"),
        // Registry name is "ABC-Loungr" (an upstream typo) — this is now curated (see
        // stations.toml) so the corrected name and a real logo stick.
        MoodStationRef("ABC Lounge", "curated"),
        // Registry name is all-lowercase ("nordic lodge copenhagen"); displayName just fixes
        // capitalisation for this list.
        MoodStationRef(
            "nordic lodge copenhagen",
            "radio-browser",
            "2ee81587-dba9-4d68-82b3-a7b32aafc525",
            displayName = "Nordic Lodge Copenhagen",
        ),
        MoodStationRef("Bossa Jazz Brasil", "curated"),
        MoodStationRef("Radio Paradise Mellow Mix", "curated"),
        MoodStationRef("Skylab Radio", "radio-browser", "24273571-703e-4373-b715-d7e7680d7599"),
        MoodStationRef("OneLuvFM", "curated"),
        MoodStationRef("FIP", "curated"),
        MoodStationRef("Jazz Sakura (asia dream radio)", "radio-browser", "9766d47a-68a3-4a30-8aec-c026f1ec6020"),
        MoodStationRef("Radio Samui Online", "radio-browser", "86468748-3045-4b88-97ef-e6d266c901f5"),
    ),
    // Ordered so logo colours flow rather than clash: navy/black/grey -> teal/cream ->
    // green -> yellow -> orange, with freeCodeCamp Code Radio fixed first as requested.
    "focus" to listOf(
        MoodStationRef("freeCodeCamp Code Radio", "radio-browser", "60ede1ca-d7fa-4a36-a047-aec873b9be41"),
        MoodStationRef("Slow Focus | NTS", "radio-browser", "d5468df4-e6d0-11e9-a96c-52543be04c81"),
        MoodStationRef("Sheet Music | NTS", "radio-browser", "1e0ad463-0dbc-4913-b12d-7d0b7300882b"),
        MoodStationRef("Systrum Sistum - SSR1", "radio-browser", "37e6772a-5ab7-429d-84bc-fedc606cc8c4"),
        MoodStationRef("SomaFM Beat Blender", "radio-browser", "960eb232-0601-11e8-ae97-52543be04c81"),
        // No stable id in the "curated" bucket (providerId is always ""), so this one still
        // resolves by name.
        MoodStationRef("A Strangely Isolated Place", "curated"),
        MoodStationRef("Box Lofi Radio", "radio-browser", "a5213a32-d614-47bc-8d52-70a2b6eed8e1"),
        MoodStationRef("SomaFM Groove Salad", "radio-browser", "960cf833-0601-11e8-ae97-52543be04c81"),
        MoodStationRef("FluxFM Chillhop – Chill Beats and LoFi HipHop", "radio-browser", "58d3cce6-62b5-43f1-8f41-8d024998aabc"),
        MoodStationRef("Radio Swiss Jazz", "curated"),
    ),
    // Ordered so logo colours flow: dark blue/purple -> dark/orange -> teal/red -> lime green
    // -> white/blue -> white/black monochrome, with Dogglounge fixed first as requested.
    "morning" to listOf(
        MoodStationRef("Dogglounge", "radio-browser", "1c41c07b-b995-11e8-aaf2-52543be04c81"),
        MoodStationRef("D3EP Radio", "radio-browser", "d210ac34-0fee-4586-b9d6-24b84e2c0c4e"),
        MoodStationRef("Oroko Radio", "radio-browser", "7babd377-ed7c-4a63-9778-47b0fd94983b"),
        MoodStationRef("SomaFM Heavyweight Reggae", "radio-browser", "c5955cee-2cdf-40b2-8b5f-aa55bafddbef"),
        // Moved from workout — talky but upbeat, a better fit for a morning mix.
        MoodStationRef("Pure Ibiza Radio", "radio-browser", "26edc6b6-d221-4814-a6a9-0dd5d5d365d2"),
        // Not in the bundled registry snapshot yet — added as a new curated entry (see
        // stations.toml).
        MoodStationRef("Veneno", "curated"),
        MoodStationRef("Cafe Mambo Ibiza Radio", "radio-browser", "450a5177-1752-11ea-a620-52543be04c81"),
        MoodStationRef("Radio Raheem", "radio-browser", "a1c99f81-f8d1-4f6d-b9e3-714763a72b7d"),
        MoodStationRef("Soho Radio", "radio-browser", "b830f77b-cb54-431d-9bdb-0af9bc6af301"),
        MoodStationRef("The Lot Radio", "radio-browser", "434e9a4b-018a-4557-8ca1-8c328bb1e09d"),
    ),
    // "On The Road" — a broad, genre-mixed set rather than one style. Ordered so logo colours
    // flow: black/white -> white/blue -> white/orange -> gold -> red, with Radio alHara fixed
    // first as requested.
    "driving" to listOf(
        // Independent community station broadcasting from Bethlehem, Palestine.
        MoodStationRef("Radio alHara", "radio-browser", "d62fa52b-7c1c-492c-9861-cd2c0ec02f00"),
        MoodStationRef("Rinse FM", "rinse"),
        // Berlin-based but explicitly globally curated (guest DJs and shows from around the
        // world), for broader geographic range.
        MoodStationRef("Refuge Worldwide", "radio-browser", "edb81cbf-0645-4944-a376-554b8299ff27"),
        MoodStationRef("Worldwide FM", "radio-browser", "1651c32f-55d8-4429-995d-872ea0dcf520"),
        MoodStationRef("ZonaSalsa Radio", "radio-browser", "0957a107-50fc-435e-a402-ddfa5cdda777"),
        MoodStationRef("Dandelion Radio", "radio-browser", "961fe58c-0601-11e8-ae97-52543be04c81"),
        MoodStationRef("Radio Paradise Main Mix", "curated"),
        MoodStationRef("KEXP 90.3 Seattle", "curated"),
        MoodStationRef("Cashmere Radio", "radio-browser", "356a337d-4552-4b2d-bfdb-d381a6d85a9d"),
        MoodStationRef("Triple J", "abc", "TRIPLEJ"),
    ),
    // Ordered so logo colours flow: pale blue -> deep blue -> purple -> dark/orange ->
    // white/grey -> pink -> brown, with SomaFM Drone Zone fixed first as requested.
    "late_night" to listOf(
        MoodStationRef("SomaFM Drone Zone", "radio-browser", "960eb2e9-0601-11e8-ae97-52543be04c81"),
        MoodStationRef("Dinamo.fm Sleep", "curated"),
        // Registry name is "SomaFM - Deep Space One (128 kb/s AAC)"; displayName drops the
        // bitrate suffix.
        MoodStationRef(
            "SomaFM - Deep Space One (128 kb/s AAC)",
            "radio-browser",
            "0042e94c-55d6-4ff4-a3a7-46136e703424",
            displayName = "SomaFM Deep Space One",
        ),
        MoodStationRef("Cryosleep", "curated"),
        MoodStationRef("MyNoise Ocean Waves", "radio-browser", "b69fe610-1522-47b1-a784-e7025d11f884"),
        MoodStationRef("Nature Radio Rain", "radio-browser", "95277bca-2c9a-4c08-b2e7-0854e5793f8e"),
        // Registry name includes a bitrate suffix; displayName drops it for this list.
        MoodStationRef(
            "Ambient Sleeping Pill | 128 kbps",
            "radio-browser",
            "4b4d1308-9fdb-42a7-b92d-6370bc3284fe",
            displayName = "Ambient Sleeping Pill",
        ),
        // No stable id in the "curated" bucket (providerId is always ""), so these three
        // resolve by name. Given solid-colour placeholder logos (see stations.toml) for
        // visual consistency as a set.
        // Registry's radio-browser "White Noise Radio" points at a mount that now actually
        // serves Brown Noise Radio's audio — curated here with the correct stream instead.
        MoodStationRef("White Noise Radio", "curated"),
        MoodStationRef("Pink Noise Radio", "curated"),
        MoodStationRef("Brown Noise Radio", "curated"),
    ),
    // Ordered so logo colours flow: black -> dark navy/red -> pink -> magenta/purple ->
    // white/red -> grey, with BBC Radio 1 Dance fixed first as requested.
    "workout" to listOf(
        // Uses the international stream (bbc_radio_one_dance_int), not the UK-only feed.
        // displayName drops "(International)" here only — search still shows the real name.
        MoodStationRef(
            "BBC Radio 1 Dance (International)",
            "bbc",
            "bbc_radio_one_dance_int",
            displayName = "BBC Radio 1 Dance",
        ),
        // Replaced Pure Ibiza Radio, which was talky and a better fit for morning — moved
        // there instead.
        MoodStationRef("Liquid DnB", "radio-browser", "b8148b29-09d0-4aa1-8bfe-43d236260170"),
        MoodStationRef("Bassdrive", "radio-browser", "960cc332-0601-11e8-ae97-52543be04c81"),
        MoodStationRef("Technolovers - TECHNO", "radio-browser", "2100610c-13c2-4536-879f-6a88ccb07dc8"),
        MoodStationRef("Kool FM", "rinse", "kool"),
        MoodStationRef("54house.fm", "radio-browser", "a20e7f55-661e-4f4c-b87f-a087c64633f8"),
        // Independent LA station. Replaced LiSTNR - Festival Bangers (an SCA network
        // aggregator channel, not an independent station) and, before that, PulseEDM Dance
        // Music Radio, whose bundled logo was dead (404).
        MoodStationRef("ShoutDRIVE", "radio-browser", "b294bf46-7c1d-45ba-b852-71c5b26298ac"),
        // No stable id in the "curated" bucket (providerId is always ""), so this one still
        // resolves by name.
        MoodStationRef("Techno.FM", "curated"),
        MoodStationRef("Radio FG 98.2", "radio-browser", "4a1bbe28-0675-43bb-98dc-fae037b0b026"),
        // Replaced FIP Electro and Point Blank FM — both ran noticeably lower BPM than the
        // rest of this mix. Q-Dance (hardstyle) was tried here too but dropped for being too
        // cheesy for a workout mix; hard techno keeps the energy without the vibe.
        MoodStationRef("Hard Techno Radio", "radio-browser", "6eef88eb-396c-4393-86aa-1ba04ff91918"),
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
        val match = if (!target.providerId.isNullOrBlank()) {
            candidates.firstOrNull { it.provider == target.provider && it.providerId == target.providerId }
        } else {
            target.provider
                ?.let { provider -> candidates.firstOrNull { it.name == target.name && it.provider == provider } }
                ?: candidates.firstOrNull { it.name == target.name && it.provider == "curated" }
                ?: candidates.firstOrNull { it.name == target.name }
        }
        return match?.let { station -> target.displayName?.let { station.copy(name = it) } ?: station }
    }

    // A-Z subsection shown as browse suggestions before the user has typed anything.
    // Capped to match the search query limit.
    suspend fun defaultStations(limit: Int = 200): List<RegistryStation> = dao.browse(limit)
}
