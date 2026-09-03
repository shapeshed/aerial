package com.shapeshed.aerial.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.shapeshed.aerial.MainActivity
import com.shapeshed.aerial.R
import com.shapeshed.aerial.testing.AerialTestEnvironmentRule
import org.junit.Rule
import org.junit.Test

/** Integration coverage for the Focus detail route and its adaptive Material app bar. */
class FocusNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val testEnvironment = AerialTestEnvironmentRule()

    @Test
    fun focusIsAFullScreenDestinationAndBackReturnsHome() {
        openFocus()

        composeRule.onNodeWithContentDescription(text(R.string.action_back)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.mood_focus_desc)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.tab_home)).assertIsNotDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.onNodeWithText(text(R.string.search_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.tab_home)).assertIsDisplayed()
    }

    @Test
    fun focusPrimaryActionsRemainVisibleInLandscape() {
        openFocus()

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        composeRule.onNodeWithText(text(R.string.play)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.save_all)).assertIsDisplayed()
    }

    private fun openFocus() {
        waitForHome()
        composeRule.onNodeWithText(text(R.string.mood_focus))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun waitForHome() {
        val searchHint = text(R.string.search_hint)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onAllNodesWithText(searchHint).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
    }

    private fun text(resourceId: Int) = composeRule.activity.getString(resourceId)
}
