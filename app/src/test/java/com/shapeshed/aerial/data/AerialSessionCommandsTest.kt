package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AerialSessionCommandsTest {
    @Test
    fun advertisesFavoriteAndSleepTimerCommands() {
        assertEquals(
            listOf(ACTION_TOGGLE_FAVORITE, ACTION_SLEEP_TIMER_SET, ACTION_SLEEP_TIMER_CANCEL),
            AERIAL_CUSTOM_COMMAND_ACTIONS,
        )
    }
}
