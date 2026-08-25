package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.SleepTimerState
import com.shapeshed.aerial.data.Station

/** Coherent player snapshot consumed by Compose as one lifecycle-aware state value. */
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

data class PlaybackScreenUiState(
    val playback: PlaybackUiState = PlaybackUiState(),
    val display: NowPlayingDisplay = NowPlayingDisplay("", ""),
    val sleepTimer: SleepTimerState? = null,
    val showNowPlaying: Boolean = false,
    val recentlyAddedStationId: Long? = null,
)

data class HomeDiscoveryUiState(
    val featuredStations: List<RegistryStation> = emptyList(),
    val forYouStations: List<RegistryStation> = emptyList(),
    val recentlyPlayedStations: List<RegistryStation> = emptyList(),
    val defaultStations: List<RegistryStation> = emptyList(),
    val curatedMoodStations: Map<String, List<RegistryStation>> = emptyMap(),
)

data class HomePreferencesUiState(
    val viewMode: HomeViewMode = HomeViewMode.Cards,
    val favoritesSort: FavoritesSort = FavoritesSort.AZ,
    val showStreamBitrate: Boolean = false,
    val showHome: Boolean = true,
)

data class HomeUiState(
    val stations: List<Station> = emptyList(),
    val discovery: HomeDiscoveryUiState = HomeDiscoveryUiState(),
    val preferences: HomePreferencesUiState = HomePreferencesUiState(),
    val selectedTab: Int = 0,
    val isOnline: Boolean = true,
)

data class SearchResultsUiState(
    val registryStations: List<RegistryStation> = emptyList(),
    val favoriteStations: List<Station> = emptyList(),
    val recentQueries: List<String> = emptyList(),
)

data class SearchFiltersUiState(
    val allTags: List<String> = emptyList(),
    val selectedCountries: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val availableCountries: List<String> = emptyList(),
)

data class SearchUiState(
    val results: SearchResultsUiState = SearchResultsUiState(),
    val filters: SearchFiltersUiState = SearchFiltersUiState(),
)

data class MainUiState(
    val playback: PlaybackScreenUiState = PlaybackScreenUiState(),
    val home: HomeUiState = HomeUiState(),
    val search: SearchUiState = SearchUiState(),
)

/** Station name plus a second-line ICY/ID3 summary for the mini player and notifications. */
data class NowPlayingDisplay(val title: String, val subtitle: String)

/** Two-line compact-player text: track title first, artist second. */
data class TrackDisplay(val title: String, val artist: String)

fun computeTrackDisplay(
    stationName: String,
    trackTitle: String?,
    trackArtist: String?,
    liveRadio: String = "Live Radio",
): TrackDisplay {
    val title = trackTitle?.trim()?.takeIf {
        it.isNotEmpty() && it != stationName && it != liveRadio
    }
    val artist = trackArtist?.trim()?.takeIf {
        it.isNotEmpty() && it != stationName && it != liveRadio
    }
    return TrackDisplay(title ?: stationName, artist ?: liveRadio)
}

/** Derives stable station and ICY/ID3 display text shared by all playback surfaces. */
fun computeNowPlayingDisplay(
    stationName: String,
    icyTitle: String?,
    icyArtist: String? = null,
    liveRadio: String = "Live Radio",
): NowPlayingDisplay {
    val title = icyTitle?.trim()?.takeIf { it.isNotEmpty() && it != stationName }
    val artist = icyArtist?.trim()?.takeIf { it.isNotEmpty() && it != stationName }
    val icyInfo = when {
        artist != null && title != null -> "$artist — $title"
        title != null -> title
        artist != null -> artist
        else -> liveRadio
    }
    return NowPlayingDisplay(stationName, icyInfo)
}
