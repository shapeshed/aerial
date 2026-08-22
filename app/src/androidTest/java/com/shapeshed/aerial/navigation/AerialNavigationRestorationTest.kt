package com.shapeshed.aerial.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AerialNavigationRestorationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun typedStationRouteSurvivesSavedStateRestoration() {
        val restorationTester = StateRestorationTester(composeRule)
        lateinit var navigateToStation: () -> Unit
        lateinit var routes: () -> List<NavKey>

        restorationTester.setContent {
            val backStack = rememberNavBackStack(AerialRoute.Main)
            navigateToStation = { backStack.add(AerialRoute.EditStation(stationId = 42L)) }
            routes = { backStack.toList() }
        }

        composeRule.runOnIdle { navigateToStation() }
        assertEquals(
            listOf(AerialRoute.Main, AerialRoute.EditStation(stationId = 42L)),
            routes(),
        )

        restorationTester.emulateSavedInstanceStateRestore()

        assertEquals(
            listOf(AerialRoute.Main, AerialRoute.EditStation(stationId = 42L)),
            routes(),
        )
    }
}
