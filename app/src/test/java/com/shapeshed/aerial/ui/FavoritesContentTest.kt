package com.shapeshed.aerial.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesContentTest {

    @Test
    fun stationActivityIndicatorOnlyReservesSpaceWhenVisible() {
        assertFalse(
            shouldShowStationActivityIndicator(
                showActivityIndicator = true,
                isActive = false,
                isPlaying = false,
                isBuffering = false,
            ),
        )
        assertFalse(
            shouldShowStationActivityIndicator(
                showActivityIndicator = true,
                isActive = true,
                isPlaying = false,
                isBuffering = false,
            ),
        )
        assertTrue(
            shouldShowStationActivityIndicator(
                showActivityIndicator = true,
                isActive = true,
                isPlaying = true,
                isBuffering = false,
            ),
        )
        assertTrue(
            shouldShowStationActivityIndicator(
                showActivityIndicator = true,
                isActive = true,
                isPlaying = false,
                isBuffering = true,
            ),
        )
    }
}
