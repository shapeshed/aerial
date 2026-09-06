package com.shapeshed.aerial.data

/** Pure queue/list ordering rules shared by the UI state holder and its tests. */
internal class FavoritesQueueCoordinator {
    fun persistedOrder(queue: List<Station>, favorites: List<Station>): List<Long>? {
        if (queue.size < 2 || queue.any { it.id == 0L }) return null
        return queue
            .takeIf { it.size == favorites.size && haveSameStationIdentities(it, favorites) }
            ?.map(Station::id)
    }

    fun reorderIfFavoritesQueue(
        activeQueue: List<Station>,
        favorites: List<Station>,
        sort: FavoritesSort,
    ): List<Station>? {
        if (activeQueue.size < 2) return null
        if (favorites.size != activeQueue.size || !haveSameStationIdentities(activeQueue, favorites)) return null
        return sortStations(favorites, sort)
    }

    fun sortForDisplay(
        stations: List<Station>,
        sort: FavoritesSort,
        activeOrder: List<Long>?,
    ): List<Station> {
        val sorted = sortStations(stations.filter(Station::isFavorite), sort)
        if (activeOrder == null) return sorted
        val ranks = stationOrder(activeOrder)
        return sorted.sortedBy { station -> ranks[station.id] ?: Int.MAX_VALUE }
    }
}
