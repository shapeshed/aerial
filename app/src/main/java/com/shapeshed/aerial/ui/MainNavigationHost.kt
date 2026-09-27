package com.shapeshed.aerial.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.MetadataScope
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
            entry<AerialRoute.Mood>(metadata = metadata { withoutTransitions() }) { route ->
                MainRouteEntry(
                    renderMainRoute,
                    TAB_HOME,
                    CURATED_MOODS.firstOrNull { it.id == route.moodId },
                )
            }
            entry<AerialRoute.Settings>(metadata = metadata { withoutTransitions() }) {
                settingsContent { navigator.goBack() }
            }
            entry<AerialRoute.AddStation> {
                StationEditEntry(stationEditContent, stationId = null) { navigator.goBack() }
            }
            entry<AerialRoute.EditStation> { route ->
                StationEditEntry(stationEditContent, route.stationId) { navigator.goBack() }
            }
        },
    )
}

/**
 * Makes a destination appear and disappear instantly, for both the committed and the
 * finger-tracked forms of a back gesture.
 *
 * Full-screen destinations set this. Nav3's default slides the outgoing and incoming entries
 * horizontally, which reads oddly for something that replaces the whole screen rather than moving
 * within a stack. The predictive variant is worse: `AdaptiveNavigationShell` hosts the navigation
 * bar *outside* the `NavDisplay` and starts its own show animation from a `LaunchedEffect` as soon
 * as the back stack changes, so the bar runs on a different timeline from the content and drifts
 * against the gesture. Removing the transition removes the mismatch — back still works, there is
 * simply nothing to animate.
 */
private fun MetadataScope.withoutTransitions() {
    put(NavDisplay.TransitionKey) { EnterTransition.None togetherWith ExitTransition.None }
    put(NavDisplay.PopTransitionKey) { EnterTransition.None togetherWith ExitTransition.None }
    put(NavDisplay.PredictivePopTransitionKey) { EnterTransition.None togetherWith ExitTransition.None }
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
