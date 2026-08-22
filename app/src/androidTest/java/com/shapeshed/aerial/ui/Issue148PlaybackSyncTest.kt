package com.shapeshed.aerial.ui

import androidx.datastore.preferences.core.edit
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.toPlayableMediaItem
import com.shapeshed.aerial.data.LAST_PLAYED_STATION_KEY
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.lastPlayedStationSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Issue148PlaybackSyncTest {

    @Test
    fun stationTransitionClearsPreviousMetadataInTheSameSnapshot() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<AerialApp>()
        val left = station("Metadata left", "metadata-left")
        val right = station("Metadata right", "metadata-right")
        val viewModel = withContext(Dispatchers.Main) {
            MainViewModel(app, app.repository, app.registryRepository, app.settingsDataStore)
        }

        withContext(Dispatchers.Main) {
            viewModel.handlePlaybackEvents(left.toPlayableMediaItem(app), isPlaying = true)
            viewModel.handlePlaybackMetadata(title = "Left song", artist = "Left artist")
            assertEquals("Left song", viewModel.playbackUiState.value.trackTitle)

            viewModel.handlePlaybackEvents(right.toPlayableMediaItem(app), isPlaying = true)

            assertEquals(right.streamUrl, viewModel.playbackUiState.value.station?.streamUrl)
            assertEquals(null, viewModel.playbackUiState.value.trackTitle)
            assertEquals(null, viewModel.playbackUiState.value.trackArtist)
        }
    }

    @Test
    fun playbackSnapshotKeepsStationQueueAndPlayStateCoherent() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<AerialApp>()
        val leftId = app.repository.insertOrGetExisting(station("Snapshot left", "snapshot-left"))
        val rightId = app.repository.insertOrGetExisting(station("Snapshot right", "snapshot-right"))
        val left = app.repository.getById(leftId)!!
        val right = app.repository.getById(rightId)!!
        val queue = listOf(left, right)
        app.settingsDataStore.edit { preferences ->
            preferences.remove(LAST_PLAYED_STATION_KEY)
        }
        val viewModel = withContext(Dispatchers.Main) {
            MainViewModel(app, app.repository, app.registryRepository, app.settingsDataStore)
        }
        withTimeout(5_000) {
            viewModel.stations.first { stations -> stations.count { it.id in setOf(leftId, rightId) } == 2 }
        }

        withContext(Dispatchers.Main) {
            viewModel.handlePlaybackEvents(left.toPlayableMediaItem(app), isPlaying = false, queue = queue)
        }
        assertEquals(
            PlaybackUiState(left, isPlaying = false, isBuffering = false, queue = queue),
            viewModel.playbackUiState.value,
        )

        withContext(Dispatchers.Main) {
            viewModel.handlePlaybackEvents(
                mediaItem = right.toPlayableMediaItem(app),
                isPlaying = false,
                playbackState = Player.STATE_BUFFERING,
                playWhenReady = true,
                queue = queue,
            )
        }
        assertEquals(
            PlaybackUiState(right, isPlaying = false, isBuffering = true, queue = queue),
            viewModel.playbackUiState.value,
        )

        withContext(Dispatchers.Main) {
            viewModel.handlePlaybackEvents(right.toPlayableMediaItem(app), isPlaying = true, queue = queue)
        }
        assertEquals(
            PlaybackUiState(right, isPlaying = true, isBuffering = false, queue = queue),
            viewModel.playbackUiState.value,
        )
    }

    @Test
    fun immediatePlayStateAfterExternalTransitionPersistsTransitionedStation() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<AerialApp>()
        val left = station("Left station", "left")
        val right = station("Right station", "right")
        val leftId = app.repository.insertOrGetExisting(left)
        val rightId = app.repository.insertOrGetExisting(right)
        val savedLeft = app.repository.getById(leftId)!!
        val savedRight = app.repository.getById(rightId)!!
        app.settingsDataStore.edit { preferences ->
            preferences.remove(LAST_PLAYED_STATION_KEY)
        }

        val viewModel = withContext(Dispatchers.Main) {
            MainViewModel(app, app.repository, app.registryRepository, app.settingsDataStore)
        }
        val collection = launch(Dispatchers.Main) { viewModel.playbackUiState.collect {} }
        withTimeout(5_000) {
            viewModel.stations.first { stations ->
                stations.any { it.id == leftId } && stations.any { it.id == rightId }
            }
        }

        withContext(Dispatchers.Main) {
            viewModel.handlePlaybackEvents(
                mediaItem = savedLeft.toPlayableMediaItem(app),
                isPlaying = false,
            )
        }
        awaitCurrentStation(viewModel, leftId)
        collection.cancelAndJoin()
        // Lifecycle-aware Compose collection stops while the activity is sleeping. Let the
        // WhileSubscribed station state stop before the system media control changes station.
        delay(5_100)

        // Media3 batches these callbacks when a lock-screen/notification Next action
        // transitions an item and resumes it while the app UI is asleep. Issue #148 is the
        // dormant derived station value read by the immediately following playback callback.
        withContext(Dispatchers.Main) {
            viewModel.handlePlaybackEvents(
                mediaItem = savedRight.toPlayableMediaItem(app),
                isPlaying = true,
            )
        }

        val persisted = awaitPersistedStation(app, rightId)
        assertEquals(
            "The persisted station must follow the MediaSession transition",
            rightId,
            persisted,
        )
        assertEquals(savedRight, viewModel.playbackUiState.value.station)
        assertEquals(true, viewModel.playbackUiState.value.isPlaying)
    }

    private suspend fun awaitCurrentStation(viewModel: MainViewModel, id: Long) {
        withTimeout(5_000) {
            viewModel.playbackUiState.first { it.station?.id == id }
        }
    }

    private suspend fun awaitPersistedStation(app: AerialApp, expectedId: Long): Long {
        var lastId: Long? = null
        repeat(100) {
            val json = app.settingsDataStore.data.first()[LAST_PLAYED_STATION_KEY]
            val id = json?.let(::lastPlayedStationSnapshot)?.station?.id
            if (id == expectedId) return id
            lastId = id
            delay(10)
        }
        return lastId ?: error("No last-played station was persisted")
    }

    private fun station(name: String, providerId: String) = Station(
        name = name,
        streamUrl = "https://example.invalid/$providerId",
        isFavorite = true,
        provider = "issue-148-test",
        providerId = providerId,
    )
}
