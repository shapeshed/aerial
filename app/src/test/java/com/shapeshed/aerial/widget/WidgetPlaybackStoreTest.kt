package com.shapeshed.aerial.widget

import android.content.Context
import com.shapeshed.aerial.testing.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WidgetPlaybackStoreTest {
    private val preferences = FakeSharedPreferences()
    private val context = mock<Context>()

    @Test
    fun writePublishesPlaybackAndNavigationState() {
        WidgetPlaybackStore.write(
            preferences,
            mediaId = "station-1",
            isPlaying = true,
            canSkipPrevious = true,
            canSkipNext = false,
        )

        assertEquals(
            WidgetPlaybackState(
                mediaId = "station-1",
                isPlaying = true,
                trackTitle = null,
                trackArtist = null,
                canSkipPrevious = true,
                canSkipNext = false,
            ),
            WidgetPlaybackStore.read(preferences),
        )
    }

    @Test
    fun changingStationClearsThePreviousTrackMetadata() {
        WidgetPlaybackStore.writeMetadata(preferences, mediaId = "station-1", title = "Song", artist = "Artist")

        WidgetPlaybackStore.write(
            preferences,
            mediaId = "station-2",
            isPlaying = true,
            canSkipPrevious = false,
            canSkipNext = false,
        )

        val state = WidgetPlaybackStore.read(preferences)
        assertEquals("station-2", state.mediaId)
        assertNull(state.trackTitle)
        assertNull(state.trackArtist)
    }

    @Test
    fun stayingOnTheSameStationKeepsTrackMetadata() {
        WidgetPlaybackStore.writeMetadata(preferences, mediaId = "station-1", title = "Song", artist = "Artist")

        WidgetPlaybackStore.write(
            preferences,
            mediaId = "station-1",
            isPlaying = false,
            canSkipPrevious = true,
            canSkipNext = true,
        )

        val state = WidgetPlaybackStore.read(preferences)
        assertEquals("Song", state.trackTitle)
        assertEquals("Artist", state.trackArtist)
    }

    @Test
    fun blankMetadataIsRemovedRatherThanStored() {
        WidgetPlaybackStore.writeMetadata(preferences, mediaId = "station-1", title = "Song", artist = "Artist")

        WidgetPlaybackStore.writeMetadata(preferences, mediaId = "station-1", title = "  ", artist = "")

        val state = WidgetPlaybackStore.read(preferences)
        assertEquals("station-1", state.mediaId)
        assertNull(state.trackTitle)
        assertNull(state.trackArtist)
    }

    @Test
    fun nullMediaIdClearsTheTrackedStation() {
        WidgetPlaybackStore.writeMetadata(preferences, mediaId = "station-1", title = "Song", artist = "Artist")

        WidgetPlaybackStore.writeMetadata(preferences, mediaId = null, title = "Song", artist = "Artist")

        assertNull(WidgetPlaybackStore.read(preferences).mediaId)
    }

    @Test
    fun markStoppedOnlyClearsThePlayingFlag() {
        WidgetPlaybackStore.writeMetadata(preferences, mediaId = "station-1", title = "Song", artist = "Artist")
        WidgetPlaybackStore.write(
            preferences,
            mediaId = "station-1",
            isPlaying = true,
            canSkipPrevious = true,
            canSkipNext = true,
        )

        WidgetPlaybackStore.markStopped(preferences)

        val state = WidgetPlaybackStore.read(preferences)
        assertEquals(false, state.isPlaying)
        assertEquals("station-1", state.mediaId)
        assertEquals("Song", state.trackTitle)
    }

    @Test
    fun contextOverloadsShareTheWidgetPreferencesFile() {
        whenever(context.getSharedPreferences(any(), any())).thenReturn(preferences)

        WidgetPlaybackStore.write(
            context,
            mediaId = "station-1",
            isPlaying = true,
            canSkipPrevious = true,
            canSkipNext = false,
        )
        WidgetPlaybackStore.writeMetadata(context, mediaId = "station-1", title = "Song", artist = "Artist")

        val playing = WidgetPlaybackStore.read(context)
        assertEquals("station-1", playing.mediaId)
        assertEquals(true, playing.isPlaying)
        assertEquals("Song", playing.trackTitle)

        WidgetPlaybackStore.markStopped(context)

        assertEquals(false, WidgetPlaybackStore.read(context).isPlaying)
    }
}
