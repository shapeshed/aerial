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
}
