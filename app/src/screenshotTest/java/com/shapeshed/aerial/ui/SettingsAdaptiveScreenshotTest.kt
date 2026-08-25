package com.shapeshed.aerial.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.shapeshed.aerial.data.RegistryStation
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
    HomeScreenshotContent(NavigationSuiteType.NavigationBar)
}

@PreviewTest
@Preview(name = "Home medium", device = "spec:width=610dp,height=500dp,dpi=420")
@Composable
fun MediumHomeScreenshot() {
    HomeScreenshotContent(NavigationSuiteType.NavigationRail)
}

@PreviewTest
@Preview(name = "Home expanded", device = "spec:width=900dp,height=500dp,dpi=420")
@Composable
fun ExpandedHomeScreenshot() {
    HomeScreenshotContent(NavigationSuiteType.NavigationRail)
}

@PreviewTest
@Preview(
    name = "Home dark",
    device = "spec:width=400dp,height=500dp,dpi=420",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun DarkHomeScreenshot() {
    HomeScreenshotContent(NavigationSuiteType.NavigationBar)
}

@PreviewTest
@Preview(
    name = "Home large font",
    device = "spec:width=400dp,height=500dp,dpi=420",
    fontScale = 1.5f,
)
@Composable
fun LargeFontHomeScreenshot() {
    HomeScreenshotContent(NavigationSuiteType.NavigationBar)
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
            gridState = rememberLazyGridState(),
            onScrollToTop = {},
            bottomPadding = 0.dp,
            onPlay = {},
            onRemoveFavorite = {},
            onHomeViewModeChange = {},
            onSortSelected = {},
            onStationLongPress = {},
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun HomeScreenshotContent(navigationSuiteType: NavigationSuiteType) {
    AerialTheme(dynamicColor = false) {
        val searchBarState = rememberContainedSearchBarState()
        val textFieldState = rememberTextFieldState()
        val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
        AdaptiveNavigationShell(
            selectedDestination = HOME_DESTINATION,
            showNavigation = true,
            onDestinationSelected = {},
            navigationSuiteType = navigationSuiteType,
        ) {
            MainAppContent(
                topBar = {
                    AppBarWithSearch(
                        state = searchBarState,
                        inputField = {
                            SearchBarDefaults.InputField(
                                textFieldState = textFieldState,
                                searchBarState = searchBarState,
                                onSearch = {},
                                placeholder = { Text(stringResource(com.shapeshed.aerial.R.string.search_hint)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Search, contentDescription = null)
                                },
                            )
                        },
                        colors = SearchBarDefaults.appBarWithSearchColors(
                            searchBarColors = SearchBarDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            scrolledSearchBarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            appBarContainerColor = MaterialTheme.colorScheme.surface,
                            scrolledAppBarContainerColor = MaterialTheme.colorScheme.surface,
                            appBarNavigationIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            appBarActionIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        actions = {
                            IconButton(
                                onClick = {},
                                shapes = IconButtonShapes(
                                    IconButtonDefaults.smallRoundShape,
                                    IconButtonDefaults.smallPressedShape,
                                ),
                            ) {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = stringResource(com.shapeshed.aerial.R.string.settings),
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                HomeTabContent(
                    forYouStations = previewRegistryStations.drop(2),
                    forYouCountry = "United Kingdom",
                    recentlyPlayedStations = previewRegistryStations.take(3),
                    listState = rememberLazyGridState(),
                    bottomPadding = 0.dp,
                    onMoodTap = {},
                    onRecentlyPlayedStationTap = {},
                    onFeaturedStationTap = {},
                    onForYouViewAll = {},
                )
            }
        }
    }
}

private val previewRegistryStations = listOf(
    RegistryStation(
        id = 1,
        name = "Mango Radio",
        streamUrl = "https://example.test/mango",
        country = "United Kingdom",
        countryCode = "GB",
        provider = "preview",
        providerId = "mango",
    ),
    RegistryStation(
        id = 2,
        name = "Jazz FM",
        streamUrl = "https://example.test/jazz",
        country = "United Kingdom",
        countryCode = "GB",
        provider = "preview",
        providerId = "jazz",
    ),
    RegistryStation(
        id = 3,
        name = "World Service",
        streamUrl = "https://example.test/world",
        country = "United Kingdom",
        countryCode = "GB",
        provider = "preview",
        providerId = "world",
    ),
    RegistryStation(
        id = 4,
        name = "Night Radio",
        streamUrl = "https://example.test/night",
        country = "United Kingdom",
        countryCode = "GB",
        provider = "preview",
        providerId = "night",
    ),
    RegistryStation(
        id = 5,
        name = "Coast",
        streamUrl = "https://example.test/coast",
        country = "United Kingdom",
        countryCode = "GB",
        provider = "preview",
        providerId = "coast",
    ),
)

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
            showNavigation = true,
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
        versionName = "preview",
        snackbarHostState = SnackbarHostState(),
        onShowStreamBitrateChange = {},
        onShowHomeChange = {},
        onExport = {},
        onImport = {},
        onDismiss = {},
    )
}
