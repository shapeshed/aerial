package com.shapeshed.aerial.ui

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.ui.theme.AerialTheme

@PreviewTest
@Preview(
    name = "Now playing standard",
    device = "spec:width=400dp,height=800dp,dpi=420",
)
@androidx.compose.runtime.Composable
fun NowPlayingStandardScreenshot() {
    NowPlayingScreenshotContent(isBuffering = false, showBitrate = true)
}

@PreviewTest
@Preview(
    name = "Now playing large font",
    device = "spec:width=400dp,height=500dp,dpi=420",
    fontScale = 1.5f,
)
@androidx.compose.runtime.Composable
fun NowPlayingLargeFontScreenshot() {
    NowPlayingScreenshotContent(isBuffering = false)
}

@PreviewTest
@Preview(
    name = "Now playing maximum font",
    device = "spec:width=400dp,height=800dp,dpi=420",
    fontScale = 2f,
)
@androidx.compose.runtime.Composable
fun NowPlayingMaximumFontScreenshot() {
    NowPlayingScreenshotContent(isBuffering = false, showBitrate = true)
}

@PreviewTest
@Preview(
    name = "Now playing buffering dark",
    device = "spec:width=400dp,height=800dp,dpi=420",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@androidx.compose.runtime.Composable
fun NowPlayingBufferingDarkScreenshot() {
    NowPlayingScreenshotContent(isBuffering = true, showBitrate = true)
}

@androidx.compose.runtime.Composable
private fun NowPlayingScreenshotContent(isBuffering: Boolean, showBitrate: Boolean = false) {
    AerialTheme(dynamicColor = false) {
        NowPlayingScreen(
            station = Station(
                id = 1,
                name = "RADIO BOLLYWOOD 90s",
                streamUrl = "https://example.test/stream",
            ),
            isPlaying = !isBuffering,
            isBuffering = isBuffering,
            currentTrackTitle = "Ghantalele.com | Alit Sen -",
            currentTrackArtist = "Hariharan, Chitra",
            currentBitrateKbps = 128.takeIf { showBitrate },
            showStreamBitrate = showBitrate,
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
