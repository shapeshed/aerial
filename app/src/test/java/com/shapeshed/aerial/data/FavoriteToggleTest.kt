package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteToggleTest {
    @Test
    fun ephemeralStationIsSaved() {
        assertEquals(FavoriteToggleAction.Save, favoriteToggleAction(station(id = 0, isFavorite = false)))
    }

    @Test
    fun savedNonFavoriteIsMarkedFavorite() {
        assertEquals(FavoriteToggleAction.MarkFavorite, favoriteToggleAction(station(id = 7, isFavorite = false)))
    }

    @Test
    fun savedFavoriteIsRemoved() {
        assertEquals(FavoriteToggleAction.Remove, favoriteToggleAction(station(id = 7, isFavorite = true)))
    }

    @Test
    fun removingAFavoriteDropsItFromTheLocalList() {
        val favorite = station(id = 7, isFavorite = true)
        val other = station(id = 8, isFavorite = true)

        val updated = applyFavoriteToggleLocally(listOf(favorite, other), favorite)

        assertEquals(listOf(other), updated)
    }

    @Test
    fun markingAFavoriteUpdatesItInTheLocalList() {
        val station = station(id = 7, isFavorite = false)
        val other = station(id = 8, isFavorite = true)

        val updated = applyFavoriteToggleLocally(listOf(station, other), station)

        assertEquals(station.copy(isFavorite = true), updated.first())
        assertEquals(other, updated.last())
    }

    private fun station(id: Long, isFavorite: Boolean) = Station(
        id = id,
        name = "Station $id",
        streamUrl = "https://stream.example/$id",
        isFavorite = isFavorite,
    )
}
