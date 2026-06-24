package com.shapeshed.aerial.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Station::class, RegistryStation::class], version = 8, exportSchema = true)
abstract class StationDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun registryDao(): RegistryDao

    companion object {
        @Volatile private var instance: StationDatabase? = null

        // v0.2.0 shipped DB version 6 — this is the oldest supported migration path
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS registry_stations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        streamUrl TEXT NOT NULL,
                        logoUrl TEXT NOT NULL DEFAULT '',
                        country TEXT NOT NULL DEFAULT '',
                        countryCode TEXT NOT NULL DEFAULT '',
                        tags TEXT NOT NULL DEFAULT '',
                        provider TEXT NOT NULL DEFAULT '',
                        searchText TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
            }
        }

        // Fixes pre-release builds that had registry_stations without the searchText column
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS registry_stations")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS registry_stations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        streamUrl TEXT NOT NULL,
                        logoUrl TEXT NOT NULL DEFAULT '',
                        country TEXT NOT NULL DEFAULT '',
                        countryCode TEXT NOT NULL DEFAULT '',
                        tags TEXT NOT NULL DEFAULT '',
                        provider TEXT NOT NULL DEFAULT '',
                        searchText TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): StationDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(context, StationDatabase::class.java, "aerial.db")
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5)
                    .build()
                    .also { instance = it }
            }
    }
}
