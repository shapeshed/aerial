package com.shapeshed.aerial.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.ui.theme.AerialTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MiniPlayerNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun nextButtonIsAvailableAndAdvancesTheQueue() {
        var advanced = false
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                MiniPlayer(
                    station = Station(id = 1, name = "One", streamUrl = "https://example.test/one"),
                    stationName = "One",
                    icyInfo = "Live Radio",
                    isPlaying = true,
                    isBuffering = false,
                    onHeightChanged = {},
                    onStop = {},
                    onTogglePlayback = {},
                    showNextStation = true,
                    onNextStation = { advanced = true },
                    onExpand = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Next station").performClick()

        composeRule.runOnIdle { assertTrue(advanced) }
    }
}
