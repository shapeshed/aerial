package com.shapeshed.aerial.ui

import androidx.compose.runtime.Immutable
import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.SleepTimerState
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.playback.NowPlayingDisplay

/** Coherent player snapshot consumed by Compose as one lifecycle-aware state value. */
@Immutable
data class PlaybackUiState(
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val queue: List<Station> = emptyList(),
    val trackTitle: String? = null,
    val trackArtist: String? = null,
    val bitrateKbps: Int? = null,
    val error: String? = null,
)

@Immutable
data class PlaybackScreenUiState(
    val playback: PlaybackUiState = PlaybackUiState(),
    val display: NowPlayingDisplay = NowPlayingDisplay("", ""),
    val sleepTimer: SleepTimerState? = null,
    val showNowPlaying: Boolean = false,
    val recentlyAddedStationId: Long? = null,
)

@Immutable
data class HomeDiscoveryUiState(
    val featuredStations: List<RegistryStation> = emptyList(),
    val forYouStations: List<RegistryStation> = emptyList(),
    val recentlyPlayedStations: List<RegistryStation> = emptyList(),
    val defaultStations: List<RegistryStation> = emptyList(),
    val curatedMoodStations: Map<String, List<RegistryStation>> = emptyMap(),
)

@Immutable
data class HomePreferencesUiState(
    val viewMode: HomeViewMode = HomeViewMode.Cards,
    val favoritesSort: FavoritesSort = FavoritesSort.AZ,
    val showStreamBitrate: Boolean = false,
    val showHome: Boolean = true,
)

@Immutable
data class HomeUiState(
    val stations: List<Station> = emptyList(),
    val discovery: HomeDiscoveryUiState = HomeDiscoveryUiState(),
    val preferences: HomePreferencesUiState = HomePreferencesUiState(),
    val selectedTab: Int = 0,
    val isOnline: Boolean = true,
)

@Immutable
data class SearchResultsUiState(
    val registryStations: List<RegistryStation> = emptyList(),
    val favoriteStations: List<Station> = emptyList(),
    val recentQueries: List<String> = emptyList(),
    /**
     * A query is in flight, so the result lists are not yet meaningful. The UI shows neither the
     * results nor "nothing found" while this is set.
     */
    val isSearching: Boolean = false,
)

@Immutable
data class SearchFiltersUiState(
    val allTags: List<String> = emptyList(),
    val selectedCountries: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val availableCountries: List<String> = emptyList(),
)

@Immutable
data class SearchUiState(
    val results: SearchResultsUiState = SearchResultsUiState(),
    val filters: SearchFiltersUiState = SearchFiltersUiState(),
)

@Immutable
data class MainUiState(
    val playback: PlaybackScreenUiState = PlaybackScreenUiState(),
    val home: HomeUiState = HomeUiState(),
    val search: SearchUiState = SearchUiState(),
)
