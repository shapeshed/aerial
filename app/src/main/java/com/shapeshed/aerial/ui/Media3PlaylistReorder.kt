package com.shapeshed.aerial.ui

import androidx.media3.common.Player
import com.shapeshed.aerial.data.Station

/** Reorders an existing Media3 playlist without clearing or preparing the active item. */
internal fun reorderPlayerPlaylist(player: Player, current: List<Station>, desired: List<Station>) {
    val working = current.toMutableList()
    desired.forEachIndexed { targetIndex, station ->
        val currentIndex = working.indexOfFirst { it.matches(station) }
        if (currentIndex >= 0 && currentIndex != targetIndex) {
            player.moveMediaItem(currentIndex, targetIndex)
            working.add(targetIndex, working.removeAt(currentIndex))
        }
    }
}
