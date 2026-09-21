package com.shapeshed.aerial.data

/** Toggles the current station's favorite state from a media notification / hardware button. */
const val ACTION_TOGGLE_FAVORITE = "com.shapeshed.aerial.action.TOGGLE_FAVORITE"

/**
 * Custom MediaSession commands Aerial advertises to controllers alongside the
 * Media3 defaults. Kept as data so the advertised set can be asserted in tests.
 */
internal val AERIAL_CUSTOM_COMMAND_ACTIONS: List<String> = listOf(
    ACTION_TOGGLE_FAVORITE,
    ACTION_SLEEP_TIMER_SET,
    ACTION_SLEEP_TIMER_CANCEL,
)
