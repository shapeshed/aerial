package com.shapeshed.aerial.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.aerial.MainActivity
import com.shapeshed.aerial.R
import com.shapeshed.aerial.testing.AerialTestEnvironmentRule
import org.junit.Rule
import org.junit.Test

/** Integration coverage for Aerial's Material search surface and configuration handling. */
class SearchHeaderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val testEnvironment = AerialTestEnvironmentRule()

    @Test
    fun rotatingMainScreenToLandscapeKeepsSearchCollapsed() {
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        composeRule.onNodeWithContentDescription(actionBack()).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(settings()).assertIsDisplayed()
        composeRule.onNodeWithText(searchHint()).assertIsDisplayed()
    }

    @Test
    fun tappingSearchOpensNativeSurfaceAndFocusesInput() {
        composeRule.onNodeWithText(searchHint()).performClick()

        composeRule.onAllNodesWithContentDescription(actionBack()).assertAny(hasClickAction())
        composeRule.onAllNodesWithText(searchHint()).assertAny(isFocused())
    }

    private fun searchHint() = composeRule.activity.getString(R.string.search_hint)
    private fun actionBack() = composeRule.activity.getString(R.string.action_back)
    private fun settings() = composeRule.activity.getString(R.string.settings)
}
