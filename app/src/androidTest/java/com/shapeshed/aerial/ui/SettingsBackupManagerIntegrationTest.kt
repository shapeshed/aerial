package com.shapeshed.aerial.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shapeshed.aerial.SHOW_HOME_KEY
import com.shapeshed.aerial.SHOW_STREAM_BITRATE_KEY
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationDatabase
import com.shapeshed.aerial.data.StationRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsBackupManagerIntegrationTest {
    private lateinit var context: Context
    private lateinit var database: StationDatabase
    private lateinit var repository: StationRepository
    private lateinit var dataStore: MemoryDataStore
    private lateinit var manager: ZipSettingsBackupManager
    private lateinit var sourceLogo: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, StationDatabase::class.java).build()
        repository = StationRepository(database.stationDao(), database.playHistoryDao())
        dataStore = MemoryDataStore(
            mutablePreferencesOf(
                SHOW_HOME_KEY to false,
                SHOW_STREAM_BITRATE_KEY to true,
            ),
        )
        manager = ZipSettingsBackupManager(context, repository, dataStore)
        sourceLogo = File(context.cacheDir, "backup-${System.nanoTime()}.svg").apply {
            writeText("<svg xmlns=\"http://www.w3.org/2000/svg\"><circle r=\"1\"/></svg>")
        }
    }

    @After
    fun tearDown() {
        sourceLogo.delete()
        database.close()
    }

    @Test
    fun roundTripRestoresStationMetadataLocalArtworkAndSettings() {
        runBlocking {
            val original = Station(
                name = "BBC Radio 4",
                streamUrl = "https://example.test/radio4",
                logoPath = sourceLogo.absolutePath,
                isFavorite = true,
                provider = "radio-browser",
                providerId = "bbc-radio-4",
                tags = "news,talk",
                description = "Speech radio",
                country = "United Kingdom",
                countryCode = "GB",
                playCount = 12,
                lastPlayedAt = 42_000L,
            )
            val id = repository.insert(original)
            val archive = ByteArrayOutputStream()

            manager.write(archive)
            repository.delete(repository.getById(id)!!)
            dataStore.updateData {
                mutablePreferencesOf(
                    SHOW_HOME_KEY to true,
                    SHOW_STREAM_BITRATE_KEY to false,
                )
            }

            assertEquals(1, manager.read(ByteArrayInputStream(archive.toByteArray())))

            val restored = repository.getAll().first().single()
            assertEquals(original.copy(id = restored.id, logoPath = restored.logoPath), restored)
            assertTrue(File(restored.logoPath).isFile)
            assertEquals(sourceLogo.readText(), File(restored.logoPath).readText())
            assertEquals(false, dataStore.current()[SHOW_HOME_KEY])
            assertEquals(true, dataStore.current()[SHOW_STREAM_BITRATE_KEY])
            File(restored.logoPath).delete()
        }
    }

    private class MemoryDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }

        fun current(): Preferences = state.value
    }
}
