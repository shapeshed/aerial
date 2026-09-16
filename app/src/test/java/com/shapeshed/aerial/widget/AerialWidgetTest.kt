package com.shapeshed.aerial.widget

import com.shapeshed.aerial.data.Station
import org.junit.Assert.assertEquals
import org.junit.Test

class AerialWidgetTest {
    @Test
    fun widgetShowsOnlyFavoritesInStableNameOrder() {
        val stations = listOf(
            Station(id = 3, name = "Zulu", streamUrl = "https://z.example", isFavorite = true),
            Station(id = 2, name = "Not saved", streamUrl = "https://n.example"),
            Station(id = 1, name = "alpha", streamUrl = "https://a.example", isFavorite = true),
        )

        assertEquals(listOf(1L, 3L), stationsForWidget(stations).map(Station::id))
    }
}
