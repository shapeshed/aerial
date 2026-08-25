package com.shapeshed.aerial.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AerialNavigatorTest {
    @Test
    fun startsAtHomeAndDoesNotPopTheRoot() {
        val navigator = AerialNavigator()

        assertEquals(listOf<AerialRoute>(AerialRoute.Home), navigator.backStack)
        assertFalse(navigator.goBack())
        assertEquals(listOf<AerialRoute>(AerialRoute.Home), navigator.backStack)
    }

    @Test
    fun moodDetailIsPushedOntoTheAppBackStack() {
        val navigator = AerialNavigator()

        navigator.navigate(AerialRoute.Mood("focus"))

        assertEquals(
            listOf(AerialRoute.Home, AerialRoute.Mood("focus")),
            navigator.backStack,
        )
    }

    @Test
    fun pushesTypedDestinationsAndPreservesStationId() {
        val navigator = AerialNavigator()

        navigator.navigate(AerialRoute.AddStation)
        navigator.goBack()
        navigator.navigate(AerialRoute.EditStation(stationId = 42L))

        assertEquals(
            listOf(AerialRoute.Home, AerialRoute.EditStation(stationId = 42L)),
            navigator.backStack,
        )
    }

    @Test
    fun repeatedNavigationToVisibleDestinationIsIgnored() {
        val navigator = AerialNavigator()

        assertTrue(navigator.navigate(AerialRoute.Settings))
        assertFalse(navigator.navigate(AerialRoute.Settings))

        assertEquals(listOf(AerialRoute.Home, AerialRoute.Settings), navigator.backStack)
    }

    @Test
    fun backRemovesOneDestinationAtATime() {
        val navigator = AerialNavigator()
        navigator.navigate(AerialRoute.Settings)
        navigator.navigate(AerialRoute.EditStation(stationId = 7L))

        assertTrue(navigator.goBack())
        assertEquals(listOf(AerialRoute.Home, AerialRoute.Settings), navigator.backStack)
        assertTrue(navigator.goBack())
        assertEquals(listOf<AerialRoute>(AerialRoute.Home), navigator.backStack)
        assertFalse(navigator.goBack())
    }

    @Test
    fun topLevelNavigationReplacesTheCurrentFlow() {
        val navigator = AerialNavigator()
        navigator.navigate(AerialRoute.Mood("focus"))

        assertTrue(navigator.navigateTopLevel(AerialRoute.Favorites))

        assertEquals(listOf(AerialRoute.Favorites), navigator.backStack)
    }
}
