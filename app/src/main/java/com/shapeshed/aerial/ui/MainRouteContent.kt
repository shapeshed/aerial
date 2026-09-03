package com.shapeshed.aerial.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.Station

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MainRouteContent(
    destination: Int,
    mood: CuratedMood?,
    snackbarHostState: SnackbarHostState,
    searchBarState: SearchBarState,
    searchInputField: @Composable () -> Unit,
    isSearchExpanded: Boolean,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    searchScrollBehavior: SearchBarScrollBehavior,
    moodScrollBehavior: TopAppBarScrollBehavior,
    renderDestination: @Composable (Int, CuratedMood?) -> Unit,
    currentStation: Station?,
    miniPlayerDisplay: TrackDisplay,
    playbackError: String?,
    isBuffering: Boolean,
    isPlaying: Boolean,
    hasStationNavigation: Boolean,
    onHeightChanged: (Int) -> Unit,
    onStop: () -> Unit,
    onTogglePlayback: () -> Unit,
    onPlayNext: () -> Unit,
    onExpand: () -> Unit,
) {
    MainAppContent(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (mood == null) {
                AppBarWithSearch(
                    state = searchBarState,
                    inputField = searchInputField,
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
                        if (!isSearchExpanded) {
                            IconButton(
                                onClick = onOpenSettings,
                                shapes = IconButtonShapes(
                                    IconButtonDefaults.smallRoundShape,
                                    IconButtonDefaults.smallPressedShape,
                                ),
                            ) {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = androidx.compose.ui.res.stringResource(R.string.settings),
                                )
                            }
                        }
                    },
                    scrollBehavior = searchScrollBehavior,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LargeFlexibleTopAppBar(
                    title = { Text(androidx.compose.ui.res.stringResource(mood.titleRes)) },
                    subtitle = { Text(androidx.compose.ui.res.stringResource(mood.detailDescriptionRes)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(R.string.action_back),
                            )
                        }
                    },
                    scrollBehavior = moodScrollBehavior,
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(
                if (mood == null) searchScrollBehavior.nestedScrollConnection
                else moodScrollBehavior.nestedScrollConnection,
            ),
    ) {
        renderDestination(destination, mood)

        MiniPlayer(
            station = currentStation,
            stationName = miniPlayerDisplay.title,
            icyInfo = playbackError
                ?: if (isBuffering) androidx.compose.ui.res.stringResource(R.string.buffering) else miniPlayerDisplay.artist,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            onHeightChanged = onHeightChanged,
            onStop = onStop,
            onTogglePlayback = onTogglePlayback,
            showNextStation = hasStationNavigation,
            onNextStation = onPlayNext,
            onExpand = onExpand,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .semantics { traversalIndex = 1f }
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        )
    }
}
