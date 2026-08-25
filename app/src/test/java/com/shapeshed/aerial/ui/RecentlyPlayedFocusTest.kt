package com.shapeshed.aerial.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentlyPlayedFocusTest {
    @Test
    fun newlyInsertedFrontItemRequestsRowFocus() {
        assertTrue(
            shouldFocusRecentlyPlayedItem(
                previousKeys = listOf("bbc:one", "bbc:four"),
                currentKeys = listOf("rinse:uk", "bbc:one", "bbc:four"),
            ),
        )
    }

    @Test
    fun initialLoadDoesNotRequestAnAnimatedScroll() {
        assertFalse(
            shouldFocusRecentlyPlayedItem(
                previousKeys = emptyList(),
                currentKeys = listOf("bbc:one"),
            ),
        )
    }

    @Test
    fun reorderedFavoriteFrontItemRequestsGridFocus() {
        assertTrue(
            shouldFocusFavoritesItem(
                previousKeys = listOf("1", "2", "3"),
                currentKeys = listOf("2", "1", "3"),
            ),
        )
    }

    @Test
    fun reorderedFavoriteScrollsHomeEvenWhenThePreviousFirstItemIsOffScreen() {
        assertTrue(
            shouldScrollFavoritesToTop(
                leadingItemChanged = true,
                isScrollInProgress = false,
            ),
        )
    }
}
