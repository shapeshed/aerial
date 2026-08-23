package com.shapeshed.aerial

import org.junit.Assert.assertEquals
import org.junit.Test

class StationMediaItemsTest {

    @Test
    fun stationNameDoesNotChangeWhenMetadataTitleBecomesTheIcySong() {
        assertEquals("Mango Radio", stationNameFromMediaMetadata("Mango Radio", "Song title from ICY"))
    }
}
