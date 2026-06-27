package com.shapeshed.aerial.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Station::class, RegistryStation::class], version = 10, exportSchema = true)
abstract class StationDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun registryDao(): RegistryDao

    companion object {
        @Volatile private var instance: StationDatabase? = null

        fun get(context: Context): StationDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(context, StationDatabase::class.java, "aerial.db")
                    .addMigrations(MIGRATION_6_10, MIGRATION_7_10, MIGRATION_8_10, MIGRATION_9_10)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_6_10 = object : Migration(6, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateStationsWithoutProviderColumns(db)
                createRegistryTable(db)
            }
        }

        private val MIGRATION_7_10 = object : Migration(7, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateStationsWithoutProviderColumns(db)
                db.execSQL("DROP TABLE IF EXISTS `registry_stations`")
                createRegistryTable(db)
            }
        }

        private val MIGRATION_8_10 = object : Migration(8, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateStationsWithoutProviderColumns(db)
                db.execSQL("DROP TABLE IF EXISTS `registry_stations`")
                createRegistryTable(db)
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateStationsWithProviderColumns(db)
            }
        }

        private fun migrateStationsWithoutProviderColumns(db: SupportSQLiteDatabase) {
            db.execSQL(createStationsNewSql())
            db.execSQL(
                """
                INSERT INTO `stations_new` (`id`, `name`, `streamUrl`, `logoPath`, `isFavorite`, `provider`, `providerId`)
                SELECT `id`, `name`, `streamUrl`, `logoPath`, `isFavorite`, '', '' FROM `stations`
                """.trimIndent(),
            )
            replaceStationsTable(db)
        }

        private fun migrateStationsWithProviderColumns(db: SupportSQLiteDatabase) {
            db.execSQL(createStationsNewSql())
            db.execSQL(
                """
                INSERT INTO `stations_new` (`id`, `name`, `streamUrl`, `logoPath`, `isFavorite`, `provider`, `providerId`)
                SELECT `id`, `name`, `streamUrl`, `logoPath`, `isFavorite`, `provider`, `providerId` FROM `stations`
                """.trimIndent(),
            )
            replaceStationsTable(db)
        }

        private fun replaceStationsTable(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE `stations`")
            db.execSQL("ALTER TABLE `stations_new` RENAME TO `stations`")
        }

        private fun createStationsNewSql(): String =
            """
            CREATE TABLE IF NOT EXISTS `stations_new` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL,
              `streamUrl` TEXT NOT NULL,
              `logoPath` TEXT NOT NULL,
              `isFavorite` INTEGER NOT NULL,
              `provider` TEXT NOT NULL,
              `providerId` TEXT NOT NULL
            )
            """.trimIndent()

        private fun createRegistryTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `registry_stations` (
                  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  `name` TEXT NOT NULL,
                  `streamUrl` TEXT NOT NULL,
                  `logoUrl` TEXT NOT NULL,
                  `country` TEXT NOT NULL,
                  `countryCode` TEXT NOT NULL,
                  `tags` TEXT NOT NULL,
                  `provider` TEXT NOT NULL,
                  `providerId` TEXT NOT NULL,
                  `searchText` TEXT NOT NULL
                )
                """.trimIndent(),
            )
        }
    }
}
