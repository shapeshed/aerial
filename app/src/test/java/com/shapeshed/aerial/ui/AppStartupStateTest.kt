package com.shapeshed.aerial.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.PlayHistoryEntry
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.testing.MemoryDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Startup gates the splash screen, so the outcome that matters most is the one where a source
 * never produces content. Before this was bounded, that left the user on a splash screen with no
 * way out.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupStateTest {

    private val mainDispatcher = StandardTestDispatcher()
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
    fun loadingIsTheOnlyStateThatHoldsTheSplashScreen() {
        assertFalse(AppStartupState.Loading.settled)
        assertTrue(AppStartupState.Ready.settled)
        assertTrue(AppStartupState.GaveUp(1_000L).settled)
    }

    @Test
    fun gaveUpRemembersHowLongItWaited() {
        assertEquals(
            STARTUP_CONTENT_TIMEOUT_MS,
            AppStartupState.GaveUp(STARTUP_CONTENT_TIMEOUT_MS).waitedMs,
        )
    }

    @Test
    fun startupIsReadyOnceStationsAndHistoryHaveBothResolved() = runTest {
        val viewModel = viewModel(stations = flowOf(emptyList()), history = flowOf(emptyList()))

        advanceUntilIdle()

        assertEquals(AppStartupState.Ready, viewModel.startupState.value)
        assertTrue(viewModel.isInitialized.value)
    }

    @Test
    fun startupGivesUpWhenHistoryCompletesWithoutEmitting() = runTest {
        // Completes with no emission, so the "first load" signal never fires. This is the case
        // that used to hang the splash screen indefinitely.
        val viewModel = viewModel(stations = flowOf(emptyList()), history = emptyFlow())

        advanceTimeBy(STARTUP_CONTENT_TIMEOUT_MS)
        runCurrent()

        assertGaveUp(viewModel)
    }

    @Test
    fun startupGivesUpWhenHistoryNeverEmitsAtAll() = runTest {
        val viewModel = viewModel(
            stations = flowOf(emptyList()),
            history = flow { awaitCancellation() },
        )

        advanceTimeBy(STARTUP_CONTENT_TIMEOUT_MS)
        runCurrent()

        assertGaveUp(viewModel)
    }

    @Test
    fun aFailingHistoryQueryStillLetsTheAppOpen() = runTest {
        val viewModel = viewModel(history = flow { throw IllegalStateException("db gone") })

        advanceUntilIdle()

        assertEquals(AppStartupState.Ready, viewModel.startupState.value)
        assertTrue(viewModel.isInitialized.value)
    }

    @Test
    fun aFailingStationQueryStillLetsTheAppOpen() = runTest {
        val viewModel = viewModel(stations = flow { throw IllegalStateException("db gone") })

        advanceUntilIdle()

        assertTrue(viewModel.isInitialized.value)
    }

    @Test
    fun aFailingQueryDoesNotTearDownTheRestOfTheViewModelScope() = runTest {
        // viewModelScope uses a SupervisorJob, so one failing collector must not cancel the
        // others. Startup settling proves the scope survived.
        val viewModel = viewModel(history = flow { throw IllegalStateException("db gone") })

        advanceUntilIdle()

        assertTrue(viewModel.isInitialized.value)
    }

    private fun assertGaveUp(viewModel: MainViewModel) {
        val state = viewModel.startupState.value
        assertTrue("expected GaveUp but was $state", state is AppStartupState.GaveUp)
        assertEquals(STARTUP_CONTENT_TIMEOUT_MS, (state as AppStartupState.GaveUp).waitedMs)
        assertTrue("the splash screen must still be dismissible", viewModel.isInitialized.value)
    }

    private fun viewModel(
        stations: Flow<List<Station>> = flowOf(emptyList()),
        history: Flow<List<PlayHistoryEntry>> = flowOf(emptyList()),
    ): MainViewModel {
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(stations)
        whenever(repository.recentlyPlayedAsFlow(any())).thenReturn(history)
        val registryRepository = mock<RegistryRepository>()
        whenever(registryRepository.countAsFlow()).thenReturn(flowOf(0))
        val network = mock<NetworkMonitor>()
        whenever(network.isOnline).thenReturn(MutableStateFlow(true).asStateFlow())
        return MainViewModel(
            application = mock<Application>(),
            repository = repository,
            registryRepository = registryRepository,
            dataStore = MemoryDataStore(),
            networkMonitor = network,
            strings = StringProvider { "test" },
            ioDispatcher = mainDispatcher,
        ).also(viewModels::add)
    }
}
