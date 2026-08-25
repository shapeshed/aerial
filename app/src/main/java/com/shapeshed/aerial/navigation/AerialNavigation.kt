package com.shapeshed.aerial.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AerialRoute : NavKey {
    @Serializable
    data object Home : AerialRoute

    @Serializable
    data object Favorites : AerialRoute

    @Serializable
    data class Mood(val moodId: String) : AerialRoute

    @Serializable
    data object Settings : AerialRoute

    @Serializable
    data object AddStation : AerialRoute

    @Serializable
    data class EditStation(val stationId: Long) : AerialRoute
}

class AerialNavigator(
    private val mutableBackStack: MutableList<NavKey> = mutableListOf(AerialRoute.Home),
) {
    init {
        require(mutableBackStack.isNotEmpty()) { "Navigation back stack must contain a top-level route" }
        require(mutableBackStack.first().isTopLevelRoute()) {
            "Navigation back stack must start at Home or Favorites"
        }
    }

    val backStack: List<AerialRoute>
        get() = mutableBackStack.map { route -> route as AerialRoute }

    fun navigate(route: AerialRoute): Boolean {
        if (mutableBackStack.last() == route) return false
        mutableBackStack.add(route)
        return true
    }

    fun navigateTopLevel(route: AerialRoute): Boolean {
        require(route.isTopLevelRoute()) { "Only Home and Favorites are top-level routes" }
        if (mutableBackStack.size == 1 && mutableBackStack.last() == route) return false
        mutableBackStack.clear()
        mutableBackStack.add(route)
        return true
    }

    fun goBack(): Boolean {
        if (mutableBackStack.size == 1) return false
        mutableBackStack.removeAt(mutableBackStack.lastIndex)
        return true
    }
}

private fun NavKey.isTopLevelRoute(): Boolean =
    this == AerialRoute.Home || this == AerialRoute.Favorites
