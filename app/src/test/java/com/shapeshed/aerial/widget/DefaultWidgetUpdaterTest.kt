package com.shapeshed.aerial.widget

import android.content.Context
import com.shapeshed.aerial.data.PlaybackSnapshotStore
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.testing.FakeSharedPreferences
import com.shapeshed.aerial.testing.MemoryDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Covers `DefaultWidgetUpdater`'s wiring: that a redraw is driven by the injected collaborators
 * and scheduled on the injected scope.
 *
 * The test stops at the first observable interaction — the repository read that opens
 * `updateAerialWidgets`. Under virtual time the call then parks inside the artwork timeout, which
 * is enough to count redraws without needing a live `AppWidgetManager`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWidgetUpdaterTest {

    private val debounceMs = 150L

    @Test
    fun aSingleRequestReadsFavouritesOnce() = runTest {
        val repository = repository()
        val updater = updater(repository, TestScope(StandardTestDispatcher(testScheduler)))

        updater.request()
        advanceTimeBy(debounceMs)
        runCurrent()

        verify(repository, times(1)).getAll()
    }

    @Test
    fun aBurstOfRequestsReadsFavouritesOnlyOnce() = runTest {
        val repository = repository()
        val updater = updater(repository, TestScope(StandardTestDispatcher(testScheduler)))

        repeat(5) { updater.request() }
        advanceTimeBy(debounceMs)
        runCurrent()

        verify(repository, times(1)).getAll()
    }

    @Test
    fun requestsSpacedBeyondTheWindowEachRedraw() = runTest {
        val repository = repository()
        val updater = updater(repository, TestScope(StandardTestDispatcher(testScheduler)))

        updater.request()
        advanceTimeBy(debounceMs)
        runCurrent()
        updater.request()
        advanceTimeBy(debounceMs)
        runCurrent()

        verify(repository, times(2)).getAll()
    }

    @Test
    fun aRequestMadeBeforeTheWindowElapsesIsDropped() = runTest {
        val repository = repository()
        val updater = updater(repository, TestScope(StandardTestDispatcher(testScheduler)))

        updater.request()
        advanceTimeBy(debounceMs - 1)
        runCurrent()
        updater.request()
        advanceTimeBy(debounceMs)
        runCurrent()

        verify(repository, times(1)).getAll()
    }

    @Test
    fun aFailedRedrawDoesNotEscapeIntoTheApplicationScope() = runTest {
        // A widget that cannot repaint must never take the process down; the updater owns that
        // guarantee, so a redraw that blows up still leaves the scope usable.
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenThrow(IllegalStateException("database gone"))
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val updater = updater(repository, scope)

        updater.request()
        advanceTimeBy(debounceMs)
        advanceUntilIdle()

        updater.request()
        advanceTimeBy(debounceMs)
        runCurrent()

        verify(repository, times(2)).getAll()
    }

    private fun updater(repository: StationRepository, scope: TestScope) = DefaultWidgetUpdater(
        context = context(),
        repository = repository,
        snapshotStore = PlaybackSnapshotStore(MemoryDataStore()),
        applicationScope = scope,
    )

    private fun repository(): StationRepository {
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(emptyList()))
        return repository
    }

    private fun context(): Context {
        val context = mock<Context>()
        whenever(context.getSharedPreferences(any(), any())).thenReturn(FakeSharedPreferences())
        return context
    }
}
