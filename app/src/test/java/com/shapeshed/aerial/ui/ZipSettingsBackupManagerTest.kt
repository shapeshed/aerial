package com.shapeshed.aerial.ui

import android.content.ContentResolver
import android.content.Context
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.shapeshed.aerial.SHOW_HOME_KEY
import com.shapeshed.aerial.SHOW_STREAM_BITRATE_KEY
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.testing.MemoryDataStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ZipSettingsBackupManagerTest {
    private val filesDir = Files.createTempDirectory("aerial-backup-test-").toFile()
    private val context: Context = mock()

    @org.junit.Before
    fun setUp() {
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.filesDir).thenReturn(filesDir)
    }

    @Test
    fun exportThenImportRestoresStationsAndSettings() = runBlocking {
        val source = Station(
            name = "Radio Paradise",
            streamUrl = "https://stream.example/rp",
            isFavorite = true,
            provider = "radio-paradise",
            providerId = "main",
            tags = "eclectic",
            countryCode = "US",
        )
        val dataStore = MemoryDataStore(
            mutablePreferencesOf(SHOW_STREAM_BITRATE_KEY to true, SHOW_HOME_KEY to false),
        )
        val exporter = ZipSettingsBackupManager(context, repositoryWith(source), dataStore)

        val bytes = ByteArrayOutputStream().also { exporter.write(it) }.toByteArray()

        val targetRepository = mock<StationRepository>()
        val targetDataStore = MemoryDataStore()
        val restore = ZipSettingsBackupManager(context, targetRepository, targetDataStore)
        val restoredCount = restore.read(ByteArrayInputStream(bytes))

        assertEquals(1, restoredCount)
        val captor = argumentCaptor<Station>()
        verify(targetRepository).upsertImported(captor.capture())
        assertEquals("Radio Paradise", captor.firstValue.name)
        assertEquals("https://stream.example/rp", captor.firstValue.streamUrl)
        assertTrue(captor.firstValue.isFavorite)
        assertEquals("radio-paradise", captor.firstValue.provider)
        assertEquals("eclectic", captor.firstValue.tags)
        assertEquals("US", captor.firstValue.countryCode)

        val restoredPrefs = targetDataStore.data.first()
        assertEquals(true, restoredPrefs[SHOW_STREAM_BITRATE_KEY])
        assertEquals(false, restoredPrefs[SHOW_HOME_KEY])
    }

    @Test
    fun roundTripEmbedsAndRestoresLocalLogoFiles() = runBlocking {
        val logo = File(filesDir, "cool station logo!.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val source = Station(name = "Logo Station", streamUrl = "https://s", logoPath = logo.absolutePath)
        val exporter = ZipSettingsBackupManager(context, repositoryWith(source), MemoryDataStore())

        val bytes = ByteArrayOutputStream().also { exporter.write(it) }.toByteArray()

        val targetRepository = mock<StationRepository>()
        ZipSettingsBackupManager(context, targetRepository, MemoryDataStore())
            .read(ByteArrayInputStream(bytes))

        val captor = argumentCaptor<Station>()
        verify(targetRepository).upsertImported(captor.capture())
        val restoredFile = File(captor.firstValue.logoPath)
        assertTrue(restoredFile.isFile)
        // The restored copy lives in the app's logos directory, not the original path.
        assertTrue(restoredFile.parentFile.absolutePath.endsWith("logos"))
        assertEquals(listOf<Byte>(1, 2, 3), restoredFile.readBytes().toList())
    }

    @Test
    fun importReturnsFailureWhenOutputCannotBeOpened() = runBlocking {
        val resolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(resolver)
        whenever(resolver.openInputStream(any())).thenReturn(null)
        val manager = ZipSettingsBackupManager(context, mock(), MemoryDataStore())

        val result = manager.import(mock())

        assertTrue(result is BackupOperationResult.Failure)
    }

    @Test
    fun importRejectsOversizedLogoEntry(): Unit = runBlocking {
        val resolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(resolver)
        val oversizedLogo = ByteArray(2 * 1024 * 1024)
        whenever(resolver.openInputStream(any()))
            .thenReturn(ByteArrayInputStream(zipWithLogo(oversizedLogo)))
        val repository = mock<StationRepository>()
        val manager = ZipSettingsBackupManager(context, repository, MemoryDataStore())

        val result = manager.import(mock())

        assertTrue("result=$result", result is BackupOperationResult.Failure)
        verify(repository, never()).upsertImported(any())
    }

    @Test
    fun importReturnsFailureForUnsupportedBackupVersion() = runBlocking {
        val resolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(resolver)
        whenever(resolver.openInputStream(any()))
            .thenReturn(ByteArrayInputStream(zipWithVersion(version = 999)))
        val manager = ZipSettingsBackupManager(context, mock(), MemoryDataStore())

        val result = manager.import(mock())

        assertTrue(result is BackupOperationResult.Failure)
    }

    @Test
    fun importReturnsFailureWhenManifestIsMissing(): Unit = runBlocking {
        val resolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(resolver)
        val zipWithoutManifest = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("logos/orphan.png"))
                zip.write(byteArrayOf(9))
                zip.closeEntry()
            }
        }.toByteArray()
        whenever(resolver.openInputStream(any())).thenReturn(ByteArrayInputStream(zipWithoutManifest))
        val repository = mock<StationRepository>()
        val manager = ZipSettingsBackupManager(context, repository, MemoryDataStore())

        val result = manager.import(mock())

        assertTrue(result is BackupOperationResult.Failure)
        verify(repository, never()).upsertImported(any())
    }

    @Test
    fun exportReturnsFailureWhenTheDestinationCannotBeOpened() = runBlocking {
        val resolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(resolver)
        whenever(resolver.openOutputStream(any())).thenReturn(null)
        val manager = ZipSettingsBackupManager(context, repositoryWith(), MemoryDataStore())

        val result = manager.export(mock())

        assertTrue(result is BackupOperationResult.Failure)
    }

    @Test
    fun unknownSettingsKeysAreIgnored() = runBlocking {
        val repository = mock<StationRepository>()
        val resolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(resolver)
        whenever(resolver.openInputStream(any()))
            .thenReturn(ByteArrayInputStream(zipWithVersion(version = 1, settingsJson = "{\"future\":true}")))
        val dataStore = MemoryDataStore()
        val manager = ZipSettingsBackupManager(context, repository, dataStore)

        val result = manager.import(mock())

        assertTrue(result is BackupOperationResult.Success)
        assertNull(dataStore.data.first()[SHOW_HOME_KEY])
        assertNull(dataStore.data.first()[SHOW_STREAM_BITRATE_KEY])
    }

    private fun repositoryWith(vararg stations: Station): StationRepository {
        val repository = mock<StationRepository>()
        whenever(repository.getAll()).thenReturn(flowOf(stations.toList()))
        return repository
    }

    private fun zipWithLogo(payload: ByteArray): ByteArray {
        val manifest = """
            {"version":1,"app":"Aerial","settings":{},"stations":[]}
        """.trimIndent()
        return ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("logos/oversized.png"))
                zip.write(payload)
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("backup.json"))
                zip.write(manifest.toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
    }

    private fun zipWithVersion(version: Int, settingsJson: String = "{}"): ByteArray {
        val manifest = """
            {"version":$version,"app":"Aerial","settings":$settingsJson,"stations":[]}
        """.trimIndent()
        return ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("backup.json"))
                zip.write(manifest.toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
    }
}
