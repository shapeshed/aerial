package com.shapeshed.aerial

import androidx.media3.common.MediaItem
import com.shapeshed.aerial.data.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackStationResolverTest {
    @Test
    fun resolvesSavedStationByNumericMediaId() {
        val saved = station(7, "Saved", "https://old.example", "radio", "seven")
        val item = MediaItem.Builder().setMediaId("7").build()

        assertEquals(saved, stationFromMediaItem(item, listOf(saved)))
    }

    @Test
    fun returnsNullWhenItemHasNoStationMetadata() {
        assertNull(stationFromMediaItem(MediaItem.Builder().setMediaId("unknown").build(), emptyList()))
    }

    private fun station(id: Long, name: String, url: String, provider: String, providerId: String) =
        Station(id = id, name = name, streamUrl = url, provider = provider, providerId = providerId)
}
