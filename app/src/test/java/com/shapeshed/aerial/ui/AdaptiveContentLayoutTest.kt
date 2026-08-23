package com.shapeshed.aerial.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveContentLayoutTest {
    @Test
    fun favoriteDensityPreferenceMapsToCompactCardWidth() {
        assertEquals(200, favoriteCardMinimumWidthDp(compactColumns = 2))
        assertEquals(133, favoriteCardMinimumWidthDp(compactColumns = 3))
        assertEquals(100, favoriteCardMinimumWidthDp(compactColumns = 4))
    }

    @Test
    fun favoriteDensityIsBoundedForInvalidOrExtremeValues() {
        assertEquals(200, favoriteCardMinimumWidthDp(compactColumns = 0))
        assertEquals(50, favoriteCardMinimumWidthDp(compactColumns = 8))
        assertEquals(50, favoriteCardMinimumWidthDp(compactColumns = 20))
    }
}
