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

    private fun station(id: Long, name: String): Station = Station(
        id = id,
        name = name,
        streamUrl = "https://stream.example/$id",
    )
}
