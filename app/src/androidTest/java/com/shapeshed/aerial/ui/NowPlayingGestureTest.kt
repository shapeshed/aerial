package com.shapeshed.aerial.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.ui.theme.AerialTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class NowPlayingGestureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun trackInformationUsesTheWidthBesideTheLeadingCopyAction() {
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

        val surfaceBounds = composeRule.onNodeWithTag("now_playing_track_surface")
            .fetchSemanticsNode().boundsInRoot
        val copyBounds = composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.copy_track_info),
        ).fetchSemanticsNode().boundsInRoot
        val titleBounds = composeRule.onNodeWithTag("now_playing_track_title")
            .fetchSemanticsNode().boundsInRoot
        val artistBounds = composeRule.onNodeWithTag("now_playing_track_artist")
            .fetchSemanticsNode().boundsInRoot
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot

        assertTrue(copyBounds.right <= titleBounds.left)
        assertTrue(titleBounds.right > surfaceBounds.center.x)
        assertTrue(titleBounds.top < artistBounds.top)
        assertTrue(abs(surfaceBounds.center.x - rootBounds.center.x) <= 1f)
        assertTrue(surfaceBounds.width < rootBounds.width * 0.9f)
    }

    @Test
    fun noIcyMetadataDoesNotShowRedundantLiveRadioText() {
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                NowPlayingScreen(
                    station = Station(id = 1, name = "Radio One", streamUrl = "https://example.test/stream"),
                    isPlaying = true,
                    isBuffering = false,
                    currentTrackTitle = null,
                    currentTrackArtist = null,
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

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.live_radio),
        ).assertDoesNotExist()
    }

    @Test
    fun bufferingPlaybackControlRemainsAvailableAndAnnouncesItsState() {
        var toggled = false
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                NowPlayingScreen(
                    station = Station(id = 1, name = "Radio One", streamUrl = "https://example.test/stream"),
                    isPlaying = false,
                    isBuffering = true,
                    currentTrackTitle = null,
                    currentTrackArtist = null,
                    currentBitrateKbps = null,
                    showStreamBitrate = false,
                    sleepTimer = null,
                    swipeStations = emptyList(),
                    onPlayStation = {},
                    onPreviousStation = {},
                    onNextStation = {},
                    onToggle = { toggled = true },
                    onToggleFavorite = {},
                    onSetSleepTimer = {},
                    onCancelSleepTimer = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.buffering),
        ).assertIsEnabled().performClick()
        composeRule.runOnIdle { assertTrue(toggled) }
    }

    @Test
    fun bitrateLabelSitsWithTheStationTitleBelowTheArtwork() {
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                NowPlayingScreen(
                    station = Station(id = 1, name = "BBC Radio 4", streamUrl = "https://example.test/stream"),
                    isPlaying = true,
                    isBuffering = false,
                    currentTrackTitle = null,
                    currentTrackArtist = null,
                    currentBitrateKbps = 128,
                    showStreamBitrate = true,
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

        val artworkBounds = composeRule.onNodeWithTag("now_playing_artwork")
            .fetchSemanticsNode().boundsInRoot
        val bitrateBounds = composeRule.onNodeWithTag("now_playing_bitrate")
            .fetchSemanticsNode().boundsInRoot
        val stationTitleBounds = composeRule.onNodeWithText("BBC Radio 4")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(bitrateBounds.top >= artworkBounds.bottom)
        assertTrue(abs(bitrateBounds.center.y - stationTitleBounds.center.y) <= bitrateBounds.height)
    }

    @Test
    fun bitrateAppearanceMovesTheStationTitleContinuously() {
        composeRule.mainClock.autoAdvance = false
        val bitrate = mutableStateOf<Int?>(null)
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                NowPlayingScreen(
                    station = Station(id = 1, name = "BBC Radio 4", streamUrl = "https://example.test/stream"),
                    isPlaying = true,
                    isBuffering = false,
                    currentTrackTitle = null,
                    currentTrackArtist = null,
                    currentBitrateKbps = bitrate.value,
                    showStreamBitrate = true,
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

        fun stationTitleCenterX() = composeRule.onNodeWithText("BBC Radio 4")
            .fetchSemanticsNode().boundsInRoot.center.x

        val initialCenter = stationTitleCenterX()
        composeRule.runOnIdle { bitrate.value = 128 }
        composeRule.mainClock.advanceTimeBy(64)
        val intermediateCenter = stationTitleCenterX()
        composeRule.mainClock.advanceTimeBy(1_000)
        val settledCenter = stationTitleCenterX()

        assertTrue(settledCenter < initialCenter)
        assertTrue(intermediateCenter < initialCenter)
        assertTrue(intermediateCenter > settledCenter)
    }

    @Test
    fun icyMetadataAppearsBelowTheFixedPlaybackControls() {
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

        val playbackControlBottom = composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.pause),
        ).fetchSemanticsNode().boundsInRoot.bottom
        val trackTitleTop = composeRule.onNodeWithTag("now_playing_track_title")
            .fetchSemanticsNode().boundsInRoot.top

        assertTrue(trackTitleTop >= playbackControlBottom)
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
