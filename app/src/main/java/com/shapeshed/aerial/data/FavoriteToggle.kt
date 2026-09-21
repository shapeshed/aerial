package com.shapeshed.aerial.data

/** The repository write a favorite toggle performs for a station. */
internal enum class FavoriteToggleAction {
    Save,
    MarkFavorite,
    Remove,
}

/**
 * Row-existence means "favourited": an unsaved (ephemeral) station is saved, a
 * saved non-favorite is marked favourite, and a favourite is deleted.
 */
internal fun favoriteToggleAction(station: Station): FavoriteToggleAction = when {
    station.id == 0L -> FavoriteToggleAction.Save
    !station.isFavorite -> FavoriteToggleAction.MarkFavorite
    else -> FavoriteToggleAction.Remove
}

/**
 * Applies the toggle to the in-memory station list so the UI is not stale before
 * the repository flow re-emits.
 */
internal fun applyFavoriteToggleLocally(stations: List<Station>, station: Station): List<Station> =
    if (station.isFavorite) {
        stations.filter { it.id != station.id }
    } else {
        stations.map { if (it.id == station.id) station.copy(isFavorite = true) else it }
    }
