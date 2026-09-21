package com.shapeshed.aerial

import android.content.Context
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ArtworkProviderTest {
    @Test
    fun resolvesOnlyExistingDirectChildrenOfKnownArtworkDirectories() {
        val cacheDir = temporaryDirectory()
        val filesDir = temporaryDirectory()
        val context = mock<Context>()
        whenever(context.cacheDir).thenReturn(cacheDir)
        whenever(context.filesDir).thenReturn(filesDir)
        val artworkDir = File(cacheDir, ArtworkProvider.REGISTRY_ARTWORK_DIR).apply { mkdirs() }
        val artwork = File(artworkDir, "station.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        assertEquals(
            artwork.canonicalFile,
            resolveArtworkFile(
                context,
                ArtworkProvider.REGISTRY_ARTWORK_DIR,
                artwork.name,
            )?.canonicalFile,
        )
        assertNull(resolveArtworkFile(context, ArtworkProvider.REGISTRY_ARTWORK_DIR, "missing.png"))
        assertNull(resolveArtworkFile(context, ArtworkProvider.REGISTRY_ARTWORK_DIR, "../outside.png"))
        assertNull(resolveArtworkFile(context, "other", artwork.name))
        assertTrue(artwork.delete())
    }

    @Test
    fun localLogoDirectoryUsesFilesDirectoryAndRejectsNestedPath() {
        val cacheDir = temporaryDirectory()
        val filesDir = temporaryDirectory()
        val context = mock<Context>()
        whenever(context.cacheDir).thenReturn(cacheDir)
        whenever(context.filesDir).thenReturn(filesDir)
        val logosDir = File(filesDir, ArtworkProvider.LOCAL_LOGO_DIR).apply { mkdirs() }
        File(logosDir, "logo.webp").writeBytes(byteArrayOf(4))

        assertTrue(resolveArtworkFile(context, ArtworkProvider.LOCAL_LOGO_DIR, "logo.webp")?.isFile == true)
        assertNull(resolveArtworkFile(context, ArtworkProvider.LOCAL_LOGO_DIR, "nested/logo.webp"))
    }

    @Test
    fun rejectsAbsoluteAndTraversalFileNames() {
        val cacheDir = temporaryDirectory()
        val context = mock<Context>()
        whenever(context.cacheDir).thenReturn(cacheDir)
        File(cacheDir, ArtworkProvider.REGISTRY_ARTWORK_DIR).apply { mkdirs() }

        assertNull(resolveArtworkFile(context, ArtworkProvider.REGISTRY_ARTWORK_DIR, "/etc/passwd"))
        assertNull(resolveArtworkFile(context, ArtworkProvider.REGISTRY_ARTWORK_DIR, ".."))
        assertNull(resolveArtworkFile(context, ArtworkProvider.REGISTRY_ARTWORK_DIR, "a/../../b.png"))
    }

    @Test
    fun providerIsReadOnlyForWritesAndQueries() {
        val provider = ArtworkProvider()
        val uri = mock<android.net.Uri>()

        assertNull(provider.insert(uri, null))
        assertEquals(0, provider.update(uri, null, null, null))
        assertEquals(0, provider.delete(uri, null, null))
        assertNull(provider.query(uri, null, null, null, null))
    }

    @Test
    fun artworkCanOnlyBeOpenedReadOnly() {
        assertTrue(isReadOnlyArtworkMode("r"))
        assertTrue(isReadOnlyArtworkMode("rt"))
        assertFalse(isReadOnlyArtworkMode("w"))
        assertFalse(isReadOnlyArtworkMode("rw"))
        assertFalse(isReadOnlyArtworkMode("wa"))
    }

    private fun temporaryDirectory(): File = Files.createTempDirectory("artwork-provider-test-").toFile()
}
