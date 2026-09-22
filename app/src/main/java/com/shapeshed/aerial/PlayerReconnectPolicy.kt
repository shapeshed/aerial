package com.shapeshed.aerial

/**
 * Rate-limits stream reconnects so a flapping connection cannot hammer a station's server, and
 * collapses the re-entrant player callbacks a reconnect itself triggers into a single attempt.
 *
 * [nowMs] is injected so the policy is testable without a real clock; production passes
 * [android.os.SystemClock.elapsedRealtime]. A null last-attempt (rather than 0L) keeps the first
 * reconnect available even when the service starts within the cooldown window of device boot.
 */
internal class ReconnectThrottle(private val cooldownMs: Long, private val nowMs: () -> Long) {
    private var lastAttemptMs: Long? = null
    private var inProgress = false

    /**
     * Returns true and arms the cooldown when a reconnect should be attempted now. Returns false
     * while one is already running or a previous attempt was too recent.
     */
    fun tryAcquire(): Boolean {
        if (inProgress) return false
        val now = nowMs()
        val last = lastAttemptMs
        if (last != null && now - last < cooldownMs) return false
        lastAttemptMs = now
        inProgress = true
        return true
    }

    /** Marks the in-flight reconnect finished so a later one can run once the cooldown passes. */
    fun release() {
        inProgress = false
    }
}

/**
 * A pause longer than [thresholdMs] means the socket almost certainly died while paused, so
 * resuming needs a fresh prepare rather than unpausing stale audio.
 */
internal fun isStalePause(pausedForMs: Long?, thresholdMs: Long): Boolean =
    pausedForMs != null && pausedForMs > thresholdMs
