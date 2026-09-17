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

    @Test
    fun navigationMirrorsTheActiveQueue() {
        assertEquals(
            WidgetNavigationAvailability(previous = false, next = false),
            widgetNavigationAvailability(index = 0, size = 1),
        )
        assertEquals(
            WidgetNavigationAvailability(previous = true, next = true),
            widgetNavigationAvailability(index = 0, size = 3),
        )
        assertEquals(
            WidgetNavigationAvailability(previous = true, next = true),
            widgetNavigationAvailability(index = 2, size = 3),
        )
    }

    @Test
    fun widgetLayoutAdaptsToAvailableSize() {
        assertEquals(WidgetLayoutSize(180, 48), widgetLayoutSize(width = 180, height = 48))
        assertEquals(WidgetLayoutSize(304, 48), widgetLayoutSize(width = 400, height = 48))
        assertEquals(WidgetLayoutSize(180, 80), widgetLayoutSize(width = 180, height = 100))
        assertEquals(WidgetLayoutSize(304, 80), widgetLayoutSize(width = 400, height = 100))
        assertEquals(WidgetLayoutSize(180, 152), widgetLayoutSize(width = 180, height = 200))
        assertEquals(WidgetLayoutSize(304, 152), widgetLayoutSize(width = 400, height = 200))
        assertEquals(WidgetLayoutSize(180, 272), widgetLayoutSize(width = 180, height = 300))
        assertEquals(WidgetLayoutSize(304, 272), widgetLayoutSize(width = 400, height = 300))
    }
}
