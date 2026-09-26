package com.shapeshed.aerial.ui

import androidx.lifecycle.ViewModel
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import java.io.File
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
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

    @Test
    fun latestLogoImportWinsWhenAnOlderImportFinishesLast() = runTest {
        val firstImport = CompletableDeferred<File?>()
        val secondImport = CompletableDeferred<File?>()
        val firstStarted = CompletableDeferred<Unit>()
        var importCount = 0
        val firstFile = File("/tmp/aerial-first-logo.png")
        val secondFile = File("/tmp/aerial-second-logo.png")
        val viewModel = StationEditViewModel(
            repository = mock(),
            registryRepository = mock(),
            stationId = null,
            logoImporter = { _, _ ->
                if (importCount++ == 0) {
                    firstStarted.complete(Unit)
                    firstImport.await()
                } else {
                    secondImport.await()
                }
            },
        )

        val firstJob = viewModel.onLogoPicked(mock(), mock())
        firstStarted.await()
        val secondJob = viewModel.onLogoPicked(mock(), mock())

        secondImport.complete(secondFile)
        secondJob.join()
        firstImport.complete(firstFile)
        firstJob.join()

        assertEquals(secondFile.absolutePath, viewModel.logoPath.value)

        val clear = ViewModel::class.java.getDeclaredMethod("clear\$lifecycle_viewmodel")
        clear.invoke(viewModel)
    }

    @Test
    fun repeatedSaveInsertsOnlyOneStation() = runTest {
        val repository = mock<StationRepository>()
        val viewModel = StationEditViewModel(repository, mock(), stationId = null)
        viewModel.onNameChange("Mango Radio")
        viewModel.onStreamUrlChange("https://stream.example/mango")

        viewModel.save {}
        viewModel.save {}
        advanceUntilIdle()

        verify(repository, times(1)).insert(any())

        val clear = ViewModel::class.java.getDeclaredMethod("clear\$lifecycle_viewmodel")
        clear.invoke(viewModel)
    }
}
