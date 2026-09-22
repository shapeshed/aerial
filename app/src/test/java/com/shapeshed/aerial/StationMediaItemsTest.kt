package com.shapeshed.aerial

import android.content.Context
import com.shapeshed.aerial.data.Station
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class StationMediaItemsTest {

    @Test
    fun stationMetadataUsesStationNameAsAlbumTitle() {
        val context = mock<Context>()
        whenever(context.getString(any())).thenReturn("Live radio")
        val station = Station(name = "Mango Radio", streamUrl = "https://example.com/stream")

        assertEquals("Mango Radio", stationMediaMetadata(context, station).albumTitle)
    }

    @Test
    fun stationNameDoesNotChangeWhenMetadataTitleBecomesTheIcySong() {
        assertEquals("Mango Radio", stationNameFromMediaMetadata("Mango Radio", "Song title from ICY"))
    }
}
