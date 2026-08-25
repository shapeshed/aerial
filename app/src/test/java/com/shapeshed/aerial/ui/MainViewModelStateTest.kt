package com.shapeshed.aerial.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import android.os.Bundle
import java.io.File
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.PlayHistoryEntry
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelStateTest {
    private val mainDispatcher = kotlinx.coroutines.test.StandardTestDispatcher()
    private val viewModels = mutableListOf<MainViewModel>()

    @Before
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @After
    fun tearDown() {
        val clear = ViewModel::class.java.getDeclaredMethod("clear\$lifecycle_viewmodel")
        viewModels.forEach { clear.invoke(it) }
        Dispatchers.resetMain()
    }

    @Test
    fun playbackFailuresMapToLocalizedMessageResources() {
        assertEquals(
            R.string.playback_connection_failed,
            playbackErrorMessageRes(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED),
        )
        assertEquals(
            R.string.playback_format_unsupported,
            playbackErrorMessageRes(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED),
        )
        assertEquals(
            R.string.playback_failed,
            playbackErrorMessageRes(PlaybackException.ERROR_CODE_UNSPECIFIED),
        )
    }

    @Test
    fun searchResultsFlowFromRegistryAndFavoritesRepositories() = runTest {
        val station = station(1, "Mango Radio")
        val registryStation = registry("Mango Radio")
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        whenever(repository.searchFavorites("mango")).thenReturn(listOf(station))
        whenever(registryRepository.search("mango", emptySet(), emptySet())).thenReturn(listOf(registryStation))
        val viewModel = viewModel(repository, registryRepository)

        viewModel.searchRegistry("mango")
        runCurrent()

        assertEquals(listOf(station), viewModel.favoriteSearchResults.first { it.isNotEmpty() })
        assertEquals(listOf(registryStation), viewModel.registrySearchResults.first { it.isNotEmpty() })
    }


    @Test
    fun countryFilterReissuesSearchAndPropagatesSelectedCountry() = runTest {
        val registryStation = registry("Mango Radio")
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        whenever(registryRepository.search("mango", emptySet(), emptySet())).thenReturn(emptyList())
        whenever(registryRepository.search("mango", setOf("GB"), emptySet())).thenReturn(listOf(registryStation))
        val viewModel = viewModel(repository, registryRepository)

        viewModel.searchRegistry("mango")
        viewModel.setCountryFilter("GB")

        assertEquals(setOf("GB"), viewModel.selectedCountries.first { it.isNotEmpty() })
        assertEquals(listOf(registryStation), viewModel.registrySearchResults.first { it.isNotEmpty() })
    }

    @Test
    fun tagFilterPropagatesAndClearAllFiltersRestoresUnfilteredSearch() = runTest {
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        whenever(registryRepository.search(any(), any(), any())).thenReturn(emptyList())
        val viewModel = viewModel(repository, registryRepository)

        viewModel.searchRegistry("mango")
        viewModel.toggleTagFilter("rock")
        assertEquals(setOf("rock"), viewModel.selectedTags.first { it.isNotEmpty() })

        viewModel.clearAllFilters()
        runCurrent()
        assertEquals(emptySet<String>(), viewModel.selectedCountries.value)
        assertEquals(emptySet<String>(), viewModel.selectedTags.value)
        verify(registryRepository, atLeastOnce()).search("mango", emptySet(), emptySet())
    }

    @Test
    fun individualFilterClearActionsPropagateAndReissueSearch() = runTest {
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        whenever(registryRepository.search(any(), any(), any())).thenReturn(emptyList())
        val viewModel = viewModel(repository, registryRepository)

        viewModel.searchRegistry("mango")
        viewModel.setCountryFilter("GB")
        viewModel.toggleTagFilter("news")
        runCurrent()

        viewModel.clearCountryFilter()
        viewModel.clearTagFilter()
        runCurrent()

        assertEquals(emptySet<String>(), viewModel.selectedCountries.value)
        assertEquals(emptySet<String>(), viewModel.selectedTags.value)
        verify(registryRepository, atLeastOnce()).search("mango", emptySet(), emptySet())
    }

    @Test
    fun metadataBeforeStationTransitionRemainsVisibleForNowPlaying() = runTest {
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        val left = station(1, "Metadata-less station")
        val right = station(2, "Song station")
        whenever(repository.getAll()).thenReturn(flowOf(listOf(left, right)))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, registryRepository)
        backgroundScope.launch { viewModel.nowPlayingDisplay.collect {} }
        runCurrent()

        viewModel.handlePlaybackEvents(mediaItem(left.name, left.id.toString()), isPlaying = true)
        assertEquals("Metadata-less station", viewModel.playbackUiState.value.station?.name)
        viewModel.handlePlaybackMetadata(
            mediaItem = mediaItem(right.name, right.id.toString()),
            title = "Song delivered by stream",
            artist = "Artist",
        )
        viewModel.handlePlaybackEvents(mediaItem(right.name, right.id.toString()), isPlaying = true)
        runCurrent()

        assertEquals("Song station", viewModel.playbackUiState.value.station?.name)
        assertEquals("Song delivered by stream", viewModel.playbackUiState.value.trackTitle)
        assertEquals("Artist", viewModel.playbackUiState.value.trackArtist)
        assertEquals(
            "Artist — Song delivered by stream",
            viewModel.nowPlayingDisplay.value.subtitle,
        )
    }

    @Test
    fun previousStationLabelDeliveredAfterTransitionIsNotTrackMetadata() = runTest {
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        val left = station(1, "Station with metadata")
        val right = station(2, "Station without metadata")
        val queue = listOf(left, right)
        whenever(repository.getAll()).thenReturn(flowOf(queue))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, registryRepository)
        runCurrent()

        viewModel.handlePlaybackEvents(mediaItem(left.name, left.id.toString()), isPlaying = true, queue = queue)
        viewModel.handlePlaybackMetadata(title = "Song title", artist = "Artist")
        viewModel.handlePlaybackEvents(mediaItem(right.name, right.id.toString()), isPlaying = true, queue = queue)

        // Media3 may dispatch the previous item's static title after currentMediaItem has already
        // advanced. A station label must never become a transient ICY track for the new station.
        viewModel.handlePlaybackMetadata(
            mediaItem = mediaItem(right.name, right.id.toString()),
            title = left.name,
            artist = null,
        )

        assertEquals(null, viewModel.playbackUiState.value.trackTitle)
        assertEquals(null, viewModel.playbackUiState.value.trackArtist)
    }

    @Test
    fun recentSearchesAreTrimmedDeduplicatedAndNewestFirst() = runTest {
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())

        viewModel.saveRecentSearch("  mango  ")
        viewModel.saveRecentSearch("jazz")
        viewModel.saveRecentSearch("mango")
        runCurrent()

        assertEquals(listOf("mango", "jazz"), viewModel.recentSearches.first { it.size == 2 })
        viewModel.removeRecentSearch("mango")
        assertEquals(listOf("jazz"), viewModel.recentSearches.first { it == listOf("jazz") })
    }

    @Test
    fun recentSearchesAreLimitedToFiveEntries() = runTest {
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())

        (1..6).forEach { viewModel.saveRecentSearch("station-$it") }
        runCurrent()

        assertEquals(
            listOf("station-6", "station-5", "station-4", "station-3", "station-2"),
            viewModel.recentSearches.first { it.size == 5 },
        )
    }

    @Test
    fun changingSortPropagatesOrderedFavoritesState() = runTest {
        val stations = listOf(
            station(1, "Alpha", lastPlayedAt = 1_000, playCount = 8),
            station(2, "Bravo", lastPlayedAt = 3_000, playCount = 2),
        )
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(stations))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())
        runCurrent()

        viewModel.setFavoritesSort(FavoritesSort.LAST_PLAYED)
        runCurrent()
        assertEquals(listOf(2L, 1L), viewModel.stations.first { it.map(Station::id) == listOf(2L, 1L) }.map(Station::id))

        viewModel.setFavoritesSort(FavoritesSort.MOST_PLAYED)
        runCurrent()
        assertEquals(listOf(1L, 2L), viewModel.stations.first { it.map(Station::id) == listOf(1L, 2L) }.map(Station::id))
    }

    @Test
    fun favoritesStateDoesNotPutNonFavoritesIntoTheSkipQueue() = runTest {
        val favoriteAlpha = station(1, "Alpha")
        val nonFavoriteBravo = station(2, "Bravo").copy(isFavorite = false)
        val favoriteCharlie = station(3, "Charlie")
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(listOf(favoriteAlpha, nonFavoriteBravo, favoriteCharlie)))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())
        runCurrent()

        assertEquals(
            listOf(favoriteAlpha.id, favoriteCharlie.id),
            viewModel.stations.first { it.size == 2 }.map(Station::id),
        )
    }

    @Test
    fun customSvgPropagatesToFavoritesListingState() = runTest {
        val customSvg = File.createTempFile("aerial-custom-logo", ".svg")
        try {
            val saved = station(4, "BBC Radio 4").copy(
                logoPath = customSvg.absolutePath,
                provider = "test",
                providerId = "bbc-radio-4",
            )
            val registryStation = registry("BBC Radio 4").copy(
                providerId = saved.providerId,
                logoUrl = "https://registry.example.test/bbc-radio-4.svg",
            )
            val repository = mock<StationRepository>()
            val registryRepository = mock<RegistryRepository>()
            whenever(repository.getAll()).thenReturn(flowOf(listOf(saved)))
            whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
            whenever(registryRepository.getByProviderId(saved.provider, saved.providerId))
                .thenReturn(registryStation)
            val viewModel = viewModel(repository, registryRepository)

            assertEquals(
                customSvg.absolutePath,
                viewModel.stations.first { it.isNotEmpty() }.single().logoPath,
            )
            runCurrent()
            verify(repository, never()).update(any())
        } finally {
            customSvg.delete()
        }
    }

    @Test
    fun changingFavoritesSortUpdatesTheActivePlaybackQueue() = runTest {
        val alpha = station(1, "Alpha", lastPlayedAt = 1_000)
        val bravo = station(2, "Bravo", lastPlayedAt = 3_000)
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(listOf(alpha, bravo)))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())
        runCurrent()

        viewModel.handlePlaybackEvents(
            mediaItem(alpha.name, alpha.id.toString()),
            isPlaying = true,
            queue = listOf(alpha, bravo),
        )
        viewModel.setFavoritesSort(FavoritesSort.LAST_PLAYED)
        runCurrent()

        assertEquals(
            listOf(bravo.id, alpha.id),
            viewModel.playbackUiState.value.queue.map(Station::id),
        )
    }

    @Test
    fun switchingBackToAzKeepsNextNavigationInDisplayedOrder() = runTest {
        val zulu = station(1, "Zulu", lastPlayedAt = 3_000)
        val alpha = station(2, "Alpha", lastPlayedAt = 1_000)
        val mike = station(3, "Mike", lastPlayedAt = 2_000)
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(listOf(zulu, alpha, mike)))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())
        runCurrent()

        viewModel.handlePlaybackEvents(
            mediaItem(alpha.name, alpha.id.toString()),
            isPlaying = true,
            queue = listOf(zulu, alpha, mike),
        )
        viewModel.setFavoritesSort(FavoritesSort.AZ)
        runCurrent()

        val orderedQueue = viewModel.playbackUiState.value.queue
        assertEquals(listOf(alpha.id, mike.id, zulu.id), orderedQueue.map(Station::id))
        assertEquals(mike.id, orderedQueue.stationAtOffset(alpha, 1)?.id)
        assertEquals(zulu.id, orderedQueue.stationAtOffset(mike, 1)?.id)
    }

    @Test
    fun lastPlayedSortKeepsTheStationPlayedImmediatelyBeforeSortingAtTheFront() = runTest {
        val radio4 = station(1, "BBC Radio 4", lastPlayedAt = 2_000)
        val radio1Dance = station(2, "BBC Radio 1 Dance", lastPlayedAt = 1_000)
        val third = station(3, "Third Station", lastPlayedAt = 500)
        val queue = listOf(radio1Dance, radio4, third)
        val repository = mock<StationRepository>()
        // This models the collection lag between PlayerService recording a play and
        // Room's station Flow delivering the updated lastPlayedAt value.
        val rows = MutableStateFlow(listOf(radio4, radio1Dance, third))
        whenever(repository.getAll()).thenReturn(rows)
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())
        runCurrent()

        viewModel.play(radio4, queue)
        viewModel.play(radio1Dance, queue)
        rows.value = listOf(radio4, radio1Dance.copy(lastPlayedAt = 5_000), third)
        runCurrent()
        viewModel.setFavoritesSort(FavoritesSort.LAST_PLAYED)
        runCurrent()

        assertEquals(
            radio1Dance.id,
            viewModel.playbackUiState.value.queue.first().id,
        )
    }

    @Test
    fun playingFromAnActiveLastPlayedListReordersTheVisibleFavorites() = runTest {
        val worldwide = station(1, "Worldwide FM", lastPlayedAt = 4_000)
        val radio4 = station(2, "BBC Radio 4", lastPlayedAt = 3_000)
        val kool = station(3, "Kool FM", lastPlayedAt = 2_000)
        val radio1Dance = station(4, "BBC Radio 1 Dance", lastPlayedAt = 1_000)
        val stationRows = MutableStateFlow(listOf(worldwide, radio4, kool, radio1Dance))
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(stationRows)
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())
        runCurrent()
        viewModel.setFavoritesSort(FavoritesSort.LAST_PLAYED)
        runCurrent()
        val initialOrder = viewModel.stations.first { it.size == 4 }.map(Station::id)

        viewModel.play(worldwide, listOf(worldwide, radio4, kool, radio1Dance))
        viewModel.play(radio4, listOf(worldwide, radio4, kool, radio1Dance))
        stationRows.value = listOf(worldwide, radio4.copy(lastPlayedAt = 5_000), kool, radio1Dance)
        runCurrent()

        assertEquals(
            listOf(radio4.id, worldwide.id, kool.id, radio1Dance.id),
            viewModel.stations.value.map(Station::id),
        )
        assertEquals(
            listOf(radio4.id, worldwide.id, kool.id, radio1Dance.id),
            viewModel.playbackUiState.value.queue.map(Station::id),
        )
    }

    @Test
    fun lastPlayedOrderRemainsUpdatedWhenReturningToFavorites() = runTest {
        val worldwide = station(1, "Worldwide FM", lastPlayedAt = 4_000)
        val radio4 = station(2, "BBC Radio 4", lastPlayedAt = 3_000)
        val rows = MutableStateFlow(listOf(worldwide, radio4))
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(rows)
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())
        runCurrent()
        viewModel.setFavoritesSort(FavoritesSort.LAST_PLAYED)
        viewModel.play(worldwide, listOf(worldwide, radio4))
        rows.value = listOf(worldwide, radio4.copy(lastPlayedAt = 5_000))
        runCurrent()
        assertEquals(
            listOf(radio4.id, worldwide.id),
            viewModel.stations.first { it.size == 2 }.map(Station::id),
        )

        viewModel.setSelectedHomeTab(0)
        viewModel.setSelectedHomeTab(1)
        runCurrent()

        assertEquals(
            listOf(radio4.id, worldwide.id),
            viewModel.stations.first { it.size == 2 && it.first().id == radio4.id }.map(Station::id),
        )
    }

    @Test
    fun playingFromAnActiveMostPlayedListReordersTheVisibleFavorites() = runTest {
        val alpha = station(1, "Alpha", playCount = 1)
        val bravo = station(2, "Bravo", playCount = 2)
        val rows = MutableStateFlow(listOf(alpha, bravo))
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(rows)
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())
        runCurrent()

        viewModel.setFavoritesSort(FavoritesSort.MOST_PLAYED)
        viewModel.play(alpha, listOf(alpha, bravo))
        rows.value = listOf(alpha.copy(playCount = 3), bravo)
        runCurrent()

        assertEquals(
            listOf(alpha.id, bravo.id),
            viewModel.stations.first { it.size == 2 }.map(Station::id),
        )
        assertEquals(
            listOf(alpha.id, bravo.id),
            viewModel.playbackUiState.value.queue.map(Station::id),
        )
    }

    @Test
    fun recentlyPlayedFlowResolvesRegistryEntriesForHomepage() = runTest {
        val entry = PlayHistoryEntry("test", "mango", 5_000L)
        val registryStation = registry("Mango Radio")
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(listOf(entry)))
        whenever(registryRepository.getByProviderId("test", "mango")).thenReturn(registryStation)
        whenever(registryRepository.countAsFlow()).thenReturn(flowOf(0))
        val viewModel = viewModel(repository, registryRepository)

        runCurrent()
        assertEquals(listOf(registryStation), viewModel.recentlyPlayedStations.first { it.isNotEmpty() })
    }

    @Test
    fun recentlyPlayedStateUpdatesWhenHistoryFlowReorders() = runTest {
        val first = registry("First Station").copy(providerId = "first")
        val second = registry("Second Station").copy(providerId = "second")
        val history = MutableStateFlow(listOf(PlayHistoryEntry("test", "first", 1_000L)))
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(history)
        whenever(registryRepository.getByProviderId("test", "first")).thenReturn(first)
        whenever(registryRepository.getByProviderId("test", "second")).thenReturn(second)
        val viewModel = viewModel(repository, registryRepository)

        assertEquals(listOf(first), viewModel.recentlyPlayedStations.first { it == listOf(first) })
        history.value = listOf(
            PlayHistoryEntry("test", "second", 2_000L),
            PlayHistoryEntry("test", "first", 1_000L),
        )

        assertEquals(listOf(second, first), viewModel.recentlyPlayedStations.first { it.size == 2 })
    }

    @Test
    fun togglingSavedFavoriteOffDeletesItsRepositoryRow() = runTest {
        val saved = station(7, "Mango Radio")
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(listOf(saved)))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())

        viewModel.toggleFavorite(saved)
        runCurrent()

        verify(repository).delete(saved)
    }

    @Test
    fun togglingImportedStationOnUpdatesFavoriteFlag() = runTest {
        val imported = station(7, "Mango Radio").copy(isFavorite = false)
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(listOf(imported)))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = viewModel(repository, mock())

        viewModel.toggleFavorite(imported)
        runCurrent()

        verify(repository).update(imported.copy(isFavorite = true))
    }

    @Test
    fun removingRegistryStationFindsAndDeletesMatchingSavedRow() = runTest {
        val saved = station(7, "Mango Radio")
        val registryStation = registry("Mango Radio")
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(listOf(saved)))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        whenever(repository.findMatching(registryStation)).thenReturn(saved)
        val viewModel = viewModel(repository, registryRepository)

        viewModel.removeFromRegistry(registryStation)
        runCurrent()

        verify(repository).delete(saved)
    }

    @Test
    fun addingRegistryStationPersistsFavoriteAndUpdatesAddedState() = runTest {
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        whenever(repository.insertOrGetExisting(any())).thenReturn(42L)
        val viewModel = viewModel(repository, registryRepository)

        viewModel.addFromRegistry(registry("Mango Radio"))
        runCurrent()

        assertEquals(42L, viewModel.recentlyAddedStationId.first { it != null })
        verify(repository).insertOrGetExisting(any())
    }

    @Test
    fun addingRegistryStationUsesArtworkLoaderForRemoteLogo() = runTest {
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        whenever(repository.insertOrGetExisting(any())).thenReturn(42L)
        val artworkLoader = RecordingArtworkLoader("/tmp/aerial-logo.png")
        val viewModel = viewModel(repository, registryRepository, artworkLoader = artworkLoader)

        viewModel.addFromRegistry(registry("Mango Radio").copy(logoUrl = "https://example.test/mango.png"))
        viewModel.recentlyAddedStationId.first { it == 42L }

        assertEquals("https://example.test/mango.png", artworkLoader.url)
        verify(repository).insertOrGetExisting(argThat { logoPath == "/tmp/aerial-logo.png" })
    }

    private fun viewModel(
        repository: StationRepository,
        registryRepository: RegistryRepository,
        dataStore: DataStore<Preferences> = MemoryDataStore(),
        artworkLoader: ArtworkLoader = CoilArtworkLoader(mock()),
    ): MainViewModel {
        val app = mock<AerialApp>()
        val network = mock<NetworkMonitor>()
        whenever(app.networkMonitor).thenReturn(network)
        whenever(app.getString(R.string.live_radio)).thenReturn("test-live-radio")
        whenever(network.isOnline).thenReturn(MutableStateFlow(true).asStateFlow())
        whenever(registryRepository.countAsFlow()).thenReturn(flowOf(0))
        return MainViewModel(app, repository, registryRepository, dataStore, SavedStateHandle(), artworkLoader).also(viewModels::add)
    }

    private class RecordingArtworkLoader(private val path: String) : ArtworkLoader {
        var url: String? = null
        override suspend fun download(url: String, directory: File): String? {
            this.url = url
            return path
        }
    }

    private fun station(id: Long, name: String, lastPlayedAt: Long = 0, playCount: Int = 0) = Station(
        id = id,
        name = name,
        streamUrl = "https://example.test/$id",
        isFavorite = true,
        lastPlayedAt = lastPlayedAt,
        playCount = playCount,
    )

    private fun registry(name: String) = RegistryStation(
        name = name,
        streamUrl = "https://example.test/${name.lowercase().replace(' ', '-')}",
        provider = "test",
        providerId = "mango",
    )

    private fun mediaItem(stationName: String, id: String): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(stationName)
                .setExtras(Bundle().apply {
                    putString("streamUrl", "https://example.test/$id")
                    putString("stationName", stationName)
                })
                .build(),
        )
        .build()

    private class MemoryDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
