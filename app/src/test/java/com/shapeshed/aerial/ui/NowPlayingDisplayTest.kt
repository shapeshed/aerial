package com.shapeshed.aerial.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingDisplayTest {
    @Test
    fun miniPlayerUsesTrackTitleThenArtist() {
        assertEquals(
            TrackDisplay("2. AGO", "Tiffany Foxx"),
            computeTrackDisplay("Worldwide FM", "2. AGO", "Tiffany Foxx"),
        )
    }

    @Test
    fun miniPlayerFallsBackToStationAndLiveRadioWithoutIcy() {
        assertEquals(
            TrackDisplay("Worldwide FM", "Live Radio"),
            computeTrackDisplay("Worldwide FM", null, null),
        )
    }

    @Test
    fun miniPlayerShowsTheStationNameAsArtistWhenOnlyATitleIsKnown() {
        // Regression: an ICY title with no "artist - title" separator makes PlayerService set the
        // artist to the station name. The mini player must match the notification rather than
        // falling back to "Live Radio".
        assertEquals(
            TrackDisplay("Now playing info goes here", "Kool FM"),
            computeTrackDisplay("Kool FM", "Now playing info goes here", null),
        )
    }

    @Test
    fun miniPlayerShowsTheStationNameAsArtistWhenTheStreamEchoesIt() {
        assertEquals(
            TrackDisplay("Some Show", "Radio X"),
            computeTrackDisplay("Radio X", "Some Show", "Radio X"),
        )
    }

    @Test
    fun noMetadataShowsStationNameAndLiveRadio() {
        val display = computeNowPlayingDisplay("Radio X", icyTitle = null)
        assertEquals(NowPlayingDisplay("Radio X", "Live Radio"), display)
    }

    @Test
    fun icyArtistAndTitleShown() {
        val display = computeNowPlayingDisplay(
            "KISS Dance",
            icyTitle = "Slow Burner",
            icyArtist = "Interplanetary Criminal",
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
