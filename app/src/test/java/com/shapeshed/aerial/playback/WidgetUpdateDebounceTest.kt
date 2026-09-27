package com.shapeshed.aerial.widget

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the coalescing rule the widget depends on: one visible change produces one redraw even
 * when playback writes several pieces of state in quick succession.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetUpdateDebounceTest {

    private val debounceMs = 150L

    @Test
    fun singleRequestIsGrantedATurnAfterTheDebounceWindow() = runTest {
        val debounce = WidgetUpdateDebounce(debounceMs)

        val pending = async { debounce.awaitTurn() }
        advanceTimeBy(debounceMs - 1)
        assertTrue("must not publish before the window elapses", pending.isCompleted.not())
        runCurrent()

        assertNotNull(pending.await())
    }

    @Test
    fun burstOfRequestsGrantsExactlyOneTurn() = runTest {
        val debounce = WidgetUpdateDebounce(debounceMs)
        val turns = List(5) { async { debounce.awaitTurn() } }

        advanceTimeBy(debounceMs)
        val results = turns.map { it.await() }

        assertEquals(
            "only the newest of a burst may redraw",
            1,
            results.count { it != null },
        )
    }

    @Test
    fun supersededRequestIsDroppedEvenWhenItResumesFirst() = runTest {
        val debounce = WidgetUpdateDebounce(debounceMs)

        val first = async { debounce.awaitTurn() }
        advanceTimeBy(debounceMs / 2)
        val second = async { debounce.awaitTurn() }
        advanceTimeBy(debounceMs)

        assertNull("the superseded request must not redraw", first.await())
        assertNotNull("the newest request must redraw", second.await())
    }

    @Test
    fun requestsSpacedBeyondTheWindowEachRedraw() = runTest {
        val debounce = WidgetUpdateDebounce(debounceMs)

        val first = async { debounce.awaitTurn() }
        advanceTimeBy(debounceMs)
        assertNotNull(first.await())

        val second = async { debounce.awaitTurn() }
        advanceTimeBy(debounceMs)
        assertNotNull("a later, independent change must still redraw", second.await())
    }

    @Test
    fun isCurrentTracksTheNewestRequestForThePublishGuard() = runTest {
        val debounce = WidgetUpdateDebounce(debounceMs)

        val token = async { debounce.awaitTurn() }
        advanceTimeBy(debounceMs)
        val granted = token.await()
        assertNotNull(granted)
        assertTrue(debounce.isCurrent(granted!!))

        // A redraw that is superseded while it is building must not be published.
        val next = async { debounce.awaitTurn() }
        runCurrent()
        assertFalse(debounce.isCurrent(granted))
        advanceTimeBy(debounceMs)
        assertTrue(debounce.isCurrent(next.await()!!))
    }

    @Test
    fun debounceDoesNotBlockOnTheCallerScheduler() = runTest {
        // Guards against the debounce being wired to a dispatcher the test cannot advance: the
        // turn must be granted by virtual time, never by real time.
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val debounce = WidgetUpdateDebounce(debounceMs)

        val token = scope.async { debounce.awaitTurn() }
        testScheduler.advanceTimeBy(debounceMs)

        assertNotNull(token.await())
    }
}
