package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerPolicyTest {
    @Test
    fun pollDelayIsCappedAtTheIntervalAndNeverNegative() {
        assertEquals(0L, SleepTimerPolicy.pollDelayMs(-5L))
        assertEquals(0L, SleepTimerPolicy.pollDelayMs(0L))
        assertEquals(500L, SleepTimerPolicy.pollDelayMs(500L))
        assertEquals(1_000L, SleepTimerPolicy.pollDelayMs(1_000L))
        assertEquals(1_000L, SleepTimerPolicy.pollDelayMs(30_000L))
    }

    @Test
    fun fadeStepsDownToZero() {
        assertEquals(listOf(0.75f, 0.5f, 0.25f, 0f), SleepTimerPolicy.fadeVolumes(startVolume = 1f, steps = 4))
    }

    @Test
    fun fadeScalesFromTheStartingVolume() {
        assertEquals(listOf(0.6f, 0.4f, 0.2f, 0f), SleepTimerPolicy.fadeVolumes(startVolume = 0.8f, steps = 4))
    }
}
