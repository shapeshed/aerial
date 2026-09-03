package com.shapeshed.aerial.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.aerial.MainActivity
import com.shapeshed.aerial.R
import com.shapeshed.aerial.testing.AerialTestEnvironmentRule
import org.junit.Rule
import org.junit.Test

/** Verifies navigation between the real main route and Settings through the Activity. */
class MainActivityNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val testEnvironment = AerialTestEnvironmentRule()

    @Test
    fun settingsOpensFromMainRouteAndBackReturnsToMainRoute() {
        composeRule.onNodeWithContentDescription(string(R.string.settings)).performClick()
        composeRule.onNodeWithText(string(R.string.settings)).assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.onNodeWithText(string(R.string.search_hint)).assertIsDisplayed()
    }

    private fun string(resourceId: Int): String = composeRule.activity.getString(resourceId)
}
