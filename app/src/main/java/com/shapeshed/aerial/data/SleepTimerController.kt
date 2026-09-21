package com.shapeshed.aerial.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the sleep-timer countdown: publishes remaining time, then fades the player
 * volume out and pauses it. Time, volume and pause access are injected so the
 * countdown is unit-testable without Android.
 */
internal class SleepTimerController(
    private val scope: CoroutineScope,
    private val nowMs: () -> Long,
    private val readVolume: () -> Float,
    private val writeVolume: (Float) -> Unit,
    private val pause: () -> Unit,
    private val publishState: (SleepTimerState?) -> Unit,
) {
    private var job: Job? = null

    val isRunning: Boolean
        get() = job?.isActive == true

    fun start(durationMs: Long) {
        job?.cancel()
        if (durationMs <= 0L) {
            cancel()
            return
        }
        writeVolume(1f) // clear any leftover fade from a previous timer
        val endAt = nowMs() + durationMs
        job = scope.launch {
            while (isActive) {
                val remaining = endAt - nowMs()
                if (remaining <= 0L) break
                publishState(SleepTimerState(totalMs = durationMs, remainingMs = remaining))
                delay(SleepTimerPolicy.pollDelayMs(remaining))
            }
            fadeOutAndPause()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        writeVolume(1f) // undo any in-progress fade
        publishState(null)
    }

    // Ease the volume down over ~4s so the timer doesn't cut playback off abruptly, then pause.
    // Volume is restored so the next play() isn't silent.
    private suspend fun fadeOutAndPause() {
        val startVolume = readVolume()
        for (volume in SleepTimerPolicy.fadeVolumes(startVolume, FADE_STEPS)) {
            writeVolume(volume)
            delay(FADE_STEP_MS)
        }
        pause()
        writeVolume(1f)
        job = null
        publishState(null)
    }

    private companion object {
        const val FADE_STEPS = 20
        const val FADE_STEP_MS = 200L
    }
}
