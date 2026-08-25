package com.shapeshed.aerial.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shapeshed.aerial.R

internal const val HOME_DESTINATION = 0
internal const val FAVORITES_DESTINATION = 1

internal fun navigationSuiteType(widthDp: Int): NavigationSuiteType =
    if (widthDp < 600) NavigationSuiteType.NavigationBar else NavigationSuiteType.NavigationRail

@Composable
internal fun AdaptiveNavigationShell(
    selectedDestination: Int,
    showNavigation: Boolean,
    onDestinationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    navigationSuiteType: NavigationSuiteType? = null,
    content: @Composable () -> Unit,
) {
    val scaffoldState = rememberNavigationSuiteScaffoldState(
        if (showNavigation) NavigationSuiteScaffoldValue.Visible
        else NavigationSuiteScaffoldValue.Hidden,
    )
    LaunchedEffect(showNavigation) {
        if (showNavigation) scaffoldState.show() else scaffoldState.hide()
    }

    val navigationItems: @Composable () -> Unit = {
        NavigationSuiteItem(
            selected = selectedDestination == HOME_DESTINATION,
            onClick = { onDestinationSelected(HOME_DESTINATION) },
            icon = {
                Icon(
                    imageVector = if (selectedDestination == HOME_DESTINATION) Icons.Rounded.Home else Icons.Outlined.Home,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_home)) },
        )
        NavigationSuiteItem(
            selected = selectedDestination == FAVORITES_DESTINATION,
            onClick = { onDestinationSelected(FAVORITES_DESTINATION) },
            icon = {
                Icon(
                    imageVector = if (selectedDestination == FAVORITES_DESTINATION) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.tab_favorites)) },
        )
    }
    if (navigationSuiteType == null) {
        NavigationSuiteScaffold(
            navigationItems = navigationItems,
            state = scaffoldState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxSize(),
            content = content,
        )
    } else {
        NavigationSuiteScaffold(
            navigationItems = navigationItems,
            navigationSuiteType = navigationSuiteType,
            state = scaffoldState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxSize(),
            content = content,
        )
    }
}
