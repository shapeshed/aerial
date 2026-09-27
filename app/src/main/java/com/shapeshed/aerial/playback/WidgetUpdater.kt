package com.shapeshed.aerial.playback

/**
 * The home-screen widget's entry point for everything outside itself: redraw requests from
 * playback, and the play/pause/previous/next buttons.
 *
 * Lives here rather than in `widget` because the media service and the UI both drive it. An
 * interface, so callers depend on the capability rather than on the repository, DataStore and
 * coroutine scope behind it, and so unit tests can substitute a fake without a DI-aware runner.
 */
interface WidgetUpdater {
    /** Coalesces and schedules a redraw. Safe to call on every playback state change. */
    fun request()

    /** Performs a transport action, connecting to the media session for the duration. */
    suspend fun handlePlaybackAction(action: String?)
}

internal data class WidgetNavigationAvailability(val previous: Boolean, val next: Boolean)

/**
 * Whether the widget's skip controls should be enabled for the selected entry.
 *
 * A single-entry queue hides both controls, so a single favourite never shows dead arrows. The
 * media service computes this to build the notification's media buttons, and the widget computes
 * the same thing for its own RemoteViews, so the rule lives in one place.
 */
internal fun widgetNavigationAvailability(index: Int, size: Int): WidgetNavigationAvailability =
    WidgetNavigationAvailability(
        previous = index in 0 until size && size > 1,
        next = index in 0 until size && size > 1,
    )
