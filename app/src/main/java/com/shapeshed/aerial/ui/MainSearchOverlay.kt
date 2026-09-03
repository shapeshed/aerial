package com.shapeshed.aerial.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.SearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun MainSearchOverlay(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    textFieldState: androidx.compose.foundation.text.input.TextFieldState,
    query: String,
    uiState: MainUiState,
    savedStreamUrls: Set<String>,
    savedRegistryKeys: Set<RegistryStationKey>,
    onCountryFilter: () -> Unit,
    onGenreFilter: () -> Unit,
    onClearFilters: () -> Unit,
    onSearch: (String) -> Unit,
    onSaveRecentSearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onPlayFavorite: (Station) -> Unit,
    onPlayRegistry: (RegistryStation) -> Unit,
    onTogglePlayback: () -> Unit,
    onAddRegistry: (RegistryStation) -> Unit,
    onRemoveRegistry: (RegistryStation) -> Unit,
    onCollapse: () -> Unit,
    onAddManually: () -> Unit,
) {
    val resultsGridState = rememberLazyGridState()
    val filterHeader: @Composable () -> Unit = {
        SearchFilterRow(
            selectedCountries = uiState.search.filters.selectedCountries,
            selectedTags = uiState.search.filters.selectedTags,
            onCountryClick = onCountryFilter,
            onGenreClick = onGenreFilter,
            onClearAll = onClearFilters,
            hasFilters = uiState.search.filters.selectedCountries.isNotEmpty() ||
                uiState.search.filters.selectedTags.isNotEmpty(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    ExpandedFullScreenContainedSearchBar(
        state = searchBarState,
        inputField = inputField,
    ) {
        if (!uiState.home.isOnline) {
            NoNetworkState()
            return@ExpandedFullScreenContainedSearchBar
        }
        val search = uiState.search
        val playback = uiState.playback.playback
        val hasFilters = search.filters.selectedCountries.isNotEmpty() || search.filters.selectedTags.isNotEmpty()
        if (query.isBlank() && !hasFilters) {
            if (search.results.recentQueries.isNotEmpty()) {
                RecentSearches(
                    searches = search.results.recentQueries,
                    onSelect = { selectedQuery ->
                        textFieldState.edit { replace(0, length, selectedQuery) }
                        onSearch(selectedQuery)
                    },
                    onRemove = onRemoveRecentSearch,
                    state = resultsGridState,
                    header = filterHeader,
                )
            } else {
                DefaultSearchResults(
                    stations = uiState.home.discovery.defaultStations,
                    savedStreamUrls = savedStreamUrls,
                    savedRegistryKeys = savedRegistryKeys,
                    currentStation = playback.station,
                    isPlaying = playback.isPlaying,
                    isBuffering = playback.isBuffering,
                    onPlay = {
                        onPlayRegistry(it)
                        textFieldState.edit { replace(0, length, "") }
                        onCollapse()
                    },
                    onPreviewPlay = onPlayRegistry,
                    onTogglePlayback = onTogglePlayback,
                    onAdd = onAddRegistry,
                    onRemove = onRemoveRegistry,
                    state = resultsGridState,
                    header = filterHeader,
                )
            }
        } else {
            RegistrySearchResults(
                favoriteResults = search.results.favoriteStations,
                results = search.results.registryStations,
                savedStreamUrls = savedStreamUrls,
                savedRegistryKeys = savedRegistryKeys,
                currentStation = playback.station,
                isPlaying = playback.isPlaying,
                isBuffering = playback.isBuffering,
                onFavoritePlay = {
                    onSaveRecentSearch(query)
                    onPlayFavorite(it)
                    textFieldState.edit { replace(0, length, "") }
                    onCollapse()
                },
                onFavoritePreviewPlay = {
                    onSaveRecentSearch(query)
                    onPlayFavorite(it)
                },
                onPlay = {
                    onSaveRecentSearch(query)
                    onPlayRegistry(it)
                    textFieldState.edit { replace(0, length, "") }
                    onCollapse()
                },
                onPreviewPlay = {
                    onSaveRecentSearch(query)
                    onPlayRegistry(it)
                },
                onTogglePlayback = onTogglePlayback,
                onAdd = onAddRegistry,
                onRemove = onRemoveRegistry,
                bottomPadding = 0.dp,
                onAddManually = {
                    onCollapse()
                    onAddManually()
                },
                state = resultsGridState,
                header = filterHeader,
            )
        }
    }
}
