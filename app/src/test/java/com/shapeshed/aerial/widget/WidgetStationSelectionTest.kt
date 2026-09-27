package com.shapeshed.aerial.widget

import com.shapeshed.aerial.data.LastPlayedStationSnapshot
import com.shapeshed.aerial.data.PlaybackSnapshotStore
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.testing.MemoryDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers which station the widget shows, and what its play button starts, once nothing is loaded.
 *
 * Both rules were unreachable from a JVM test while the widget resolved its collaborators by
 * casting `Application`: they needed a live `AerialApp`. Now that they take a repository and a
 * snapshot store, the whole decision runs against in-memory fakes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetStationSelectionTest {

    private val rock = station(1, "Rock Radio", "rock")
    private val jazz = station(2, "Jazz Radio", "jazz")
    private val pop = station(3, "Pop Radio", "pop")

    // --- which station the widget shows ---

    @Test
    fun theStationTheSessionIsPlayingWinsOverEverythingElse() = runTest {
        val snapshot = snapshotStore(rock)
        snapshot.write(jazz, listOf(jazz, rock))

        val selected = selectedStation(listOf(rock, jazz, pop), mediaId = "2", snapshotStore = snapshot)

        assertEquals(jazz, selected)
    }

    @Test
    fun thePersistedStationIsShownWhenTheSessionIsPlayingSomethingUnsaved() = runTest {
        val snapshot = snapshotStore(jazz)

        val selected = selectedStation(listOf(rock), mediaId = "999", snapshotStore = snapshot)

        assertEquals(jazz, selected)
    }

    @Test
    fun theFirstFavouriteIsShownWhenNothingIsPlayingOrPersisted() = runTest {
        val selected = selectedStation(listOf(rock, jazz), mediaId = null, snapshotStore = snapshotStore())

        assertEquals(rock, selected)
    }

    @Test
    fun aPersistedStationThatIsNoLongerFavouriteIsStillShown() = runTest {
        // Unfavouriting keeps the station playable, so the widget must keep showing it rather
        // than silently falling back to an unrelated favourite.
        val snapshot = snapshotStore(pop)

        val selected = selectedStation(listOf(rock), mediaId = null, snapshotStore = snapshot)

        assertEquals(pop, selected)
    }

    @Test
    fun noStationIsSelectedWhenThereAreNoFavouritesAndNoSnapshot() = runTest {
        val selected = selectedStation(emptyList(), mediaId = null, snapshotStore = snapshotStore())

        assertNull(selected)
    }

    // --- what the play button starts ---

    @Test
    fun thePersistedQueueIsRestoredWithThePersistedStationSelected() {
        val snapshot = LastPlayedStationSnapshot(pop, listOf(rock, jazz, pop))

        val plan = widgetRestorePlan(snapshot, favorites = emptyList())

        assertEquals(listOf(rock, jazz, pop), plan?.stations)
        assertEquals(2, plan?.selectedIndex)
    }

    @Test
    fun anEmptyPersistedQueueFallsBackToThePersistedStationAlone() {
        val snapshot = LastPlayedStationSnapshot(pop, queue = emptyList())

        val plan = widgetRestorePlan(snapshot, favorites = emptyList())

        assertEquals(listOf(pop), plan?.stations)
        assertEquals(0, plan?.selectedIndex)
    }

    @Test
    fun withNoSnapshotTheFirstFavouriteIsRestored() {
        val plan = widgetRestorePlan(snapshot = null, favorites = listOf(jazz, pop))

        assertEquals(listOf(jazz), plan?.stations)
        assertEquals(0, plan?.selectedIndex)
    }

    @Test
    fun aPersistedStationMissingFromItsQueueResumesFromTheStart() {
        // The queue can outlive the station it was built from (it was deleted elsewhere).
        val snapshot = LastPlayedStationSnapshot(pop, listOf(rock, jazz))

        val plan = widgetRestorePlan(snapshot, favorites = emptyList())

        assertEquals(listOf(rock, jazz), plan?.stations)
        assertEquals(0, plan?.selectedIndex)
    }

    @Test
    fun anEphemeralSnapshotStationIsMatchedByStreamUrl() {
        // A station played without saving has id 0, so matches() falls back to the stream URL.
        val ephemeral = pop.copy(id = 0L)
        val snapshot = LastPlayedStationSnapshot(ephemeral, listOf(rock, pop))

        val plan = widgetRestorePlan(snapshot, favorites = emptyList())

        assertEquals(1, plan?.selectedIndex)
    }

    @Test
    fun thereIsNothingToRestoreWithoutASnapshotOrFavourites() {
        assertNull(widgetRestorePlan(snapshot = null, favorites = emptyList()))
    }

    private suspend fun snapshotStore(station: Station? = null): PlaybackSnapshotStore {
        val store = PlaybackSnapshotStore(MemoryDataStore())
        station?.let { store.write(it) }
        return store
    }

    private fun station(id: Long, name: String, providerId: String) = Station(
        id = id,
        name = name,
        streamUrl = "https://example.invalid/$providerId",
        isFavorite = true,
        provider = "widget-test",
        providerId = providerId,
    )
}
