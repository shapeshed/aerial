package com.shapeshed.aerial.ui

/**
 * How far startup has got, which is what decides whether the splash screen can be dismissed.
 *
 * Deliberately not a `Boolean`: a boolean cannot distinguish "still loading" from "gave up", and
 * conflating the two is what let a source that never produced content leave the user on a splash
 * screen forever.
 */
sealed interface AppStartupState {

    /** Waiting on the first home-screen content. */
    data object Loading : AppStartupState

    /** Enough content to render the home screen. */
    data object Ready : AppStartupState

    /**
     * A source never produced content in time. The app still opens — an empty home screen beats
     * an indefinite splash — and the wait is kept for diagnosis.
     */
    data class GaveUp(val waitedMs: Long) : AppStartupState

    /** Whether the splash screen can be dismissed, whether content arrived or not. */
    val settled: Boolean get() = this !is Loading
}

/**
 * How long startup waits for home-screen content before opening the app anyway.
 *
 * The wait covers opening the station database, reading the preferences and resolving the
 * recently-played history against the registry. On a slow device or a cold page cache that is
 * real work, so this is generous; it exists only to bound the pathological case, not to be tuned
 * for speed.
 */
internal const val STARTUP_CONTENT_TIMEOUT_MS = 5_000L
