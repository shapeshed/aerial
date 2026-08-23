package com.shapeshed.aerial

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerServiceQueueRecoveryTest {
    @Test
    fun failedCurrentStationRecoveryPreservesQueueForNextStation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<AerialApp>()
        withContext(Dispatchers.Main) {
            val player = ExoPlayer.Builder(context).build()
            try {
                player.setMediaItems(
                    listOf("failed", "next").map { id ->
                        MediaItem.Builder()
                            .setMediaId(id)
                            .setUri("https://example.invalid/$id")
                            .build()
                    },
                    /* startIndex = */ 0,
                    /* startPositionMs = */ 0L,
                )
                player.prepare()

                reconnectPlayerAfterError(player, shouldResume = false)

                assertEquals(2, player.mediaItemCount)
                assertEquals("failed", player.currentMediaItem?.mediaId)
                player.seekToNextMediaItem()
                assertEquals("next", player.currentMediaItem?.mediaId)
            } finally {
                player.release()
            }
        }
    }
}
