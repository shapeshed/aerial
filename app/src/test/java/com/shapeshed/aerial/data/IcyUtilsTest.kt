package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IcyUtilsTest {
    @Test
    fun parsesArtistAndTitleAndTrimsWhitespace() {
        assertEquals("Artist" to "Track", parseIcyTitle("  Artist - Track  "))
    }

    @Test
    fun keepsAdditionalSeparatorsInTrackTitle() {
        assertEquals("Artist" to "Track - Remix", parseIcyTitle("Artist - Track - Remix"))
    }

    @Test
    fun parsesEqualsSeparatorUsedBySomeStations() {
        assertEquals("Barker" to "Stochtastic Drift", parseIcyTitle("Barker = Stochtastic Drift"))
    }

    @Test
    fun commonParserNormalizesCombinedAndSeparateMetadata() {
        assertEquals(
            ParsedTrackMetadata("Barker", "Stochtastic Drift"),
            parseTrackMetadata("Barker = Stochtastic Drift"),
        )
        assertEquals(
            ParsedTrackMetadata("Barker", "Stochtastic Drift"),
            parseTrackMetadata("Stochtastic Drift", "Barker"),
        )
    }

    @Test
    fun treatsMissingOrLeadingSeparatorAsUntitledArtist() {
        assertEquals(null to "Live Radio", parseIcyTitle("Live Radio"))
        assertEquals(null to "- Track", parseIcyTitle(" - Track"))
    }
}
