package com.shapeshed.aerial.ui

import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.ui.theme.AerialTheme

@PreviewTest
@Preview(
    name = "Now playing large font",
    device = "spec:width=400dp,height=500dp,dpi=420",
    fontScale = 1.5f,
)
@androidx.compose.runtime.Composable
fun NowPlayingLargeFontScreenshot() {
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
            onDismiss = {},
        )
    }
}
