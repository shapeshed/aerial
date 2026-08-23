package com.shapeshed.aerial.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LogoAppearanceCacheTest {
    @Test
    fun repeatedKeyUsesCachedAnalysis() {
        val cache = LogoAppearanceCache(maxEntries = 2)
        var analyses = 0

        val first = cache.getOrCompute("station-logo") {
            analyses++
            LogoAppearance(isLight = true, hasTransparentMargin = false)
        }
        val second = cache.getOrCompute("station-logo") {
            analyses++
            LogoAppearance(isLight = false, hasTransparentMargin = true)
        }

        assertEquals(first, second)
        assertEquals(1, analyses)
    }

    @Test
    fun leastRecentlyUsedEntryIsEvicted() {
        val cache = LogoAppearanceCache(maxEntries = 2)
        var analyses = 0
        fun appearance() = LogoAppearance(isLight = analyses++ % 2 == 0, hasTransparentMargin = false)

        cache.getOrCompute("a", ::appearance)
        cache.getOrCompute("b", ::appearance)
        cache.getOrCompute("a", ::appearance)
        cache.getOrCompute("c", ::appearance)
        cache.getOrCompute("b", ::appearance)

        assertEquals(4, analyses)
    }
}
