package com.shapeshed.aerial.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Session command actions for the sleep timer. Defined here (rather than on PlayerService's
// private companion) so both the service and the MediaController side can share them.
const val ACTION_SLEEP_TIMER_SET = "com.shapeshed.aerial.action.SLEEP_TIMER_SET"
const val ACTION_SLEEP_TIMER_CANCEL = "com.shapeshed.aerial.action.SLEEP_TIMER_CANCEL"
const val SLEEP_TIMER_DURATION_MS = "durationMs"

/** Snapshot of a running sleep timer. Null in the store means no timer is active. */
data class SleepTimerState(
    val totalMs: Long,
    val remainingMs: Long,
)

/**
 * Process-wide holder for the active sleep-timer countdown, mirroring [NowPlayingStore].
 * PlayerService owns the countdown and publishes ticks here; the UI collects them.
 */
object SleepTimerStore {
    private val _state = MutableStateFlow<SleepTimerState?>(null)
    val state: StateFlow<SleepTimerState?> = _state.asStateFlow()
    fun set(state: SleepTimerState?) { _state.value = state }
}
