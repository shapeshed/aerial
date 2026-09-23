package com.shapeshed.aerial

import android.content.Context
import androidx.media3.common.MediaMetadata
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.ui.computeTrackDisplay
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

    @Test
    fun notificationTrackTextMatchesTheMiniPlayerWhenOnlyATitleIsKnown() {
        val metadata = trackDisplayMetadata(
            base = MediaMetadata.Builder().build(),
            stationName = "Kool FM",
            title = "Now playing info goes here",
            artist = null,
            liveRadio = "Live Radio",
        )
        val display = computeTrackDisplay("Kool FM", "Now playing info goes here", null, "Live Radio")

        assertEquals(display.title, metadata.title.toString())
        assertEquals(display.artist, metadata.artist.toString())
        assertEquals("Kool FM", metadata.artist.toString())
    }

    @Test
    fun notificationTextMatchesTheMiniPlayerWithoutTrackInfo() {
        val context = mock<Context>()
        whenever(context.getString(any())).thenReturn("Live Radio")
        val station = Station(name = "Kool FM", streamUrl = "https://example.com/stream")

        val metadata = stationMediaMetadata(context, station)
        val display = computeTrackDisplay("Kool FM", null, null, "Live Radio")

        assertEquals(display.title, metadata.title.toString())
        assertEquals(display.artist, metadata.artist.toString())
        assertEquals("Live Radio", metadata.artist.toString())
    }
}
