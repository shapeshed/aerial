package com.shapeshed.aerial.ui

import androidx.lifecycle.ViewModel
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StationEditViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun editWithoutCustomLogoFallsBackToRegistryArtwork() = runTest {
        val station = Station(
            id = 42,
            name = "Mango Radio",
            streamUrl = "https://stream.example/mango",
            provider = "radio-browser",
            providerId = "mango-42",
        )
        val registry = RegistryStation(
            name = station.name,
            streamUrl = station.streamUrl,
            provider = station.provider,
            providerId = station.providerId,
            logoUrl = "https://cdn.example/mango.svg",
        )
        val repository = mock<StationRepository>()
        val registryRepository = mock<RegistryRepository>()
        whenever(repository.getById(station.id)).thenReturn(station)
        whenever(registryRepository.getByProviderId(station.provider, station.providerId))
            .thenReturn(registry)

        val viewModel = StationEditViewModel(repository, registryRepository, station.id)
        runCurrent()

        assertEquals("https://cdn.example/mango.svg", viewModel.registryLogoUrl.first())
        assertEquals("", viewModel.logoPath.first())

        val clear = ViewModel::class.java.getDeclaredMethod("clear\$lifecycle_viewmodel")
        clear.invoke(viewModel)
    }

    @Test
    fun cancellingLogoImportRemainsCoroutineCancellation() = runTest {
        val viewModel = StationEditViewModel(
            mock(),
            mock(),
            null,
            logoImporter = { _, _ -> throw CancellationException("cancelled") },
        )

        val copyJob: Job = viewModel.onLogoPicked(mock(), mock())
        runCurrent()
        copyJob.join()

        assertTrue(copyJob.isCancelled)
        val clear = ViewModel::class.java.getDeclaredMethod("clear\$lifecycle_viewmodel")
        clear.invoke(viewModel)
    }
}
