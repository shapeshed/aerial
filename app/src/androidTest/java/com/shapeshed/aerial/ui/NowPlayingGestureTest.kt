package com.shapeshed.aerial.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.ui.theme.AerialTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NowPlayingGestureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun swipeDownFromScrollableMetadataDismissesPlayer() {
        var dismissed = false
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                NowPlayingScreen(
                    station = Station(
                        id = 1,
                        name = "RADIO BOLLYWOOD 90s",
                        streamUrl = "https://example.test/stream",
                    ),
                    isPlaying = true,
                    isBuffering = false,
                    currentTrackTitle = "Ghantalele.com | Alit Sen -",
                    currentTrackArtist = "Hariharan, Chitra",
                    currentBitrateKbps = null,
                    showStreamBitrate = false,
                    sleepTimer = null,
                    swipeStations = emptyList(),
                    onPlayStation = {},
                    onToggle = {},
                    onToggleFavorite = {},
                    onSetSleepTimer = {},
                    onCancelSleepTimer = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithText("RADIO BOLLYWOOD 90s").performTouchInput {
            swipeDown()
        }

        composeRule.runOnIdle { assertTrue(dismissed) }
    }
}
