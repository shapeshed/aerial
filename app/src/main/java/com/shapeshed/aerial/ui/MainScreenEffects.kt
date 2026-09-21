package com.shapeshed.aerial.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.shapeshed.aerial.navigation.AerialNavigator
import com.shapeshed.aerial.navigation.AerialRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun MainScreenEffects(
    onConnect: () -> Unit,
    onSetShowNowPlaying: (Boolean) -> Unit,
    onSearchRegistry: (String) -> Unit,
    onClearRecentlyAddedStation: (Long) -> Unit,
    showNowPlaying: Boolean,
    isSearchExpanded: Boolean,
    showCountrySheet: Boolean,
    showGenreSheet: Boolean,
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    scope: CoroutineScope,
    searchQueryText: String,
    recentlyAddedStationId: Long?,
    showHome: Boolean,
    selectedTab: Int,
    currentRoute: AerialRoute,
    navigator: AerialNavigator,
) {
    val currentOnConnect by rememberUpdatedState(onConnect)
    val currentOnSearchRegistry by rememberUpdatedState(onSearchRegistry)
    val currentOnClearRecentlyAddedStation by rememberUpdatedState(onClearRecentlyAddedStation)
    LaunchedEffect(Unit) { currentOnConnect() }
    LaunchedEffect(showHome, selectedTab, currentRoute) {
        val desiredRoute = if (showHome && selectedTab == TAB_HOME) {
            AerialRoute.Home
        } else {
            AerialRoute.Favorites
        }
        if ((currentRoute == AerialRoute.Home || currentRoute == AerialRoute.Favorites) &&
            currentRoute != desiredRoute
        ) {
            navigator.navigateTopLevel(desiredRoute)
        }
    }
    BackHandler(enabled = showNowPlaying) { onSetShowNowPlaying(false) }
    BackHandler(enabled = isSearchExpanded && !showCountrySheet && !showGenreSheet) {
        textFieldState.edit { replace(0, length, "") }
        scope.launch { searchBarState.animateToCollapsed() }
    }
    // SearchStateHolder debounces and distincts this event stream. Dispatching from
    // the query effect only also avoids running the same query when the bar expands.
    LaunchedEffect(searchQueryText) { currentOnSearchRegistry(searchQueryText) }
    LaunchedEffect(recentlyAddedStationId) {
        val stationId = recentlyAddedStationId ?: return@LaunchedEffect
        delay(1_500)
        currentOnClearRecentlyAddedStation(stationId)
    }
}
