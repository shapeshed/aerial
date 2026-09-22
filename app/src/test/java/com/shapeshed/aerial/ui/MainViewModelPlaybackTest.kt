package com.shapeshed.aerial.ui

import android.app.Application
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.testing.MemoryDataStore
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
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

    private fun kotlinx.coroutines.test.TestScope.testDispatcher(): CoroutineDispatcher {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        return dispatcher
    }

    private fun deliverPlayerError(controller: MediaController) {
        val captor = argumentCaptor<Player.Listener>()
        verify(controller).addListener(captor.capture())
        captor.firstValue.onPlayerError(
            PlaybackException(
                "boom",
                null,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            ),
        )
    }

    private fun viewModel(controller: MediaController, ioDispatcher: CoroutineDispatcher): MainViewModel {
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        val network = mock<NetworkMonitor>()
        whenever(network.isOnline).thenReturn(MutableStateFlow(true).asStateFlow())
        whenever(registryRepository.countAsFlow()).thenReturn(flowOf(0))
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(flowOf(emptyList()))
        return MainViewModel(
            application = mock<Application>(),
            repository = repository,
            registryRepository = registryRepository,
            dataStore = MemoryDataStore(),
            networkMonitor = network,
            strings = StringProvider { "error" },
            artworkLoader = NoopArtworkLoader,
            ioDispatcher = ioDispatcher,
        ).also { it.attachController(controller) }
    }

    private fun station(id: Long = 1L) = Station(
        id = id,
        name = "Mango Radio",
        streamUrl = "https://example.test/$id",
        isFavorite = true,
    )

    private object NoopArtworkLoader : ArtworkLoader {
        override suspend fun download(url: String, directory: File): String? = null
    }
}
