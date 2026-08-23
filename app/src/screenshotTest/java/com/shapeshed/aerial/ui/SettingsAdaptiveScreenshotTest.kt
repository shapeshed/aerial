package com.shapeshed.aerial.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shapeshed.aerial.ui.theme.AerialTheme
import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.Station

@Preview(name = "400x400", device = "spec:width=400dp,height=400dp,dpi=420")
@Preview(name = "400x500", device = "spec:width=400dp,height=500dp,dpi=420")
@Preview(name = "400x1000", device = "spec:width=400dp,height=1000dp,dpi=420")
@Preview(name = "610x400", device = "spec:width=610dp,height=400dp,dpi=420")
@Preview(name = "610x500", device = "spec:width=610dp,height=500dp,dpi=420")
@Preview(name = "610x1000", device = "spec:width=610dp,height=1000dp,dpi=420")
@Preview(name = "900x400", device = "spec:width=900dp,height=400dp,dpi=420")
@Preview(name = "900x500", device = "spec:width=900dp,height=500dp,dpi=420")
@Preview(name = "900x1000", device = "spec:width=900dp,height=1000dp,dpi=420")
annotation class AdaptiveFormFactorPreviews

@PreviewTest
@AdaptiveFormFactorPreviews
@Composable
fun SettingsAdaptiveScreenshot() {
    AerialTheme(dynamicColor = false) {
        SettingsScreenshotContent()
    }
}

@PreviewTest
@Preview(name = "Phone dark", device = "spec:width=400dp,height=500dp,dpi=420", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsDarkScreenshot() {
    AerialTheme(dynamicColor = false) {
        SettingsScreenshotContent()
    }
}

@PreviewTest
@Preview(name = "Phone font 1.5", device = "spec:width=400dp,height=500dp,dpi=420", fontScale = 1.5f)
@Composable
fun SettingsLargeFontScreenshot() {
    AerialTheme(dynamicColor = false) {
        SettingsScreenshotContent()
    }
}

@PreviewTest
@Preview(name = "Navigation compact", device = "spec:width=400dp,height=500dp,dpi=420")
@Composable
fun CompactNavigationScreenshot() {
    NavigationScreenshotContent(NavigationSuiteType.NavigationBar)
}

@PreviewTest
@Preview(name = "Navigation expanded", device = "spec:width=900dp,height=500dp,dpi=420")
@Composable
fun ExpandedNavigationScreenshot() {
    NavigationScreenshotContent(NavigationSuiteType.NavigationRail)
}

@PreviewTest
@Preview(name = "Favorites compact", device = "spec:width=400dp,height=500dp,dpi=420")
@Composable
fun CompactFavoritesScreenshot() {
    FavoritesScreenshotContent()
}

@PreviewTest
@Preview(name = "Favorites expanded", device = "spec:width=900dp,height=500dp,dpi=420")
@Composable
fun ExpandedFavoritesScreenshot() {
    FavoritesScreenshotContent()
}

@PreviewTest
@Preview(name = "Home compact", device = "spec:width=400dp,height=500dp,dpi=420")
@Composable
fun CompactHomeScreenshot() {
    HomeScreenshotContent()
}

@PreviewTest
@Preview(name = "Home expanded", device = "spec:width=900dp,height=500dp,dpi=420")
@Composable
fun ExpandedHomeScreenshot() {
    HomeScreenshotContent()
}

@PreviewTest
@Preview(name = "Mini player", device = "spec:width=400dp,height=220dp,dpi=420")
@Composable
fun MiniPlayerScreenshot() {
    AerialTheme(dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            MiniPlayer(
                station = previewStations.first(),
                stationName = "Mango Radio",
                icyInfo = "Mango Groove",
                isPlaying = true,
                isBuffering = false,
                onHeightChanged = {},
                onStop = {},
                onTogglePlayback = {},
                showNextStation = true,
                onNextStation = {},
                onExpand = {},
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
    }
}

@Composable
private fun FavoritesScreenshotContent() {
    AerialTheme(dynamicColor = false) {
        FavoritesTabContent(
            stations = previewStations,
            currentStation = previewStations.first(),
            isPlaying = true,
            isBuffering = false,
            homeViewMode = HomeViewMode.Cards,
            favoritesSort = FavoritesSort.AZ,
            gridColumns = 3,
            listState = rememberLazyGridState(),
            bottomPadding = 0.dp,
            onPlay = {},
            onTogglePlayback = {},
            onToggleFavorite = {},
            onHomeViewModeChange = {},
            onSortSelected = {},
            onStationLongPress = {},
        )
    }
}

@Composable
private fun HomeScreenshotContent() {
    AerialTheme(dynamicColor = false) {
        HomeTabContent(
            forYouStations = emptyList(),
            forYouCountry = null,
            recentlyPlayedStations = emptyList(),
            listState = rememberLazyGridState(),
            bottomPadding = 0.dp,
            onMoodTap = {},
            onRecentlyPlayedStationTap = {},
            onFeaturedStationTap = {},
            onForYouViewAll = {},
        )
    }
}

private val previewStations = listOf(
    Station(id = 1, name = "Mango Radio", streamUrl = "https://example.test/mango", country = "United Kingdom", countryCode = "GB"),
    Station(id = 2, name = "Jazz FM", streamUrl = "https://example.test/jazz", country = "France", countryCode = "FR"),
    Station(id = 3, name = "World Service", streamUrl = "https://example.test/world", country = "Germany", countryCode = "DE"),
    Station(id = 4, name = "Night Radio", streamUrl = "https://example.test/night", country = "Spain", countryCode = "ES"),
    Station(id = 5, name = "Coast", streamUrl = "https://example.test/coast", country = "Portugal", countryCode = "PT"),
    Station(id = 6, name = "Pulse", streamUrl = "https://example.test/pulse", country = "Ireland", countryCode = "IE"),
)

@Composable
private fun NavigationScreenshotContent(type: NavigationSuiteType) {
    AerialTheme(dynamicColor = false) {
        AdaptiveNavigationShell(
            selectedDestination = HOME_DESTINATION,
            showHome = true,
            onDestinationSelected = {},
            navigationSuiteType = type,
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text("Aerial")
                }
            }
        }
    }
}

@Composable
private fun SettingsScreenshotContent() {
    SettingsContent(
        showStreamBitrate = true,
        showHome = true,
        favoritesGridColumns = 3,
        versionName = "preview",
        snackbarHostState = SnackbarHostState(),
        onShowStreamBitrateChange = {},
        onShowHomeChange = {},
        onFavoritesGridColumnsChange = {},
        onExport = {},
        onImport = {},
        onDismiss = {},
    )
}
