package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateSyncTest {
    @Test
    fun identityIsEmptyWhenNoStationIsPlaying() {
        val identity = PlaybackStationIdentity.of(null)

        assertNull(identity.stationId)
        assertNull(identity.ephemeralStation)
    }

    @Test
    fun identityKeepsEphemeralStationInMemory() {
        val ephemeral = station(id = 0, name = "Ephemeral")

        val identity = PlaybackStationIdentity.of(ephemeral)

        assertNull(identity.stationId)
        assertEquals(ephemeral, identity.ephemeralStation)
    }

    @Test
    fun identityKeepsPersistedRowId() {
        val saved = station(id = 7, name = "Saved")

        val identity = PlaybackStationIdentity.of(saved)

        assertEquals(7L, identity.stationId)
        assertNull(identity.ephemeralStation)
    }

    @Test
    fun stationChangeIsDetectedByIdentityNotValue() {
        val playing = station(id = 7, name = "Saved")

        assertFalse(playbackStationChanged(null, null))
        assertTrue(playbackStationChanged(null, playing))
        assertTrue(playbackStationChanged(playing, null))
        assertFalse(playbackStationChanged(playing, playing.copy(name = "Renamed")))
        assertTrue(playbackStationChanged(playing, station(id = 8, name = "Other")))
    }

    @Test
    fun clearingPerStationStateKeepsSessionFields() {
        val playing = station(id = 7, name = "Saved")
        val state = PlaybackUiState(
            station = playing,
            isPlaying = true,
            queue = listOf(playing),
            trackTitle = "Title",
            trackArtist = "Artist",
            bitrateKbps = 128,
            error = "boom",
        )

        val cleared = state.clearedPerStationState()

        assertNull(cleared.trackTitle)
        assertNull(cleared.trackArtist)
        assertNull(cleared.bitrateKbps)
        assertNull(cleared.error)
        assertEquals(playing, cleared.station)
        assertTrue(cleared.isPlaying)
        assertEquals(listOf(playing), cleared.queue)
    }

    @Test
    fun metadataFilterNamesIncludeCurrentQueueAndAllStations() {
        val playing = station(id = 7, name = "Playing")
        val queued = station(id = 8, name = "Queued")
        val other = station(id = 9, name = "Other")
        val state = PlaybackUiState(station = playing, queue = listOf(queued))

        val names = state.stationNamesForMetadataFilter(listOf(other, playing))

        assertEquals(listOf("Playing", "Queued", "Other", "Playing"), names)
    }

    @Test
    fun metadataForADifferentStationIsDeferred() {
        val playing = station(id = 7, name = "Playing")
        val incoming = station(id = 8, name = "Incoming")

        assertEquals(MetadataArrival.ApplyNow, metadataArrival(null, playing))
        assertEquals(MetadataArrival.ApplyNow, metadataArrival(null, null))
        assertEquals(MetadataArrival.Defer(incoming), metadataArrival(incoming, null))
        assertEquals(MetadataArrival.Defer(incoming), metadataArrival(incoming, playing))
        assertEquals(MetadataArrival.ApplyNow, metadataArrival(playing, playing))
    }

    @Test
    fun pendingMetadataMatchesOnlyItsOwnStation() {
        val pendingStation = station(id = 8, name = "Pending")
        val pending = PendingPlaybackMetadata(pendingStation, "Title", "Artist")
        val none: PendingPlaybackMetadata? = null

        assertNull(none.matching(pendingStation))
        assertEquals(pending, pending.matching(pendingStation))
        assertNull(pending.matching(station(id = 9, name = "Other")))
    }

    @Test
    fun reducePlaybackSyncKeepsCurrentValuesWhenNotProvided() {
        val playing = station(id = 7, name = "Playing")
        val queued = station(id = 8, name = "Queued")
        val state = PlaybackUiState(station = playing, isPlaying = false, queue = listOf(queued))

        val reduced = state.reducePlaybackSync(
            station = null,
            isPlaying = true,
            isBuffering = true,
            queue = emptyList(),
        )

        assertEquals(playing, reduced.station)
        assertEquals(listOf(queued), reduced.queue)
        assertTrue(reduced.isPlaying)
        assertTrue(reduced.isBuffering)
    }

    @Test
    fun reducePlaybackSyncReplacesStationAndQueueWhenProvided() {
        val playing = station(id = 7, name = "Playing")
        val next = station(id = 8, name = "Next")
        val state = PlaybackUiState(station = playing, queue = listOf(playing))

        val reduced = state.reducePlaybackSync(
            station = next,
            isPlaying = true,
            isBuffering = false,
            queue = listOf(next),
        )

        assertEquals(next, reduced.station)
        assertEquals(listOf(next), reduced.queue)
        assertFalse(reduced.isBuffering)
    }

    private fun station(id: Long, name: String): Station = Station(
        id = id,
        name = name,
        streamUrl = "https://stream.example/$id",
    )
}
