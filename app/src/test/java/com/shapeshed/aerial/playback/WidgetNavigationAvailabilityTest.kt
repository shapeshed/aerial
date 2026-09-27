package com.shapeshed.aerial.playback

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The media service builds the notification's skip buttons from this rule and the widget applies
 * it to its own RemoteViews, so the two must agree on when the controls appear.
 */
class WidgetNavigationAvailabilityTest {

    @Test
    fun aSingleEntryQueueHidesBothControls() {
        assertEquals(
            WidgetNavigationAvailability(previous = false, next = false),
            widgetNavigationAvailability(index = 0, size = 1),
        )
    }

    @Test
    fun aMultiEntryQueueEnablesBothControls() {
        assertEquals(
            WidgetNavigationAvailability(previous = true, next = true),
            widgetNavigationAvailability(index = 0, size = 3),
        )
        assertEquals(
            WidgetNavigationAvailability(previous = true, next = true),
            widgetNavigationAvailability(index = 2, size = 3),
        )
    }

    @Test
    fun anIndexOutsideTheQueueHidesBothControls() {
        assertEquals(
            WidgetNavigationAvailability(previous = false, next = false),
            widgetNavigationAvailability(index = 3, size = 3),
        )
        assertEquals(
            WidgetNavigationAvailability(previous = false, next = false),
            widgetNavigationAvailability(index = -1, size = 3),
        )
    }
}
