package com.shapeshed.aerial.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
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
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.goBack() },
        modifier = Modifier.fillMaxSize(),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AerialRoute.Home> { renderMainRoute(TAB_HOME, null) }
            entry<AerialRoute.Favorites> { renderMainRoute(TAB_FAVORITES, null) }
            entry<AerialRoute.Mood> { route ->
                renderMainRoute(
                    TAB_HOME,
                    CURATED_MOODS.firstOrNull { it.id == route.moodId },
                )
            }
            entry<AerialRoute.Settings> { settingsContent { navigator.goBack() } }
            entry<AerialRoute.AddStation> { stationEditContent(null) { navigator.goBack() } }
            entry<AerialRoute.EditStation> { route ->
                stationEditContent(route.stationId) { navigator.goBack() }
            }
        },
    )
}
