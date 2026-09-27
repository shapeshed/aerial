package com.shapeshed.aerial.widget

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay

/**
 * Coalesces bursts of widget redraw requests into a single draw.
 *
 * Playback state changes arrive in bursts — one metadata update writes several pieces of state —
 * so a naive "redraw on every change" repaints the widget several times for one visible change.
 * Each request takes a generation number; a request that has been superseded by the time its
 * debounce window elapses is dropped without doing any work.
 *
 * Split out of [DefaultWidgetUpdater] so the coalescing rule is unit-testable on virtual time
 * instead of needing a live `AppWidgetManager`.
 */
internal class WidgetUpdateDebounce(private val debounceMs: Long) {

    private val generation = AtomicLong()

    /**
     * Waits out the debounce window.
     *
     * @return the token to use for [isCurrent], or `null` if a newer request arrived meanwhile and
     *   this one should be abandoned.
     */
    suspend fun awaitTurn(): Long? {
        val token = generation.incrementAndGet()
        delay(debounceMs)
        return token.takeIf { it == generation.get() }
    }

    /**
     * Whether [token] is still the newest request. Callers pass this as the publish guard so a
     * redraw that was superseded *while it was building* is discarded rather than shown.
     */
    fun isCurrent(token: Long): Boolean = token == generation.get()
}
