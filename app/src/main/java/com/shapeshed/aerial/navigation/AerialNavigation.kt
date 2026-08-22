package com.shapeshed.aerial.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AerialRoute : NavKey {
    @Serializable
    data object Main : AerialRoute

    @Serializable
    data object Settings : AerialRoute

    @Serializable
    data object AddStation : AerialRoute

    @Serializable
    data class EditStation(val stationId: Long) : AerialRoute
}

class AerialNavigator(
    private val mutableBackStack: MutableList<NavKey> = mutableListOf(AerialRoute.Main),
) {
    init {
        require(mutableBackStack.isNotEmpty()) { "Navigation back stack must contain Main" }
        require(mutableBackStack.first() == AerialRoute.Main) { "Navigation back stack must start at Main" }
    }

    val backStack: List<AerialRoute>
        get() = mutableBackStack.map { route -> route as AerialRoute }

    fun navigate(route: AerialRoute): Boolean {
        if (mutableBackStack.last() == route) return false
        mutableBackStack.add(route)
        return true
    }

    fun goBack(): Boolean {
        if (mutableBackStack.size == 1) return false
        mutableBackStack.removeAt(mutableBackStack.lastIndex)
        return true
    }
}
