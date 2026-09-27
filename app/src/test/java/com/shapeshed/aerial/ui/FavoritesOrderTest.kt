package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the two orders the favourites tab juggles.
 *
 * The configured sort decides the list. While a favourites queue is playing, next/previous follow
 * the order the user was looking at when they pressed play, so the list and the skip controls
 * cannot disagree. These tests pin when that override applies and, just as importantly, when it
 * must not.
 */
class FavoritesOrderTest {

    // Names chosen so A-Z order matches id order, which keeps the expectations below readable
    // without hiding that the AZ sort is by name and not by id.
    private val alpha = station(1, "Alpha", lastPlayedAt = 300)
    private val bravo = station(2, "Bravo", lastPlayedAt = 200)
    private val charlie = station(3, "Charlie", lastPlayedAt = 100)
    private val favorites = listOf(alpha, bravo, charlie)

    @Test
    fun withNothingPlayingTheConfiguredSortWins() {
        val order = FavoritesOrder()

        assertEquals(
            listOf(alpha, bravo, charlie),
            order.forDisplay(favorites, FavoritesSort.AZ),
        )
    }

    @Test
    fun aPlayingFavoritesQueuePinsTheListToItsOwnOrder() {
        // The user pressed play while the list was in an order the sort no longer produces; the
        // list must keep showing that, or the skip controls would jump.
        val order = FavoritesOrder()
        order.remember(queue = listOf(charlie, alpha, bravo), favorites = favorites)

        assertEquals(listOf(charlie, alpha, bravo), order.forDisplay(favorites, FavoritesSort.AZ))
    }

    @Test
    fun clearingTheOverrideHandsControlBackToTheSort() {
        val order = FavoritesOrder()
        order.remember(queue = listOf(charlie, alpha, bravo), favorites = favorites)

        order.clear()

        assertEquals(listOf(alpha, bravo, charlie), order.forDisplay(favorites, FavoritesSort.AZ))
    }

    @Test
    fun theOverrideIsNullUntilSomethingPinsIt() {
        assertNull(FavoritesOrder().activeOrder.value)
    }

    @Test
    fun rememberingAQueueClearsRatherThanPinsWhenItIsNotAFavoritesQueue() {
        // A single station, or a queue containing an unsaved one, cannot be "the favourites", so
        // pinning to it would freeze the list on the wrong order.
        val order = FavoritesOrder()

        order.remember(queue = listOf(alpha), favorites = favorites)
        assertNull("a single-item queue must not pin the list", order.activeOrder.value)

        order.remember(queue = listOf(charlie, alpha), favorites = listOf(charlie, alpha))
        assertEquals(listOf(3L, 1L), order.activeOrder.value)
    }

    @Test
    fun aQueueWithAnUnsavedStationDoesNotPinTheList() {
        val order = FavoritesOrder()
        val ephemeral = alpha.copy(id = 0L)

        order.remember(queue = listOf(ephemeral, bravo), favorites = favorites)

        assertNull(order.activeOrder.value)
    }

    @Test
    fun aQueueThatIsNotTheFavouriteSetDoesNotPinTheList() {
        // A mood's stations, or search results, played from elsewhere.
        val order = FavoritesOrder()
        val other = listOf(station(9, "Mood One"), station(10, "Mood Two"))

        order.remember(queue = other, favorites = favorites)

        assertNull(order.activeOrder.value)
    }

    @Test
    fun adoptingAnExplicitOrderPinsIt() {
        val order = FavoritesOrder()

        order.adopt(listOf(3L, 2L, 1L))

        assertEquals(listOf(charlie, bravo, alpha), order.forDisplay(favorites, FavoritesSort.AZ))
    }

    @Test
    fun lastPlayedSortStillAppliesWhenNothingIsPlaying() {
        val order = FavoritesOrder()

        assertEquals(
            listOf(alpha, bravo, charlie),
            order.forDisplay(favorites, FavoritesSort.LAST_PLAYED),
        )
    }

    @Test
    fun theActiveOrderWinsOverTheSortButNotOverMembership() {
        val order = FavoritesOrder()
        order.remember(queue = listOf(charlie, bravo, alpha), favorites = favorites)

        // Sorting is recomputed first, then the override ranks it — so a station that was
        // removed from favourites disappears even while the order is pinned.
        val afterRemoval = order.forDisplay(listOf(charlie, alpha), FavoritesSort.LAST_PLAYED)

        assertEquals(listOf(charlie, alpha), afterRemoval)
    }

    private fun station(id: Long, name: String, lastPlayedAt: Long = 0, playCount: Int = 0) = Station(
        id = id,
        name = name,
        streamUrl = "https://example.invalid/$id",
        isFavorite = true,
        lastPlayedAt = lastPlayedAt,
        playCount = playCount,
    )
}
