package com.shapeshed.aerial.ui

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.shapeshed.aerial.navigation.AerialNavigator
import com.shapeshed.aerial.navigation.AerialRoute

@Composable
internal fun MainNavigationHost(
    backStack: List<NavKey>,
    navigator: AerialNavigator,
    renderMainRoute: @Composable (Int, CuratedMood?) -> Unit,
    settingsContent: @Composable (onDismiss: () -> Unit) -> Unit,
    stationEditContent: @Composable (stationId: Long?, onDismiss: () -> Unit) -> Unit,
) {
    val saveableStateDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val viewModelDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()
    val decorators = androidx.compose.runtime.remember<List<NavEntryDecorator<NavKey>>>(
        saveableStateDecorator,
        viewModelDecorator,
    ) {
        listOf(saveableStateDecorator, viewModelDecorator)
    }
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.goBack() },
        modifier = Modifier.fillMaxSize(),
        entryDecorators = decorators,
        entryProvider = entryProvider {
            entry<AerialRoute.Home> { MainRouteEntry(renderMainRoute, TAB_HOME, mood = null) }
            entry<AerialRoute.Favorites> { MainRouteEntry(renderMainRoute, TAB_FAVORITES, mood = null) }
            entry<AerialRoute.Mood>(
                metadata = metadata {
                    put(NavDisplay.TransitionKey) {
                        androidx.compose.animation.EnterTransition.None togetherWith
                            androidx.compose.animation.ExitTransition.None
                    }
                    put(NavDisplay.PopTransitionKey) {
                        androidx.compose.animation.EnterTransition.None togetherWith
                            androidx.compose.animation.ExitTransition.None
                    }
                    put(NavDisplay.PredictivePopTransitionKey) {
                        androidx.compose.animation.EnterTransition.None togetherWith
                            androidx.compose.animation.ExitTransition.None
                    }
                },
            ) { route ->
                MainRouteEntry(
                    renderMainRoute,
                    TAB_HOME,
                    CURATED_MOODS.firstOrNull { it.id == route.moodId },
                )
            }
            entry<AerialRoute.Settings> { settingsContent { navigator.goBack() } }
            entry<AerialRoute.AddStation> {
                StationEditEntry(stationEditContent, stationId = null) { navigator.goBack() }
            }
            entry<AerialRoute.EditStation> { route ->
                StationEditEntry(stationEditContent, route.stationId) { navigator.goBack() }
            }
        },
    )
}

@Composable
private fun MainRouteEntry(renderMainRoute: @Composable (Int, CuratedMood?) -> Unit, tab: Int, mood: CuratedMood?) {
    renderMainRoute(tab, mood)
}

@Composable
private fun StationEditEntry(
    stationEditContent: @Composable (stationId: Long?, onDismiss: () -> Unit) -> Unit,
    stationId: Long?,
    onDismiss: () -> Unit,
) {
    stationEditContent(stationId, onDismiss)
}
