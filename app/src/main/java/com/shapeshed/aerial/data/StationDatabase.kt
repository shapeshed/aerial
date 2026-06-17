package com.shapeshed.aerial.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Station::class], version = 5, exportSchema = false)
abstract class StationDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        @Volatile private var instance: StationDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stations ADD COLUMN iconEmoji TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stations ADD COLUMN logoPath TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE stations_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, streamUrl TEXT NOT NULL, logoPath TEXT NOT NULL DEFAULT '')")
                db.execSQL("INSERT INTO stations_new (id, name, streamUrl, logoPath) SELECT id, name, streamUrl, logoPath FROM stations")
                db.execSQL("DROP TABLE stations")
                db.execSQL("ALTER TABLE stations_new RENAME TO stations")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stations ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stations ADD COLUMN radioBrowserUuid TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): StationDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(context, StationDatabase::class.java, "aerial.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
