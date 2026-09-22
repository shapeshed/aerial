package com.shapeshed.aerial

import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionPaginationTest {
    private val items = (1..5).map { mediaItem("station-$it") }

    @Test
    fun nonPositivePageSizeReturnsEveryItem() {
        assertEquals(items, items.paginated(page = 7, pageSize = 0))
        assertEquals(items, items.paginated(page = 7, pageSize = -1))
    }

    @Test
    fun firstPageReturnsTheLeadingSlice() {
        assertEquals(listOf("station-1", "station-2"), items.paginated(page = 0, pageSize = 2).ids())
    }

    @Test
    fun finalPageIsTruncatedToTheRemainingItems() {
        assertEquals(listOf("station-5"), items.paginated(page = 2, pageSize = 2).ids())
    }

    @Test
    fun pagePastTheEndReturnsNothing() {
        assertTrue(items.paginated(page = 9, pageSize = 2).isEmpty())
    }

    @Test
    fun negativePageClampsToTheFirstPage() {
        assertEquals(listOf("station-1", "station-2"), items.paginated(page = -3, pageSize = 2).ids())
    }

    private fun List<MediaItem>.ids() = map(MediaItem::mediaId)

    private fun mediaItem(id: String) = MediaItem.Builder().setMediaId(id).build()
}
