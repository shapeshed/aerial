package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackMetadataPolicyTest {
    @Test
    fun filtersStationAndLiveRadioEchoes() {
        assertEquals(
            NormalizedTrackMetadata(title = null, artist = null),
            normalizeTrackMetadata(
                title = "Aerial Live Radio",
                artist = "Mango Radio",
                liveRadioLabel = "Aerial Live Radio",
                stationNames = listOf("Mango Radio"),
            ),
        )
    }

    @Test
    fun keepsParsedTrackWhenMetadataIsNotAStationName() {
        assertEquals(
            NormalizedTrackMetadata(title = "Track", artist = "Artist"),
            normalizeTrackMetadata(
                title = "Artist - Track",
                artist = null,
                liveRadioLabel = "Live Radio",
                stationNames = listOf("Mango Radio"),
            ),
        )
    }
}
