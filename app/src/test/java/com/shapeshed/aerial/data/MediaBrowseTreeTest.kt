package com.shapeshed.aerial.data

import android.content.Context
import com.shapeshed.aerial.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MediaBrowseTreeTest {

    @Test
    fun rootChildrenAreFavoritesMoodsAndRecentFolders() {
        val tree = tree()

        val root = tree.rootChildren()

        assertEquals(
            listOf("favorites", "moods", "recent"),
            root.map { it.mediaId },
        )
        root.forEach {
            assertTrue(it.mediaMetadata.isBrowsable == true)
            assertTrue(it.mediaMetadata.isPlayable == false)
        }
    }

    @Test
    fun moodChildrenAreTheSixCuratedMoodFolders() {
        val tree = tree()

        val moods = tree.moodChildren()

        assertEquals(
            listOf("mood:relax", "mood:focus", "mood:morning", "mood:driving", "mood:late_night", "mood:workout"),
            moods.map { it.mediaId },
        )
        moods.forEach { assertTrue(it.mediaMetadata.isBrowsable == true) }
    }

    @Test
    fun favoriteChildrenMapsSavedStationsToPlayableItems() = runBlocking {
        val stationDao = FakeStationDao(station(id = 5L, name = "Kool FM"))
        val tree = tree(stationRepository = StationRepository(stationDao, FakePlayHistoryDao()))

        val children = tree.favoriteChildren()

        assertEquals(listOf("5"), children.map { it.mediaId })
        assertEquals("Kool FM", children.single().mediaMetadata.title.toString())
        assertTrue(children.single().mediaMetadata.isPlayable == true)
    }

    @Test
    fun recentChildrenOrdersByMostRecentPlayRegardlessOfFavoriteStatus() = runBlocking {
        val playHistoryDao = FakePlayHistoryDao(
            PlayHistoryEntry(provider = "radio-browser", providerId = "earlier", playedAt = 1_000L),
            PlayHistoryEntry(provider = "radio-browser", providerId = "recent", playedAt = 2_000L),
        )
        val registryDao = FakeRegistryDao(
            registryStation(id = 1L, name = "Played earlier", provider = "radio-browser", providerId = "earlier"),
            registryStation(id = 2L, name = "Played most recently", provider = "radio-browser", providerId = "recent"),
        )
        val tree = tree(
            stationRepository = StationRepository(FakeStationDao(), playHistoryDao),
            registryRepository = RegistryRepository(registryDao),
        )

        val children = tree.recentChildren()

        assertEquals(listOf("reg:2", "reg:1"), children.map { it.mediaId })
    }

    @Test
    fun recentChildrenSkipsEntriesNoLongerInTheRegistry() = runBlocking {
        val playHistoryDao = FakePlayHistoryDao(
            PlayHistoryEntry(provider = "radio-browser", providerId = "vanished", playedAt = 1_000L),
        )
        val tree = tree(stationRepository = StationRepository(FakeStationDao(), playHistoryDao))

        val children = tree.recentChildren()

        assertEquals(emptyList<String>(), children.map { it.mediaId })
    }

    @Test
    fun moodStationChildrenResolvesCuratedRegistryStations() = runBlocking {
        val registryDao = FakeRegistryDao(
            registryStation(
                id = 101L,
                name = "Café del Mar",
                provider = "radio-browser",
                providerId = "3fd18c3f-8157-11e9-aa30-52543be04c81",
            ),
        )
        val tree = tree(registryRepository = RegistryRepository(registryDao))

        val children = tree.moodStationChildren("relax")

        val cafeDelMar = children.firstOrNull { it.mediaId == "reg:101" }
        assertNotNull(cafeDelMar)
        assertEquals("Café del Mar", cafeDelMar!!.mediaMetadata.title.toString())
    }

    @Test
    fun searchDelegatesToRegistryCatalogueSearch() = runBlocking {
        val registryDao = FakeRegistryDao(
            registryStation(id = 42L, name = "Dogglounge", searchText = "dogglounge house"),
        )
        val tree = tree(registryRepository = RegistryRepository(registryDao))

        val results = tree.search("dogglounge")

        assertEquals(listOf("reg:42"), results.map { it.mediaId })
    }

    @Test
    fun resolveLooksUpLocalStationById() = runBlocking {
        val stationDao = FakeStationDao(station(id = 9L, name = "Rinse FM"))
        val tree = tree(stationRepository = StationRepository(stationDao, FakePlayHistoryDao()))

        val resolved = tree.resolve("9")

        assertEquals("9", resolved?.mediaId)
        assertEquals("Rinse FM", resolved?.mediaMetadata?.title.toString())
    }

    @Test
    fun resolveLooksUpRegistryStationByPrefixedId() = runBlocking {
        val registryDao = FakeRegistryDao(registryStation(id = 55L, name = "NTS Radio"))
        val tree = tree(registryRepository = RegistryRepository(registryDao))

        val resolved = tree.resolve("reg:55")

        assertEquals("reg:55", resolved?.mediaId)
        assertEquals("NTS Radio", resolved?.mediaMetadata?.title.toString())
    }

    @Test
    fun resolveReturnsNullForUnknownId() = runBlocking {
        val tree = tree()

        assertNull(tree.resolve("404"))
        assertNull(tree.resolve("reg:404"))
        assertNull(tree.resolve("not-a-number"))
    }

    @Test
    fun childrenDispatchesToTheRightFolderAndIsNullForUnknownIds() = runBlocking {
        val tree = tree()

        assertEquals(tree.rootChildren().map { it.mediaId }, tree.children(MEDIA_BROWSE_ROOT_ID)?.map { it.mediaId })
        assertEquals(tree.moodChildren().map { it.mediaId }, tree.children("moods")?.map { it.mediaId })
        assertNotNull(tree.children("favorites"))
        assertNotNull(tree.children("recent"))
        assertNotNull(tree.children("mood:relax"))
        assertNull(tree.children("unknown"))
    }

    private fun tree(
        stationRepository: StationRepository = StationRepository(FakeStationDao(), FakePlayHistoryDao()),
        registryRepository: RegistryRepository = RegistryRepository(FakeRegistryDao()),
    ): MediaBrowseTree = MediaBrowseTree(fakeContext(), stationRepository, registryRepository)

    private fun fakeContext(): Context {
        val context = mock<Context>()
        // Folder titles and the "Live Radio" fallback text come from string resources; station
        // names in tests below come straight from station data, not from these ids, so a stable
        // placeholder per resource id is enough to verify the right resource was requested.
        whenever(context.getString(any())).thenAnswer { invocation -> "string-${invocation.arguments[0]}" }
        return context
    }

    private fun station(
        id: Long = 0,
        name: String = "Mango",
        streamUrl: String = "https://stream.example.com/mango",
        lastPlayedAt: Long = 0,
    ) = Station(id = id, name = name, streamUrl = streamUrl, lastPlayedAt = lastPlayedAt)

    private fun registryStation(
        id: Long = 0,
        name: String = "Mango",
        streamUrl: String = "https://stream.example.com/mango",
        provider: String = "",
        providerId: String = "",
        searchText: String = name.lowercase(),
    ) = RegistryStation(
        id = id,
        name = name,
        streamUrl = streamUrl,
        provider = provider,
        providerId = providerId,
        searchText = searchText,
    )

    private class FakeStationDao(vararg initialStations: Station) : StationDao {
        val stations = initialStations.toMutableList()

        override fun getAll(): Flow<List<Station>> = flowOf(stations)
        override suspend fun getById(id: Long): Station? = stations.firstOrNull { it.id == id }
        override suspend fun getByStreamUrl(streamUrl: String): Station? =
            stations.firstOrNull { it.streamUrl == streamUrl }
        override suspend fun getByProviderId(provider: String, providerId: String): Station? =
            stations.firstOrNull { it.provider == provider && it.providerId == providerId }
        override suspend fun searchStationFts(match: String): List<Station> = emptyList()
        override suspend fun updateStreamUrlByProviderId(provider: String, providerId: String, streamUrl: String) {}
        override suspend fun recordPlay(id: Long, playedAt: Long) {}
        override suspend fun insert(station: Station): Long = 0
        override suspend fun update(station: Station) {}
        override suspend fun delete(station: Station) {}
    }

    private class FakeRegistryDao(vararg initialStations: RegistryStation) : RegistryDao() {
        val stations = initialStations.toMutableList()

        override suspend fun searchFts(match: String): List<RegistryStation> {
            val terms = match.split(" ").map { it.removeSuffix("*").lowercase() }
            return stations
                .filter { station ->
                    val text = "${station.searchText} ${station.description} ${station.country}".lowercase()
                    terms.all { text.contains(it) }
                }
                .sortedBy { it.name.lowercase() }
        }

        override fun countAsFlow(): Flow<Int> = flowOf(stations.size)
        override suspend fun count(): Int = stations.size
        override suspend fun randomStationByTag(tag: String): RegistryStation? =
            stations.firstOrNull { it.tags.contains(tag, ignoreCase = true) }
        override suspend fun randomByCountryWithLogo(countryCode: String, limit: Int): List<RegistryStation> =
            stations.filter { it.countryCode.equals(countryCode, ignoreCase = true) && it.logoUrl.isNotEmpty() }
                .take(limit)
        override suspend fun getByProviderIds(ids: List<String>): List<RegistryStation> =
            stations.filter { it.providerId in ids }
        override suspend fun getByProviderId(provider: String, providerId: String): RegistryStation? =
            stations.firstOrNull { it.provider == provider && it.providerId == providerId }
        override suspend fun getById(id: Long): RegistryStation? = stations.firstOrNull { it.id == id }
        override suspend fun getByNames(names: List<String>): List<RegistryStation> =
            stations.filter { it.name in names }
        override suspend fun distinctCountryCodes(): List<String> =
            stations.map { it.countryCode }.filter { it.isNotEmpty() }.distinct().sorted()
        override suspend fun tagRows(): List<String> = stations.map { it.tags }.filter { it.isNotEmpty() }
        override suspend fun filterByCountryCodes(countryCodes: List<String>): List<RegistryStation> =
            stations.filter { it.countryCode.lowercase() in countryCodes }.sortedBy { it.name }
        override suspend fun byTagLike(tag: String): List<RegistryStation> =
            stations.filter { it.tags.contains(tag, ignoreCase = true) }
        override suspend fun all(): List<RegistryStation> = stations.sortedBy { it.name }
        override suspend fun browse(limit: Int): List<RegistryStation> = stations.sortedBy { it.name }.take(limit)
    }

    private class FakePlayHistoryDao(vararg initialEntries: PlayHistoryEntry) : PlayHistoryDao {
        val entries = initialEntries.toMutableList()

        override suspend fun recordPlay(entry: PlayHistoryEntry) {
            entries.removeAll { it.provider == entry.provider && it.providerId == entry.providerId }
            entries += entry
        }

        override suspend fun recent(limit: Int): List<PlayHistoryEntry> =
            entries.sortedByDescending { it.playedAt }.take(limit)

        override fun recentAsFlow(limit: Int): Flow<List<PlayHistoryEntry>> =
            flowOf(entries.sortedByDescending { it.playedAt }.take(limit))
    }
}
