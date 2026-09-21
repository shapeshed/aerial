package com.shapeshed.aerial.data

/** Pure sleep-timer timing maths, kept separate from the Android player. */
internal object SleepTimerPolicy {
    const val POLL_INTERVAL_MS = 1_000L

    /** How long to wait before publishing the next tick, never negative or above the poll interval. */
    fun pollDelayMs(remainingMs: Long): Long = remainingMs.coerceIn(0L, POLL_INTERVAL_MS)

    /** Volume step-down from [startVolume] to zero over [steps] even steps. */
    fun fadeVolumes(startVolume: Float, steps: Int): List<Float> =
        (1..steps).map { step -> startVolume * (1f - step / steps.toFloat()) }
}
