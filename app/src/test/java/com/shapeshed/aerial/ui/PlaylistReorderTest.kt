package com.shapeshed.aerial.ui

import androidx.media3.common.Player
import com.shapeshed.aerial.data.Station
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class PlaylistReorderTest {
    @Test
    fun reorderingFavoritesMovesExistingItemsWithoutReplacingPlaylist() {
        val alpha = Station(id = 1, name = "Alpha", streamUrl = "https://example.test/a")
        val bravo = Station(id = 2, name = "Bravo", streamUrl = "https://example.test/b")
        val player = mock<Player>()

        reorderPlayerPlaylist(player, listOf(alpha, bravo), listOf(bravo, alpha))

        verify(player).moveMediaItem(1, 0)
    }
}
