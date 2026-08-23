package com.shapeshed.aerial.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
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

        composeRule.onNodeWithTag("now_playing_artwork").performTouchInput {
            swipeDown()
        }

        composeRule.runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun swipeLeftOnArtworkAdvancesToNextStation() {
        val stations = listOf(
            Station(id = 1, name = "RADIO BOLLYWOOD 90s", streamUrl = "https://example.test/one"),
            Station(id = 2, name = "Jazz FM", streamUrl = "https://example.test/two"),
        )
        var playedStation: Station? = null
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                NowPlayingScreen(
                    station = stations.first(),
                    isPlaying = true,
                    isBuffering = false,
                    currentTrackTitle = null,
                    currentTrackArtist = null,
                    currentBitrateKbps = null,
                    showStreamBitrate = false,
                    sleepTimer = null,
                    swipeStations = stations,
                    onPlayStation = { playedStation = it },
                    onToggle = {},
                    onToggleFavorite = {},
                    onSetSleepTimer = {},
                    onCancelSleepTimer = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag("now_playing_artwork").performTouchInput {
            swipeLeft()
        }

        composeRule.runOnIdle { assertTrue(playedStation?.id == 2L) }
    }
}
