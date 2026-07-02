package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.NowPlayingInfo
import com.shapeshed.aerial.data.TrackInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingDisplayTest {

    @Test
    fun noMetadataShowsStationNameAndLiveRadio() {
        val display = computeNowPlayingDisplay("Radio X", info = null, icyTitle = null)
        assertEquals(NowPlayingDisplay("Radio X", "Live Radio"), display)
    }

    @Test
    fun trackShowsArtistThenTitle() {
        val info = NowPlayingInfo(stationId = 1, track = TrackInfo(artist = "Aphex Twin", title = "Xtal"))
        val display = computeNowPlayingDisplay("Radio X", info, icyTitle = null)
        assertEquals(NowPlayingDisplay("Aphex Twin", "Xtal"), display)
    }

    @Test
    fun trackWithoutArtistFallsBackToStationNameAsSubtitle() {
        val info = NowPlayingInfo(stationId = 1, track = TrackInfo(artist = "", title = "Some Track"))
        val display = computeNowPlayingDisplay("Radio X", info, icyTitle = null)
        assertEquals(NowPlayingDisplay("Some Track", "Radio X"), display)
    }

    @Test
    fun programmeShownWhenNoTrack() {
        val info = NowPlayingInfo(stationId = 1, programmeTitle = "Breakfast", programmeSubtitle = "with Zoe Ball")
        val display = computeNowPlayingDisplay("Radio X", info, icyTitle = null)
        assertEquals(NowPlayingDisplay("Breakfast", "with Zoe Ball"), display)
    }

    @Test
    fun programmeWithoutSubtitleFallsBackToStationName() {
        val info = NowPlayingInfo(stationId = 1, programmeTitle = "The News")
        val display = computeNowPlayingDisplay("Radio X", info, icyTitle = null)
        assertEquals(NowPlayingDisplay("The News", "Radio X"), display)
    }

    @Test
    fun icyArtistAndTitleShownWhenNoEnricher() {
        val display = computeNowPlayingDisplay(
            "KISS Dance", info = null, icyTitle = "Slow Burner", icyArtist = "Interplanetary Criminal",
        )
        assertEquals(NowPlayingDisplay("Interplanetary Criminal", "Slow Burner"), display)
    }

    @Test
    fun icyTitleOnlyFallsBackToStationName() {
        val display = computeNowPlayingDisplay("Radio X", info = null, icyTitle = "Some Show", icyArtist = null)
        assertEquals(NowPlayingDisplay("Some Show", "Radio X"), display)
    }

    @Test
    fun icyTitleEqualToStationNameFallsBackToLiveRadio() {
        // No track metadata: the media item title is the station name — must not show twice.
        val display = computeNowPlayingDisplay("Radio X", info = null, icyTitle = "Radio X", icyArtist = null)
        assertEquals(NowPlayingDisplay("Radio X", "Live Radio"), display)
    }

    @Test
    fun icyArtistEqualToStationNameIsIgnored() {
        // ICY with no "artist - title" separator: PlayerService sets artist = station name.
        val display = computeNowPlayingDisplay("Radio X", info = null, icyTitle = "Some Show", icyArtist = "Radio X")
        assertEquals(NowPlayingDisplay("Some Show", "Radio X"), display)
    }

    @Test
    fun icyTitleIgnoredWhenEnricherActiveButHasNoTrackOrProgramme() {
        // Enricher is running (info != null) but produced nothing — don't fall back to ICY.
        val info = NowPlayingInfo(stationId = 1)
        val display = computeNowPlayingDisplay("Radio X", info, icyTitle = "Stale ICY")
        assertEquals(NowPlayingDisplay("Radio X", "Live Radio"), display)
    }
}
