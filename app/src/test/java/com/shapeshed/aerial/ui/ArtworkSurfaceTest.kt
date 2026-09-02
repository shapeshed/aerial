package com.shapeshed.aerial.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkSurfaceTest {
    @Test
    fun darkTransparentArtworkUsesLightPlateInDarkTheme() {
        assertTrue(artworkNeedsLightPlate(true, isArtworkLight = false, prefersLightPlate = false, hasTransparentMargin = true))
    }

    @Test
    fun lightTransparentArtworkKeepsDarkSurfaceInDarkTheme() {
        assertFalse(artworkNeedsLightPlate(true, isArtworkLight = true, prefersLightPlate = false, hasTransparentMargin = true))
    }

    @Test
    fun opaqueLightArtworkKeepsDarkPlateInDarkThemeUnlessRequested() {
        assertFalse(artworkNeedsLightPlate(true, isArtworkLight = true, prefersLightPlate = false, hasTransparentMargin = false))
        assertTrue(artworkNeedsLightPlate(true, isArtworkLight = true, prefersLightPlate = true, hasTransparentMargin = false))
    }

    @Test
    fun lightThemeDoesNotForceLightPlate() {
        assertFalse(artworkNeedsLightPlate(false, isArtworkLight = false, prefersLightPlate = false, hasTransparentMargin = true))
    }

    @Test
    fun contrastPlateIsOnlyUsedWhenArtworkConflictsWithTheme() {
        assertFalse(artworkNeedsContrastPlate(true, isArtworkLight = true, prefersLightPlate = false, hasTransparentMargin = true))
        assertTrue(artworkNeedsContrastPlate(true, isArtworkLight = false, prefersLightPlate = false, hasTransparentMargin = true))
        assertTrue(artworkNeedsContrastPlate(false, isArtworkLight = true, prefersLightPlate = false, hasTransparentMargin = true))
    }
}
