package com.shapeshed.aerial.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.shapeshed.aerial.navigation.AerialNavigator
import com.shapeshed.aerial.navigation.AerialRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun MainScreenEffects(
    viewModel: MainViewModel,
    context: Context,
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
    LaunchedEffect(Unit) { viewModel.connect(context) }
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
    BackHandler(enabled = showNowPlaying) { viewModel.setShowNowPlaying(false) }
    BackHandler(enabled = isSearchExpanded && !showCountrySheet && !showGenreSheet) {
        textFieldState.edit { replace(0, length, "") }
        scope.launch { searchBarState.animateToCollapsed() }
    }
    LaunchedEffect(searchQueryText) {
        viewModel.searchRegistry(searchQueryText)
    }
    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) viewModel.searchRegistry(searchQueryText)
    }
    LaunchedEffect(recentlyAddedStationId) {
        val stationId = recentlyAddedStationId ?: return@LaunchedEffect
        delay(1_500)
        viewModel.clearRecentlyAddedStation(stationId)
    }
}
