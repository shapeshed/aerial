package com.shapeshed.aerial

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaybackQueuePolicyTest {
    @Test
    fun externalControllerCannotSubmitAnUnresolvedMediaItem() {
        val incoming = MediaItem.Builder().setMediaId("unknown").build()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                expandControllerQueue(
                    mediaItems = listOf(incoming),
                    startIndex = 0,
                    startPositionMs = 0L,
                    controllerPackage = "external.controller",
                    appPackage = "com.shapeshed.aerial",
                    parentIdForMediaId = { null },
                    childrenForParent = { emptyList() },
                    resolveMediaItem = { null },
                )
            }
        }
    }

    @Test
    fun ownControllerMaySubmitAnEphemeralMediaItem() {
        val incoming = MediaItem.Builder().setMediaId("ephemeral").build()

        val result: MediaSession.MediaItemsWithStartPosition = runBlocking {
            expandControllerQueue(
                mediaItems = listOf(incoming),
                startIndex = 0,
                startPositionMs = 0L,
                controllerPackage = "com.shapeshed.aerial",
                appPackage = "com.shapeshed.aerial",
                parentIdForMediaId = { null },
                childrenForParent = { emptyList() },
                resolveMediaItem = { null },
            )
        }

        assertSame(incoming, result.mediaItems.single())
    }
}
