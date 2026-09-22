package com.shapeshed.aerial.ui

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(UnstableApi::class)
class TrackBitrateTest {
    @Test
    fun selectedAudioTrackReportsItsBitrateInKbps() {
        assertEquals(128, currentBitrateKbps(tracks(audioGroup(bitrate = 128_000))))
    }

    @Test
    fun videoTrackIsIgnored() {
        assertNull(currentBitrateKbps(tracks(videoGroup(bitrate = 5_000_000))))
    }

    @Test
    fun audioIsFoundWhenAVideoGroupComesFirst() {
        assertEquals(96, currentBitrateKbps(tracks(videoGroup(5_000_000), audioGroup(bitrate = 96_000))))
    }

    @Test
    fun unselectedAudioTrackIsIgnored() {
        assertNull(currentBitrateKbps(tracks(audioGroup(bitrate = 128_000, selected = false))))
    }

    @Test
    fun unspecifiedBitrateIsIgnored() {
        assertNull(currentBitrateKbps(tracks(audioGroup(bitrate = Format.NO_VALUE))))
    }

    @Test
    fun zeroBitrateIsIgnored() {
        assertNull(currentBitrateKbps(tracks(audioGroup(bitrate = 0))))
    }

    @Test
    fun subKbpsBitrateRoundsUpToOne() {
        assertEquals(1, currentBitrateKbps(tracks(audioGroup(bitrate = 500))))
    }

    @Test
    fun noGroupsReportsNothing() {
        assertNull(currentBitrateKbps(Tracks(emptyList())))
    }

    private fun tracks(vararg groups: Tracks.Group) = Tracks(groups.toList())

    private fun audioGroup(bitrate: Int, selected: Boolean = true) = group(MimeTypes.AUDIO_AAC, bitrate, selected)

    private fun videoGroup(bitrate: Int) = group(MimeTypes.VIDEO_H264, bitrate, selected = true)

    private fun group(mimeType: String, bitrate: Int, selected: Boolean): Tracks.Group {
        val format = Format.Builder()
            .setSampleMimeType(mimeType)
            .apply {
                if (bitrate != Format.NO_VALUE) {
                    setAverageBitrate(bitrate)
                    setPeakBitrate(bitrate)
                }
            }
            .build()
        // Not adaptive, supported, and selected per the test's `selected` flag.
        return Tracks.Group(
            TrackGroup(format),
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(selected),
        )
    }
}
