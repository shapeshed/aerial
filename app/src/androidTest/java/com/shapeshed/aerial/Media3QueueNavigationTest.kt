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
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Media3QueueNavigationTest {

    @Test
    fun nextAndPreviousNavigateAndWrapWhilePausedOrPlaying() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<AerialApp>()
        withContext(Dispatchers.Main) {
            val player = ExoPlayer.Builder(context).build()
            try {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.setMediaItems(
                    listOf("left", "right").map { id ->
                        MediaItem.Builder()
                            .setMediaId(id)
                            .setUri("https://example.invalid/$id")
                            .build()
                    },
                )

                assertEquals("left", player.currentMediaItem?.mediaId)
                player.seekToNextMediaItem()
                assertEquals("right", player.currentMediaItem?.mediaId)
                player.seekToNextMediaItem()
                assertEquals("left", player.currentMediaItem?.mediaId)

                player.playWhenReady = true
                player.seekToPreviousMediaItem()
                assertEquals("right", player.currentMediaItem?.mediaId)
                player.seekToPreviousMediaItem()
                assertEquals("left", player.currentMediaItem?.mediaId)
            } finally {
                player.release()
            }
        }
    }
}
