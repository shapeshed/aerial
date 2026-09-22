package com.shapeshed.aerial

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerReconnectPolicyTest {
    private var nowMs = 100_000L
    private val throttle = ReconnectThrottle(cooldownMs = 10_000L) { nowMs }

    @Test
    fun firstAttemptIsAllowedEvenWithinTheCooldownOfBoot() {
        nowMs = 5_000L

        assertTrue(throttle.tryAcquire())
    }

    @Test
    fun firstAttemptIsAllowed() {
        assertTrue(throttle.tryAcquire())
    }

    @Test
    fun attemptInsideTheCooldownIsSuppressed() {
        assertTrue(throttle.tryAcquire())
        throttle.release()

        nowMs += 9_999

        assertFalse(throttle.tryAcquire())
    }

    @Test
    fun attemptIsAllowedOnceTheCooldownHasPassed() {
        assertTrue(throttle.tryAcquire())
        throttle.release()

        nowMs += 10_000

        assertTrue(throttle.tryAcquire())
    }

    @Test
    fun reentrantAttemptWhileInProgressIsSuppressed() {
        assertTrue(throttle.tryAcquire())

        assertFalse(throttle.tryAcquire())
    }

    @Test
    fun releaseDoesNotAllowAnAttemptBeforeTheCooldown() {
        assertTrue(throttle.tryAcquire())
        throttle.release()
        nowMs += 1

        assertFalse(throttle.tryAcquire())
    }

    @Test
    fun nullPauseIsNotStale() {
        assertFalse(isStalePause(pausedForMs = null, thresholdMs = 3_000L))
    }

    @Test
    fun pauseAtTheThresholdIsNotStale() {
        assertFalse(isStalePause(pausedForMs = 3_000L, thresholdMs = 3_000L))
    }

    @Test
    fun pauseBeyondTheThresholdIsStale() {
        assertTrue(isStalePause(pausedForMs = 3_001L, thresholdMs = 3_000L))
    }
}
