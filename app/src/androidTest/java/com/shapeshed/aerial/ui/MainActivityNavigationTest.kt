package com.shapeshed.aerial.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.shapeshed.aerial.MainActivity
import com.shapeshed.aerial.R
import com.shapeshed.aerial.testing.AerialTestEnvironmentRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertFalse

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

    @Test
    fun tappingVersionCopiesBuildInformation() {
        composeRule.onNodeWithContentDescription(string(R.string.settings)).performClick()
        composeRule.onNodeWithTag("settings-version").performClick()

        val clipboard = composeRule.activity
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertFalse(clipboard.primaryClip?.getItemAt(0)?.coerceToText(composeRule.activity).isNullOrBlank())
    }

    @Test
    fun noSearchResultsOpenAddStationAndSystemBackReturnsToSearch() {
        composeRule.onNodeWithText(string(R.string.search_hint)).performClick()
        composeRule.onAllNodesWithText(string(R.string.search_hint))
            .get(1)
            .performTextInput("no-station-9f3c2a")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(string(R.string.no_stations_found)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(string(R.string.add_your_own_station)).performClick()
        composeRule.onNodeWithText(string(R.string.add_station)).assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText("no-station-9f3c2a").assertIsDisplayed()
    }

    private fun string(resourceId: Int): String = composeRule.activity.getString(resourceId)
}
