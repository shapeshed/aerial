package com.shapeshed.aerial.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
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
    fun songTitleAppearsAboveArtistLikeSystemMediaControls() {
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                NowPlayingScreen(
                    station = Station(id = 1, name = "Radio One", streamUrl = "https://example.test/stream"),
                    isPlaying = true,
                    isBuffering = false,
                    currentTrackTitle = "Song title",
                    currentTrackArtist = "Artist",
                    currentBitrateKbps = null,
                    showStreamBitrate = false,
                    sleepTimer = null,
                    swipeStations = emptyList(),
                    onPlayStation = {},
                    onPreviousStation = {},
                    onNextStation = {},
                    onToggle = {},
                    onToggleFavorite = {},
                    onSetSleepTimer = {},
                    onCancelSleepTimer = {},
                    onDismiss = {},
                )
            }
        }

        val titleTop = composeRule.onNodeWithTag("now_playing_track_title").fetchSemanticsNode().boundsInRoot.top
        val artistTop = composeRule.onNodeWithTag("now_playing_track_artist").fetchSemanticsNode().boundsInRoot.top
        assertTrue(titleTop < artistTop)
    }

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
                    onPreviousStation = {},
                    onNextStation = {},
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
                    onPreviousStation = {},
                    onNextStation = {},
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

    @Test
    fun stationNavigationButtonsPlayPreviousAndNextQueueEntries() {
        val stations = listOf(
            Station(id = 1, name = "One", streamUrl = "https://example.test/one"),
            Station(id = 2, name = "Two", streamUrl = "https://example.test/two"),
            Station(id = 3, name = "Three", streamUrl = "https://example.test/three"),
        )
        val playedStations = mutableListOf<Long>()
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                NowPlayingScreen(
                    station = stations[1],
                    isPlaying = true,
                    isBuffering = false,
                    currentTrackTitle = null,
                    currentTrackArtist = null,
                    currentBitrateKbps = null,
                    showStreamBitrate = false,
                    sleepTimer = null,
                    swipeStations = stations,
                    onPlayStation = { playedStations += it.id },
                    onPreviousStation = { playedStations += stations[0].id },
                    onNextStation = { playedStations += stations[2].id },
                    onToggle = {},
                    onToggleFavorite = {},
                    onSetSleepTimer = {},
                    onCancelSleepTimer = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Previous station").performClick()
        composeRule.onNodeWithContentDescription("Next station").performClick()

        composeRule.runOnIdle { assertTrue(playedStations == listOf(1L, 3L)) }
    }
}
