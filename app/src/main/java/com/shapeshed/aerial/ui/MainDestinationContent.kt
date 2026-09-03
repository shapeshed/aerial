package com.shapeshed.aerial.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.grid.LazyGridState
import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station

@Composable
internal fun MainDestinationContent(
    uiState: MainUiState,
    selectedMood: CuratedMood?,
    selectedMoodStations: List<RegistryStation>,
    savedStreamUrls: Set<String>,
    savedRegistryKeys: Set<RegistryStationKey>,
    bottomPadding: Dp,
    selectedTab: Int,
    appLocale: java.util.Locale,
    homeListState: LazyGridState,
    favoritesGridState: LazyGridState,
    onScrollToTop: () -> Unit,
    onMoodSelected: (CuratedMood) -> Unit,
    onSetForYouCountry: (String) -> Unit,
    onOpenCountrySearch: (String) -> Unit,
    onOpenRegistrySearch: () -> Unit,
    onPlayRegistryStation: (RegistryStation) -> Unit,
    onPlayRegistryQueue: (RegistryStation, List<RegistryStation>) -> Unit,
    onAddRegistryStation: (RegistryStation) -> Unit,
    onRemoveRegistryStation: (RegistryStation) -> Unit,
    onPlayFavorite: (Station) -> Unit,
    onRemoveFavorite: (Station) -> Unit,
    onHomeViewModeChange: (HomeViewMode) -> Unit,
    onSortSelected: (FavoritesSort) -> Unit,
    onStationLongPress: (Station) -> Unit,
) {
    val playback = uiState.playback.playback
    val home = uiState.home
    if (!home.isOnline) {
        NoNetworkState()
    } else if (selectedMood != null) {
        MoodDetailScreen(
            stations = selectedMoodStations,
            currentStation = playback.station,
            isPlaying = playback.isPlaying,
            isBuffering = playback.isBuffering,
            savedStreamUrls = savedStreamUrls,
            savedRegistryKeys = savedRegistryKeys,
            bottomPadding = bottomPadding,
            onPlay = { selectedMoodStations.firstOrNull()?.let { onPlayRegistryQueue(it, selectedMoodStations) } },
            onSave = { selectedMoodStations.forEach(onAddRegistryStation) },
            onAddStation = onAddRegistryStation,
            onRemoveStation = onRemoveRegistryStation,
            onPlayStation = { onPlayRegistryQueue(it, selectedMoodStations) },
        )
    } else if (selectedTab == TAB_HOME) {
        val countryCode = appLocale.country.takeIf { it.isNotBlank() } ?: "GB"
        LaunchedEffect(countryCode) { onSetForYouCountry(countryCode) }
        val hasCountrySelection = home.discovery.forYouStations.isNotEmpty()
        val forYouStations = home.discovery.forYouStations.ifEmpty { home.discovery.featuredStations }
        HomeTabContent(
            forYouStations = forYouStations,
            forYouCountry = countryName(countryCode, appLocale).takeIf { hasCountrySelection },
            recentlyPlayedStations = home.discovery.recentlyPlayedStations,
            listState = homeListState,
            bottomPadding = bottomPadding,
            onMoodTap = onMoodSelected,
            onRecentlyPlayedStationTap = onPlayRegistryStation,
            onFeaturedStationTap = onPlayRegistryStation,
            onForYouViewAll = {
                if (hasCountrySelection) onOpenCountrySearch(countryCode) else onOpenRegistrySearch()
            },
        )
    } else {
        FavoritesTabContent(
            stations = home.stations,
            currentStation = playback.station,
            isPlaying = playback.isPlaying,
            isBuffering = playback.isBuffering,
            homeViewMode = home.preferences.viewMode,
            favoritesSort = home.preferences.favoritesSort,
            gridState = favoritesGridState,
            onScrollToTop = onScrollToTop,
            bottomPadding = bottomPadding,
            onPlay = onPlayFavorite,
            onRemoveFavorite = onRemoveFavorite,
            onHomeViewModeChange = onHomeViewModeChange,
            onSortSelected = onSortSelected,
            onStationLongPress = onStationLongPress,
        )
    }
}
