package com.shapeshed.aerial.ui

import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Test

class ArtworkPathRecoveryTest {
    @Test
    fun deletesCachedArtworkAndMediaCompanion() {
        val directory = Files.createTempDirectory("aerial-artwork").toFile()
        val artwork = directory.resolve("station.png").apply { writeBytes(byteArrayOf(1)) }
        val companion = directory.resolve("station_media.png").apply { writeBytes(byteArrayOf(1)) }

        deleteStationArtworkFiles(artwork.absolutePath)

        assertFalse(artwork.exists())
        assertFalse(companion.exists())
        directory.delete()
    }
}
