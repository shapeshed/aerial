package com.shapeshed.aerial.ui

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.PlaybackSnapshotStore
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.testing.MemoryDataStore
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
class MainViewModelPlaybackTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun playSelectsTheStationAndStartsPlayback() = runTest {
        val controller = mock<MediaController>()
        val viewModel = viewModel(controller, testDispatcher())

        viewModel.play(station())

        assertEquals(station(), viewModel.playbackUiState.value.station)

        advanceUntilIdle()

        verify(controller).prepare()
        verify(controller).play()
    }

    @Test
    fun togglePlaybackPausesWhenPlaying() = runTest {
        val controller = mock<MediaController>()
        whenever(controller.isPlaying).thenReturn(true)
        val viewModel = viewModel(controller, testDispatcher())

        viewModel.togglePlayback()

        verify(controller).pause()
    }

    @Test
    fun togglePlaybackClearsAnyErrorAndPlays() = runTest {
        val controller = mock<MediaController>()
        whenever(controller.isPlaying).thenReturn(false)
        val viewModel = viewModel(controller, testDispatcher())

        deliverPlayerError(controller)
        assertNotNull(viewModel.playbackUiState.value.error)

        viewModel.togglePlayback()

        assertNull(viewModel.playbackUiState.value.error)
        verify(controller).play()
    }

    @Test
    fun stopAndClearStopsTheControllerAndForgetsTheStation() = runTest {
        val controller = mock<MediaController>()
        val viewModel = viewModel(controller, testDispatcher())
        viewModel.play(station())
        assertEquals(station(), viewModel.playbackUiState.value.station)

        viewModel.stopAndClear()

        verify(controller).stop()
        verify(controller).clearMediaItems()
        assertNull(viewModel.playbackUiState.value.station)
    }

    @Test
    fun theSavedStationIsRestoredPaused() = runTest {
        val controller = mock<MediaController>()
        val dataStore = MemoryDataStore()
        PlaybackSnapshotStore(dataStore).write(station(1), listOf(station(1)))
        val viewModel = viewModel(controller, testDispatcher(), dataStore = dataStore)

        advanceUntilIdle()

        verify(controller).prepare()
        verify(controller).pause()
    }

    @Test
    fun tracksChangesUpdateTheDisplayedBitrate() = runTest {
        val controller = mock<MediaController>()
        val viewModel = viewModel(controller, testDispatcher())
        val listener = playerListener(controller)

        listener.onTracksChanged(audioTracks(bitrate = 128_000))

        assertEquals(128, viewModel.playbackUiState.value.bitrateKbps)
    }

    @Test
    fun mediaMetadataChangesUpdateTheTrackTitle() = runTest {
        val controller = mock<MediaController>()
        val viewModel = viewModel(controller, testDispatcher())
        val listener = playerListener(controller)

        listener.onMediaMetadataChanged(MediaMetadata.Builder().setTitle("Song").setArtist("Artist").build())

        assertEquals("Song", viewModel.playbackUiState.value.trackTitle)
    }

    @Test
    fun homeDiscoveryLoadsRegistryContent() = runTest {
        val controller = mock<MediaController>()
        val registry = mock<RegistryRepository>()
        val featured = registryStation("Featured")
        whenever(registry.featuredStations()).thenReturn(listOf(featured))
        whenever(registry.defaultStations()).thenReturn(listOf(registryStation("Default")))
        whenever(registry.curatedMoodStations()).thenReturn(mapOf("relax" to listOf(registryStation("Mood"))))
        whenever(registry.availableCountryCodes()).thenReturn(listOf("GB"))
        whenever(registry.availableTags()).thenReturn(listOf("Pop"))
        whenever(registry.forYouStations("GB")).thenReturn(listOf(registryStation("For You")))
        val viewModel = viewModel(
            controller,
            testDispatcher(),
            registryRepository = registry,
            registryCount = flowOf(1),
        )

        advanceUntilIdle()

        assertEquals(listOf(featured), viewModel.featuredStations.value)
        assertEquals(listOf("Default"), viewModel.defaultStations.value.map(RegistryStation::name))
        assertEquals(listOf("GB"), viewModel.availableCountries.value)
        assertEquals(listOf("Pop"), viewModel.allTags.value)
        assertEquals(listOf("For You"), viewModel.forYouStations.value.map(RegistryStation::name))
    }

    @Test
    fun favouritingAnEphemeralStationCachesItsLogoLocally() = runTest {
        val controller = mock<MediaController>()
        val repository = mock<StationRepository>()
        whenever(repository.saveAsFavorite(any())).thenReturn(42L)
        val application = mock<Application>()
        whenever(application.filesDir).thenReturn(Files.createTempDirectory("aerial-playback-test-").toFile())
        val viewModel = viewModel(
            controller,
            testDispatcher(),
            repository = repository,
            application = application,
            artworkLoader = FixedArtworkLoader("/cached/logo.png"),
        )

        viewModel.toggleFavorite(station(id = 0, logoPath = "https://example.test/logo.png"))
        advanceUntilIdle()

        verify(repository).saveAsFavorite(argThat { logoPath == "/cached/logo.png" })
    }

    private fun TestScope.testDispatcher(): CoroutineDispatcher {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        return dispatcher
    }

    private fun deliverPlayerError(controller: MediaController) {
        playerListener(controller).onPlayerError(
            PlaybackException(
                "boom",
                null,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            ),
        )
    }

    private fun playerListener(controller: MediaController): Player.Listener {
        val captor = argumentCaptor<Player.Listener>()
        verify(controller).addListener(captor.capture())
        return captor.firstValue
    }

    private fun audioTracks(bitrate: Int): Tracks {
        val format = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_AAC)
            .setAverageBitrate(bitrate)
            .setPeakBitrate(bitrate)
            .build()
        return Tracks(
            listOf(
                Tracks.Group(
                    TrackGroup(format),
                    false,
                    intArrayOf(C.FORMAT_HANDLED),
                    booleanArrayOf(true),
                ),
            ),
        )
    }

    private fun viewModel(
        controller: MediaController,
        ioDispatcher: CoroutineDispatcher,
        repository: StationRepository = mock(),
        registryRepository: RegistryRepository = mock(),
        registryCount: Flow<Int> = flowOf(0),
        dataStore: DataStore<Preferences> = MemoryDataStore(),
        artworkLoader: ArtworkLoader = NoopArtworkLoader,
        application: Application = mock(),
    ): MainViewModel {
        val network = mock<NetworkMonitor>()
        whenever(network.isOnline).thenReturn(MutableStateFlow(true).asStateFlow())
        whenever(registryRepository.countAsFlow()).thenReturn(registryCount)
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        return MainViewModel(
            application = application,
            repository = repository,
            registryRepository = registryRepository,
            dataStore = dataStore,
            networkMonitor = network,
            strings = StringProvider { id -> if (id == R.string.live_radio) "test-live-radio" else "error" },
            artworkLoader = artworkLoader,
            ioDispatcher = ioDispatcher,
        ).also { it.attachController(controller) }
    }

    private fun station(id: Long = 1L, logoPath: String = "") = Station(
        id = id,
        name = "Mango Radio",
        streamUrl = "https://example.test/$id",
        isFavorite = true,
        logoPath = logoPath,
    )

    private fun registryStation(name: String) = RegistryStation(
        name = name,
        streamUrl = "https://example.test/${name.lowercase()}",
        provider = "test",
        providerId = name.lowercase(),
    )

    private object NoopArtworkLoader : ArtworkLoader {
        override suspend fun download(url: String, directory: File): String? = null
    }

    private class FixedArtworkLoader(private val path: String) : ArtworkLoader {
        override suspend fun download(url: String, directory: File): String? = path
    }
}
