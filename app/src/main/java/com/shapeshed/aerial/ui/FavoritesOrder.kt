package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.FavoritesQueueCoordinator
import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Owns the order the favourites tab shows, and when that order is allowed to override the
 * configured sort.
 *
 * There are two orders in play and confusing them is the bug this class exists to prevent. The
 * user's sort (A-Z, last played, most played) decides the list. But while a favourites queue is
 * actually playing, next/previous follow the order the user was *looking at* when they pressed
 * play, so the visible list and the skip controls cannot disagree. That override is the
 * "active order", and it is deliberately null whenever no such queue is playing.
 *
 * Extracted from [MainViewModel] so the two orders, and the rules for when the override applies,
 * are testable without a ViewModel, a player or a database.
 */
internal class FavoritesOrder(private val coordinator: FavoritesQueueCoordinator = FavoritesQueueCoordinator()) {

    /**
     * The override, or null when the configured sort should win. Null is the common case: it is
     * cleared when playback stops and whenever the user leaves the favourites tab.
     */
    private val _activeOrder = MutableStateFlow<List<Long>?>(null)
    val activeOrder: StateFlow<List<Long>?> = _activeOrder.asStateFlow()

    /**
     * The order [activeQueue] should be in under [sort], or null when it is not a favourites
     * queue and so must not be reordered.
     */
    fun reorderIfFavoritesQueue(
        activeQueue: List<Station>,
        favorites: List<Station>,
        sort: FavoritesSort,
    ): List<Station>? = coordinator.reorderIfFavoritesQueue(activeQueue, favorites, sort)

    /**
     * The order to remember for [queue], or null when it is not a favourites queue and therefore
     * must not pin the list.
     */
    fun orderFor(queue: List<Station>, favorites: List<Station>): List<Long>? =
        coordinator.persistedOrder(queue, favorites)

    /**
     * Records that [queue] is now playing, so the list and the skip controls follow it.
     *
     * [favorites] is passed in rather than read so the caller decides which snapshot counts; the
     * caller is the one that knows whether the station list has caught up yet.
     */
    fun remember(queue: List<Station>, favorites: List<Station>) {
        _activeOrder.value = orderFor(queue, favorites)
    }

    /** Adopts [ids] directly, for when a reorder has already decided the new order. */
    fun adopt(ids: List<Long>) {
        _activeOrder.value = ids
    }

    /** Drops the override so the configured sort wins again. */
    fun clear() {
        _activeOrder.value = null
    }

    /**
     * The station list as the favourites tab should render it: the favourites only, in sort
     * order, with the active override applied on top when one is in force.
     */
    fun forDisplay(stations: List<Station>, sort: FavoritesSort): List<Station> =
        coordinator.sortForDisplay(stations, sort, _activeOrder.value)

    /**
     * [forDisplay] as a state flow over the whole station list and the configured sort.
     *
     * `WhileSubscribed` so the combine is not running while no screen is collecting, matching
     * the rest of the ViewModel's derived state.
     */
    fun display(
        allStations: StateFlow<List<Station>>,
        sort: StateFlow<FavoritesSort>,
        scope: CoroutineScope,
    ): StateFlow<List<Station>> = combine(allStations, sort, _activeOrder) { list, activeSort, order ->
        coordinator.sortForDisplay(list, activeSort, order)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
