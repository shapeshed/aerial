package com.shapeshed.aerial.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkSurfaceTest {
    @Test
    fun transparentArtworkUsesLightPlateInDarkTheme() {
        assertTrue(artworkNeedsLightPlate(true, isArtworkLight = true, prefersLightPlate = false, hasTransparentMargin = true))
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
}
