package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the decision a player event implies, without a live `MediaController`.
 *
 * These transitions used to be computed and applied inside `MainViewModel`, so the only way to
 * reach them was a mocked player with `handlePlaybackEvents` — which cannot distinguish "the
 * reducer decided to clear the bitrate" from "the reducer forgot to".
 */
class PlaybackTransitionTest {

    private val rock = station(1, "Rock Radio")
    private val jazz = station(2, "Jazz Radio")

    // --- station changes ---

    @Test
    fun aNewStationClearsThePreviousStationsTransientState() {
        val current = PlaybackUiState(
            station = rock,
            isPlaying = true,
            trackTitle = "Old song",
            trackArtist = "Old artist",
            bitrateKbps = 128,
        )

        val transition =
            reducePlaybackTransition(current, jazz, queue = emptyList(), isPlaying = true, isBuffering = false)

        assertTrue(transition.stationChanged)
        assertEquals(jazz, transition.state.station)
        assertNull("title belongs to the old station", transition.state.trackTitle)
        assertNull("artist belongs to the old station", transition.state.trackArtist)
        assertNull("bitrate belongs to the old station", transition.state.bitrateKbps)
    }

    @Test
    fun theSameStationKeepsItsMetadata() {
        val current = PlaybackUiState(
            station = rock,
            isPlaying = true,
            trackTitle = "Current song",
            bitrateKbps = 128,
        )

        val transition =
            reducePlaybackTransition(current, rock, queue = emptyList(), isPlaying = true, isBuffering = false)

        assertFalse(transition.stationChanged)
        assertEquals("Current song", transition.state.trackTitle)
        assertEquals(128, transition.state.bitrateKbps)
    }

    @Test
    fun aNewBitrateLandsAfterTheOldOneIsCleared() {
        // Regression-shaped: clearing must not wipe the bitrate the same transition reports.
        val current = PlaybackUiState(station = rock, isPlaying = true, bitrateKbps = 128)

        val transition = reducePlaybackTransition(
            current = current,
            station = jazz,
            queue = emptyList(),
            isPlaying = true,
            isBuffering = false,
            bitrateKbps = 320,
        )

        assertTrue(transition.stationChanged)
        assertEquals(320, transition.state.bitrateKbps)
    }

    @Test
    fun transportStateIsCarriedThrough() {
        val transition = reducePlaybackTransition(
            current = PlaybackUiState(),
            station = rock,
            queue = emptyList(),
            isPlaying = false,
            isBuffering = true,
        )

        assertFalse(transition.state.isPlaying)
        assertTrue(transition.state.isBuffering)
    }

    // --- identity ---

    @Test
    fun aPersistedStationReportsItsRowId() {
        val transition = reducePlaybackTransition(PlaybackUiState(), rock, emptyList(), true, false)

        assertEquals(1L, transition.identity.stationId)
        assertNull(transition.identity.ephemeralStation)
    }

    @Test
    fun anUnsavedStationIsHeldAsEphemeralRatherThanById() {
        // An ephemeral station has id 0, which is not a row; keying on it would make the
        // favourites logic look for a station that does not exist.
        val ephemeral = rock.copy(id = 0L)

        val transition = reducePlaybackTransition(PlaybackUiState(), ephemeral, emptyList(), true, false)

        assertNull(transition.identity.stationId)
        assertEquals(ephemeral, transition.identity.ephemeralStation)
    }

    @Test
    fun noStationClearsTheIdentity() {
        val transition = reducePlaybackTransition(PlaybackUiState(station = rock), null, emptyList(), false, false)

        assertNull(transition.identity.stationId)
        assertNull(transition.identity.ephemeralStation)
    }

    // --- persistence ---

    @Test
    fun aResolvedStationAsksForTheSnapshotToBePersisted() {
        val transition = reducePlaybackTransition(PlaybackUiState(), rock, emptyList(), true, false)

        assertTrue(transition.shouldPersist)
    }

    @Test
    fun anUnresolvedStationDoesNotPersist() {
        val transition = reducePlaybackTransition(PlaybackUiState(station = rock), null, emptyList(), false, false)

        assertFalse("nothing to persist when no station is resolved", transition.shouldPersist)
    }

    @Test
    fun persistenceCanBeSuppressed() {
        // Used while restoring, where writing the snapshot back would overwrite the very
        // snapshot being restored.
        val transition = reducePlaybackTransition(
            current = PlaybackUiState(),
            station = rock,
            queue = emptyList(),
            isPlaying = false,
            isBuffering = false,
            suppressPersist = true,
        )

        assertFalse(transition.shouldPersist)
    }

    // --- metadata that arrived ahead of its station ---

    @Test
    fun pendingMetadataForTheNewStationIsApplied() {
        // Media3 can deliver metadata for the next station before the transition completes, so
        // the reducer has to hand it back for the caller to apply.
        val pending = PendingPlaybackMetadata(jazz, "Queued up", "Some artist")

        val transition = reducePlaybackTransition(
            current = PlaybackUiState(station = rock),
            station = jazz,
            queue = emptyList(),
            isPlaying = true,
            isBuffering = false,
            pendingMetadata = pending,
        )

        assertEquals(pending, transition.applyPendingMetadata)
    }

    @Test
    fun pendingMetadataForADifferentStationIsNotApplied() {
        val pending = PendingPlaybackMetadata(jazz, "Wrong station", "Some artist")

        val transition = reducePlaybackTransition(
            current = PlaybackUiState(station = rock),
            station = rock,
            queue = emptyList(),
            isPlaying = true,
            isBuffering = false,
            pendingMetadata = pending,
        )

        assertNull(transition.applyPendingMetadata)
    }

    @Test
    fun pendingMetadataIsIgnoredWhenNoStationResolved() {
        val pending = PendingPlaybackMetadata(jazz, "Orphan", "Some artist")

        val transition = reducePlaybackTransition(
            current = PlaybackUiState(),
            station = null,
            queue = emptyList(),
            isPlaying = false,
            isBuffering = false,
            pendingMetadata = pending,
        )

        assertNull(transition.applyPendingMetadata)
    }

    @Test
    fun anEphemeralPendingStationMatchesThePersistedOne() {
        // The same station arrives as a saved row and as the id-0 copy from the session, so the
        // match has to go through Station.matches rather than id equality.
        val pending = PendingPlaybackMetadata(jazz.copy(id = 0L), "Song", "Artist")

        val transition = reducePlaybackTransition(
            current = PlaybackUiState(station = rock),
            station = jazz,
            queue = emptyList(),
            isPlaying = true,
            isBuffering = false,
            pendingMetadata = pending,
        )

        assertEquals(pending, transition.applyPendingMetadata)
    }

    // --- errors ---

    @Test
    fun anErrorStopsClaimingToPlayOrBuffer() {
        val errored = reducePlaybackError(
            PlaybackUiState(isPlaying = true, isBuffering = true, station = rock),
            "Could not connect",
        )

        assertFalse(errored.isPlaying)
        assertFalse(errored.isBuffering)
        assertEquals("Could not connect", errored.error)
    }

    @Test
    fun anErrorLeavesTheStationAndQueueAlone() {
        // Losing the station on a transient network error would blank the Now Playing screen.
        val errored = reducePlaybackError(
            PlaybackUiState(station = rock, queue = listOf(rock, jazz)),
            "Could not connect",
        )

        assertEquals(rock, errored.station)
        assertEquals(listOf(rock, jazz), errored.queue)
    }

    // --- queue ---

    @Test
    fun aQueueIsAdoptedAndAnEmptyOneIsNot() {
        val current = PlaybackUiState(station = rock, queue = listOf(rock))

        val withQueue = reducePlaybackTransition(current, rock, listOf(rock, jazz), true, false)
        assertEquals(listOf(rock, jazz), withQueue.state.queue)

        // Media3 reports an empty queue for some single-item transitions; clearing the visible
        // queue there would make the favourites list vanish mid-playback.
        val withoutQueue = reducePlaybackTransition(withQueue.state, rock, emptyList(), true, false)
        assertEquals(listOf(rock, jazz), withoutQueue.state.queue)
    }

    private fun station(id: Long, name: String) = Station(
        id = id,
        name = name,
        streamUrl = "https://example.invalid/$name",
        isFavorite = true,
        provider = "transition-test",
        providerId = name.lowercase(),
    )
}
