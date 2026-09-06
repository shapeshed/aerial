package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoritesQueueCoordinatorTest {
    private val coordinator = FavoritesQueueCoordinator()

    @Test
    fun displayOrderUsesThePersistedQueueRankWithoutChangingSortSemantics() {
        val first = station(1, "First")
        val second = station(2, "Second")

        assertEquals(
            listOf(2L, 1L),
            coordinator.sortForDisplay(
                stations = listOf(first, second),
                sort = FavoritesSort.AZ,
                activeOrder = listOf(2L, 1L),
            ).map(Station::id),
        )
    }

    @Test
    fun queueMustContainTheSameStationIdentitiesBeforeItIsReordered() {
        val first = station(1, "First")
        val second = station(2, "Second")

        assertNull(
            coordinator.reorderIfFavoritesQueue(
                activeQueue = listOf(first, second),
                favorites = listOf(first),
                sort = FavoritesSort.AZ,
            ),
        )
        assertEquals(listOf(2L, 1L), coordinator.persistedOrder(listOf(second, first), listOf(first, second)))
    }

    @Test
    fun largeFavoritesCollectionUsesIdentityLookupForQueueOperations() {
        val favorites = (1L..10_000L).map { id -> station(id, "Station $id") }
        val reversedQueue = favorites.asReversed()

        assertEquals(
            sortStations(favorites, FavoritesSort.AZ).map(Station::id),
            coordinator.reorderIfFavoritesQueue(
                activeQueue = reversedQueue,
                favorites = favorites,
                sort = FavoritesSort.AZ,
            )?.map(Station::id),
        )
        assertEquals(
            reversedQueue.map(Station::id),
            coordinator.persistedOrder(reversedQueue, favorites),
        )
    }

    private fun station(id: Long, name: String) = Station(
        id = id,
        name = name,
        streamUrl = "https://stream.example/$id",
        isFavorite = true,
    )
}
