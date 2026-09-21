package com.shapeshed.aerial.data

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerControllerTest {
    @Test
    fun nonPositiveDurationCancelsWithoutStarting() = runTest {
        val published = mutableListOf<SleepTimerState?>()
        var paused = false
        val controller = controller(published = published, pause = { paused = true })

        controller.start(0)
        runCurrent()

        assertFalse(controller.isRunning)
        assertFalse(paused)
        assertEquals(listOf<SleepTimerState?>(null), published)
    }

    @Test
    fun startPublishesTicksThenFadesAndPauses() = runTest {
        val published = mutableListOf<SleepTimerState?>()
        val volumes = mutableListOf<Float>()
        var volume = 1f
        var paused = false
        val controller = controller(
            published = published,
            readVolume = { volume },
            writeVolume = {
                volume = it
                volumes += it
            },
            pause = { paused = true },
        )

        controller.start(5_000)
        advanceUntilIdle()

        assertEquals(SleepTimerState(totalMs = 5_000, remainingMs = 5_000), published.first())
        assertEquals(null, published.last())
        assertTrue(paused)
        assertEquals(1f, volume)
        assertTrue(volumes.contains(0f))
        assertFalse(controller.isRunning)
    }

    @Test
    fun cancelStopsTheCountdownAndResetsVolume() = runTest {
        val published = mutableListOf<SleepTimerState?>()
        var volume = 0.4f
        var paused = false
        val controller = controller(
            published = published,
            readVolume = { volume },
            writeVolume = { volume = it },
            pause = { paused = true },
        )

        controller.start(60_000)
        runCurrent()
        assertTrue(controller.isRunning)

        controller.cancel()

        assertFalse(controller.isRunning)
        assertEquals(1f, volume)
        assertFalse(paused)
        assertEquals(null, published.last())
    }

    private fun TestScope.controller(
        published: MutableList<SleepTimerState?>,
        readVolume: () -> Float = { 1f },
        writeVolume: (Float) -> Unit = {},
        pause: () -> Unit = {},
    ) = SleepTimerController(
        scope = this,
        nowMs = { testScheduler.currentTime },
        readVolume = readVolume,
        writeVolume = writeVolume,
        pause = pause,
        publishState = { published += it },
    )
}
