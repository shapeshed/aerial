package com.shapeshed.aerial.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StationDatabaseIntegrationTest {
    private lateinit var database: StationDatabase
    private lateinit var stationDao: StationDao
    private lateinit var historyDao: PlayHistoryDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            StationDatabase::class.java,
        ).build()
        stationDao = database.stationDao()
        historyDao = database.playHistoryDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun stationSearchUsesSqliteFtsForMetadataFields() = runBlocking {
        stationDao.insert(
            Station(
                name = "Kool FM",
                streamUrl = "https://example.test/kool",
                tags = "drum bass jungle",
                description = "Underground radio",
                country = "United Kingdom",
            ),
        )

        assertEquals("Kool FM", stationDao.searchStationFts("jungle*").single().name)
        assertEquals("Kool FM", stationDao.searchStationFts("underground*").single().name)
        assertEquals("Kool FM", stationDao.searchStationFts("kingdom*").single().name)
    }

    @Test
    fun playHistoryReplacesStationAndReturnsNewestFirst() = runBlocking {
        historyDao.recordPlay(PlayHistoryEntry("radio", "old", 1_000L))
        historyDao.recordPlay(PlayHistoryEntry("radio", "new", 3_000L))
        historyDao.recordPlay(PlayHistoryEntry("radio", "old", 4_000L))

        val entries = historyDao.recent(10)

        assertEquals(listOf("old", "new"), entries.map(PlayHistoryEntry::providerId))
        assertEquals(2, entries.size)
    }

    @Test
    fun stationPlayUpdatesCountersAndFavoriteRowCanBeDeleted() = runBlocking {
        val id = stationDao.insert(
            Station(
                name = "Mango Radio",
                streamUrl = "https://example.test/mango",
                isFavorite = true,
            ),
        )

        stationDao.recordPlay(id, playedAt = 9_000L)
        val played = stationDao.getById(id)!!
        assertEquals(1, played.playCount)
        assertEquals(9_000L, played.lastPlayedAt)

        stationDao.delete(played)
        assertTrue(stationDao.getById(id) == null)
    }
}
