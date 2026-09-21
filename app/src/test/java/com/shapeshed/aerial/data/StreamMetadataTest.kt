package com.shapeshed.aerial.data

import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamMetadataTest {
    @Test
    fun extractsIcyTitle() {
        val frames = streamMetadataFrames(Metadata(IcyInfo(byteArrayOf(), "Artist - Track", null)))

        assertEquals("Artist - Track", frames.icyTitle)
        assertNull(frames.id3Title)
        assertNull(frames.id3Artist)
        assertNull(frames.id3Artwork)
    }

    @Test
    fun extractsAndTrimsId3TitleAndArtist() {
        val frames = streamMetadataFrames(
            Metadata(
                TextInformationFrame("TIT2", null, "  Track  "),
                TextInformationFrame("TPE1", null, "  Artist  "),
                TextInformationFrame("TALB", null, "ignored"),
            ),
        )

        assertEquals("Track", frames.id3Title)
        assertEquals("Artist", frames.id3Artist)
    }

    @Test
    fun blankId3TitleIsIgnored() {
        val frames = streamMetadataFrames(Metadata(TextInformationFrame("TIT2", null, "   ")))

        assertNull(frames.id3Title)
    }

    @Test
    fun extractsId3Artwork() {
        val artwork = byteArrayOf(1, 2, 3)

        val frames = streamMetadataFrames(Metadata(ApicFrame("image/png", null, 3, artwork)))

        assertArrayEquals(artwork, frames.id3Artwork)
    }

    @Test
    fun emptyBundleHasNoFrames() {
        val frames = streamMetadataFrames(Metadata())

        assertNull(frames.icyTitle)
        assertNull(frames.id3Title)
        assertNull(frames.id3Artist)
        assertNull(frames.id3Artwork)
    }
}
