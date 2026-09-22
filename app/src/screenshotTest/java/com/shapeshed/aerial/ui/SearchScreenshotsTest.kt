package com.shapeshed.aerial.ui

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.ui.theme.AerialTheme

private val sampleRegistryStations = listOf(
    RegistryStation(
        id = 1,
        name = "BBC Radio 6 Music",
        streamUrl = "https://example.test/6music",
        country = "United Kingdom",
        countryCode = "GB",
        tags = "Alternative",
    ),
    RegistryStation(
        id = 2,
        name = "KEXP",
        streamUrl = "https://example.test/kexp",
        country = "United States",
        countryCode = "US",
        tags = "Indie",
    ),
    RegistryStation(
        id = 3,
        name = "FIP",
        streamUrl = "https://example.test/fip",
        country = "France",
        countryCode = "FR",
        tags = "Eclectic",
    ),
)

private val sampleFavorite = Station(
    id = 1,
    name = "BBC Radio 2",
    streamUrl = "https://example.test/r2",
    isFavorite = true,
)

private val noHeader: @Composable () -> Unit = {}

@PreviewTest
@Preview(name = "Search default", device = "spec:width=400dp,height=800dp,dpi=420")
@Composable
private fun SearchDefaultScreenshot() {
    AerialTheme(dynamicColor = false) {
        DefaultSearchResults(
            stations = sampleRegistryStations,
            savedStreamUrls = emptySet(),
            savedRegistryKeys = emptySet(),
            currentStation = null,
            isPlaying = false,
            isBuffering = false,
            onPlay = {},
            onPreviewPlay = {},
            onTogglePlayback = {},
            onAdd = {},
            onRemove = {},
            state = rememberLazyGridState(),
            header = noHeader,
        )
    }
}

@PreviewTest
@Preview(name = "Search recent", device = "spec:width=400dp,height=800dp,dpi=420")
@Composable
private fun SearchRecentScreenshot() {
    AerialTheme(dynamicColor = false) {
        RecentSearches(
            searches = listOf("Mango", "Jazz", "BBC"),
            onSelect = {},
            onRemove = {},
            state = rememberLazyGridState(),
            header = noHeader,
        )
    }
}

@PreviewTest
@Preview(name = "Search no results", device = "spec:width=400dp,height=800dp,dpi=420")
@Composable
private fun SearchNoResultsScreenshot() {
    AerialTheme(dynamicColor = false) {
        RegistrySearchResults(
            favoriteResults = emptyList(),
            results = emptyList(),
            savedStreamUrls = emptySet(),
            savedRegistryKeys = emptySet(),
            currentStation = null,
            isPlaying = false,
            isBuffering = false,
            onFavoritePlay = {},
            onFavoritePreviewPlay = {},
            onPlay = {},
            onPreviewPlay = {},
            onTogglePlayback = {},
            onAdd = {},
            onRemove = {},
            bottomPadding = 0.dp,
            state = rememberLazyGridState(),
            header = noHeader,
            onAddManually = {},
        )
    }
}

@PreviewTest
@Preview(name = "Search results", device = "spec:width=400dp,height=800dp,dpi=420")
@Composable
private fun SearchResultsScreenshot() {
    AerialTheme(dynamicColor = false) {
        RegistrySearchResults(
            favoriteResults = listOf(sampleFavorite),
            results = sampleRegistryStations,
            savedStreamUrls = setOf(sampleFavorite.streamUrl),
            savedRegistryKeys = emptySet(),
            currentStation = sampleFavorite,
            isPlaying = true,
            isBuffering = false,
            onFavoritePlay = {},
            onFavoritePreviewPlay = {},
            onPlay = {},
            onPreviewPlay = {},
            onTogglePlayback = {},
            onAdd = {},
            onRemove = {},
            bottomPadding = 0.dp,
            state = rememberLazyGridState(),
            header = noHeader,
        )
    }
}
