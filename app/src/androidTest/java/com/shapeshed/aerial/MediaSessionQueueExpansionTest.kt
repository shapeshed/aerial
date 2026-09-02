package com.shapeshed.aerial

import androidx.media3.common.MediaItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaSessionQueueExpansionTest {
    @Test
    fun notificationSkipCommandsAreHiddenForSingleItemQueue() = runBlocking {
        withContext(Dispatchers.Main) {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<AerialApp>()
            val player = androidx.media3.exoplayer.ExoPlayer.Builder(context).build()
            try {
                player.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                player.setMediaItem(item("station-1"))

                val sessionPlayer = createSessionPlayer(player)

                assertEquals(false, sessionPlayer.isCommandAvailable(
                    androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS,
                ))
                assertEquals(false, sessionPlayer.isCommandAvailable(
                    androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT,
                ))
                assertEquals(false, sessionPlayer.availableCommands.contains(
                    androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS,
                ))
                assertEquals(false, sessionPlayer.availableCommands.contains(
                    androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT,
                ))
            } finally {
                player.release()
            }
        }
    }

    @Test
    fun notificationSkipCommandsRemainAvailableForMultiItemQueue() = runBlocking {
        withContext(Dispatchers.Main) {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<AerialApp>()
            val player = androidx.media3.exoplayer.ExoPlayer.Builder(context).build()
            try {
                player.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                player.setMediaItems(listOf(item("station-1"), item("station-2")))

                val sessionPlayer = createSessionPlayer(player)

                assertEquals(true, sessionPlayer.isCommandAvailable(
                    androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS,
                ))
                assertEquals(true, sessionPlayer.isCommandAvailable(
                    androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT,
                ))
                assertEquals(true, sessionPlayer.availableCommands.contains(
                    androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS,
                ))
                assertEquals(true, sessionPlayer.availableCommands.contains(
                    androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT,
                ))
            } finally {
                player.release()
            }
        }
    }

    @Test
    fun browseControllerGetsFreshSiblingQueueAndTappedIndex() = runBlocking {
        val tapped = item("station-2")
        val siblings = listOf(item("station-1"), tapped, item("station-3"))

        val result = expandControllerQueue(
            mediaItems = listOf(tapped),
            startIndex = -1,
            startPositionMs = 123L,
            controllerPackage = "com.google.android.projection.gearhead",
            appPackage = "com.shapeshed.aerial",
            parentIdForMediaId = { if (it == "station-2") "favorites" else null },
            childrenForParent = { if (it == "favorites") siblings else null },
            resolveMediaItem = { null },
        )

        assertEquals(listOf("station-1", "station-2", "station-3"), result.mediaItems.map(MediaItem::mediaId))
        assertEquals(1, result.startIndex)
        assertEquals(123L, result.startPositionMs)
    }

    @Test
    fun phoneControllerDoesNotExpandSingleItemQueue() = runBlocking {
        val tapped = item("station-2")
        val resolved = item("station-2-resolved")

        val result = expandControllerQueue(
            mediaItems = listOf(tapped),
            startIndex = -1,
            startPositionMs = 0L,
            controllerPackage = "com.shapeshed.aerial",
            appPackage = "com.shapeshed.aerial",
            parentIdForMediaId = { "favorites" },
            childrenForParent = { listOf(item("unexpected-sibling")) },
            resolveMediaItem = { resolved },
        )

        assertEquals(listOf("station-2-resolved"), result.mediaItems.map(MediaItem::mediaId))
        assertEquals(-1, result.startIndex)
    }

    private fun item(id: String) = MediaItem.Builder()
        .setMediaId(id)
        .setUri("https://example.invalid/$id")
        .build()
}
