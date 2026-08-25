package com.shapeshed.aerial.ui

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.ui.theme.AerialTheme
import org.junit.Rule
import org.junit.Test

class FavoritesViewModeTransitionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun switchingToListDoesNotRemeasureAndFlashTheOutgoingCard() {
        val stations = (1L..20L).map { id ->
            Station(
                id = id,
                name = "Station $id",
                streamUrl = "https://example.test/$id",
            )
        }

        composeRule.setContent {
            var viewMode by remember { mutableStateOf(HomeViewMode.Cards) }
            AerialTheme(dynamicColor = false) {
                FavoritesTabContent(
                    stations = stations,
                    currentStation = null,
                    isPlaying = false,
                    isBuffering = false,
                    homeViewMode = viewMode,
                    favoritesSort = FavoritesSort.AZ,
                    gridState = rememberLazyGridState(),
                    onScrollToTop = {},
                    bottomPadding = 0.dp,
                    onPlay = {},
                    onRemoveFavorite = {},
                    onHomeViewModeChange = { viewMode = it },
                    onSortSelected = {},
                    onStationLongPress = {},
                )
            }
        }
        composeRule.waitForIdle()

        waitForTag("favorite-card-1")
        val initialSecondItem = composeRule.onNodeWithTag("favorite-card-2")
            .fetchSemanticsNode().boundsInRoot
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription(
            ApplicationProvider.getApplicationContext<Context>().getString(R.string.home_view_list),
        ).performClick()
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onAllNodesWithTag("favorites-controls").assertCountEquals(1)
        composeRule.onNodeWithTag("favorite-card-1").assertDoesNotExist()
        composeRule.onNodeWithTag("favorite-list-1").assertExists()
        val intermediateSecondItem = composeRule.onNodeWithTag("favorite-list-2")
            .fetchSemanticsNode().boundsInRoot

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onNodeWithTag("favorite-card-1").assertDoesNotExist()
        composeRule.onNodeWithTag("favorite-list-1").assertExists()
        val settledSecondItem = composeRule.onNodeWithTag("favorite-list-2")
            .fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(intermediateSecondItem.left > settledSecondItem.left)
        org.junit.Assert.assertTrue(intermediateSecondItem.top < settledSecondItem.top)
        org.junit.Assert.assertTrue(initialSecondItem.left >= intermediateSecondItem.left)

        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag("favorites-content").performScrollToIndex(20)
        composeRule.onNodeWithTag("favorites-controls").assertDoesNotExist()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeRule.onNodeWithTag(tag).assertExists()
                true
            } catch (_: AssertionError) {
                false
            } catch (_: IllegalStateException) {
                false
            }
        }
    }
}
