package com.shapeshed.aerial.ui

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveNavigationShellTest {
    @Test
    fun compactWidthUsesNavigationBar() {
        assertEquals(NavigationSuiteType.NavigationBar, navigationSuiteType(widthDp = 599))
    }

    @Test
    fun mediumAndExpandedWidthsUseNavigationRail() {
        assertEquals(NavigationSuiteType.NavigationRail, navigationSuiteType(widthDp = 600))
        assertEquals(NavigationSuiteType.NavigationRail, navigationSuiteType(widthDp = 900))
    }
}
