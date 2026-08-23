package com.shapeshed.aerial.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingDisplayTest {

    @Test
    fun noMetadataShowsStationNameAndLiveRadio() {
        val display = computeNowPlayingDisplay("Radio X", icyTitle = null)
        assertEquals(NowPlayingDisplay("Radio X", "Live Radio"), display)
    }

    @Test
    fun icyArtistAndTitleShown() {
        val display = computeNowPlayingDisplay(
            "KISS Dance", icyTitle = "Slow Burner", icyArtist = "Interplanetary Criminal",
        )
        assertEquals(NowPlayingDisplay("KISS Dance", "Interplanetary Criminal — Slow Burner"), display)
    }

    @Test
    fun icyTitleOnlyFallsBackToStationName() {
        val display = computeNowPlayingDisplay("Radio X", icyTitle = "Some Show", icyArtist = null)
        assertEquals(NowPlayingDisplay("Radio X", "Some Show"), display)
    }

    @Test
    fun icyTitleEqualToStationNameFallsBackToLiveRadio() {
        // No track metadata: the media item title is the station name — must not show twice.
        val display = computeNowPlayingDisplay("Radio X", icyTitle = "Radio X", icyArtist = null)
        assertEquals(NowPlayingDisplay("Radio X", "Live Radio"), display)
    }

    @Test
    fun icyArtistEqualToStationNameIsIgnored() {
        // ICY with no "artist - title" separator: PlayerService sets artist = station name.
        val display = computeNowPlayingDisplay("Radio X", icyTitle = "Some Show", icyArtist = "Radio X")
        assertEquals(NowPlayingDisplay("Radio X", "Some Show"), display)
    }

}
