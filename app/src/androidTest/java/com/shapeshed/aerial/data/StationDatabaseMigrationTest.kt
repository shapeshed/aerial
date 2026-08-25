package com.shapeshed.aerial.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StationDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StationDatabase::class.java,
    )

    @Test
    fun migrationFrom14PreservesStationsAndAddsPlaybackHistory() {
        helper.createDatabase(DATABASE_NAME, 14).use { database ->
            database.execSQL(
                """
                INSERT INTO stations (
                  id, name, streamUrl, logoPath, isFavorite, provider, providerId,
                  tags, description, country, countryCode
                ) VALUES (
                  7, 'BBC Radio 4', 'https://example.test/radio4', '', 1,
                  'radio-browser', 'bbc-radio-4', 'news,talk', 'Speech radio',
                  'United Kingdom', 'GB'
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            DATABASE_NAME,
            16,
            true,
            *StationDatabase.supportedMigrations,
        ).use { database ->
            database.query(
                "SELECT name, playCount, lastPlayedAt FROM stations WHERE id = 7",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("BBC Radio 4", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
                assertEquals(0L, cursor.getLong(2))
            }

            database.execSQL(
                "INSERT INTO play_history (provider, providerId, playedAt) " +
                    "VALUES ('radio-browser', 'bbc-radio-4', 42000)",
            )
            database.query("SELECT playedAt FROM play_history").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(42_000L, cursor.getLong(0))
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "station-migration-test"
    }
}
