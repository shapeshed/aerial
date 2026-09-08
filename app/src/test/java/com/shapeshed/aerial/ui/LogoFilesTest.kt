package com.shapeshed.aerial.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class LogoFilesTest {
    @Test
    fun svgLogoUsesRasterCompanionForSystemArtwork() {
        assertEquals(
            File("/tmp/bbc-radio-1_media.png"),
            mediaArtworkFileForSystem(File("/tmp/bbc-radio-1.svg")),
        )
    }

    @Test
    fun opaqueSquareArtworkNeedsThreeContrastingEdgesToBeCircular() {
        assertEquals(
            false,
            looksLikeCircularArtwork(
                transparentCorners = 0,
                cornersMatch = true,
                contrastingEdges = 2,
            ),
        )
        assertEquals(
            true,
            looksLikeCircularArtwork(
                transparentCorners = 0,
                cornersMatch = true,
                contrastingEdges = 3,
            ),
        )
    }

    @Test
    fun transparentCircularArtworkCanUseTwoContrastingEdges() {
        assertEquals(
            true,
            looksLikeCircularArtwork(
                transparentCorners = 4,
                cornersMatch = false,
                contrastingEdges = 2,
            ),
        )
    }
}
