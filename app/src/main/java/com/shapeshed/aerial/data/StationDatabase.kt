package com.shapeshed.aerial.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Station::class, StationFts::class],
    version = 15,
    exportSchema = true,
)
abstract class StationDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        @Volatile private var instance: StationDatabase? = null

        fun get(context: Context): StationDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(context, StationDatabase::class.java, "aerial.db")
                    // Explicit migrations cover versions 6–9 → 10.
                    // Any user still on v5 or below will have their data wiped by the fallback.
                    // Future version bumps MUST add an explicit Migration before relying on this fallback.
                    .addMigrations(
                        MIGRATION_6_10,
                        MIGRATION_7_10,
                        MIGRATION_8_10,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                    )
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

        // Adds the description column and the FTS4 search index. The CREATE VIRTUAL TABLE DDL
        // must match Room's generated schema exactly (see schemas/…/11.json) or the identity
        // check fails on open. The final INSERT('rebuild') populates the index from the content.
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `registry_stations` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `registry_stations_fts` USING FTS4(" +
                        "`searchText` TEXT NOT NULL, `description` TEXT NOT NULL, `country` TEXT NOT NULL, " +
                        "tokenize=unicode61 `remove_diacritics=1`, content=`registry_stations`)",
                )
                db.execSQL("INSERT INTO `registry_stations_fts`(`registry_stations_fts`) VALUES('rebuild')")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `stations_fts` USING FTS4(" +
                        "`name` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `provider` TEXT NOT NULL, " +
                        "`providerId` TEXT NOT NULL, tokenize=unicode61 `remove_diacritics=1`, content=`stations`)",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_stations_fts_BEFORE_UPDATE " +
                        "BEFORE UPDATE ON `stations` BEGIN DELETE FROM `stations_fts` WHERE `docid`=OLD.`rowid`; END",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_stations_fts_BEFORE_DELETE " +
                        "BEFORE DELETE ON `stations` BEGIN DELETE FROM `stations_fts` WHERE `docid`=OLD.`rowid`; END",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_stations_fts_AFTER_UPDATE " +
                        "AFTER UPDATE ON `stations` BEGIN INSERT INTO `stations_fts`(`docid`, `name`, `streamUrl`, `provider`, `providerId`) " +
                        "VALUES (NEW.`rowid`, NEW.`name`, NEW.`streamUrl`, NEW.`provider`, NEW.`providerId`); END",
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_stations_fts_AFTER_INSERT " +
                        "AFTER INSERT ON `stations` BEGIN INSERT INTO `stations_fts`(`docid`, `name`, `streamUrl`, `provider`, `providerId`) " +
                        "VALUES (NEW.`rowid`, NEW.`name`, NEW.`streamUrl`, NEW.`provider`, NEW.`providerId`); END",
                )
                db.execSQL("INSERT INTO `stations_fts`(`stations_fts`) VALUES('rebuild')")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `stations` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `stations` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `stations` ADD COLUMN `country` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `stations` ADD COLUMN `countryCode` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    UPDATE `stations`
                    SET
                      `tags` = COALESCE((
                        SELECT `registry_stations`.`tags`
                        FROM `registry_stations`
                        WHERE
                          (`stations`.`provider` != '' AND `stations`.`providerId` != ''
                            AND `registry_stations`.`provider` = `stations`.`provider`
                            AND `registry_stations`.`providerId` = `stations`.`providerId`)
                          OR `registry_stations`.`streamUrl` = `stations`.`streamUrl`
                        LIMIT 1
                      ), ''),
                      `description` = COALESCE((
                        SELECT `registry_stations`.`description`
                        FROM `registry_stations`
                        WHERE
                          (`stations`.`provider` != '' AND `stations`.`providerId` != ''
                            AND `registry_stations`.`provider` = `stations`.`provider`
                            AND `registry_stations`.`providerId` = `stations`.`providerId`)
                          OR `registry_stations`.`streamUrl` = `stations`.`streamUrl`
                        LIMIT 1
                      ), ''),
                      `country` = COALESCE((
                        SELECT `registry_stations`.`country`
                        FROM `registry_stations`
                        WHERE
                          (`stations`.`provider` != '' AND `stations`.`providerId` != ''
                            AND `registry_stations`.`provider` = `stations`.`provider`
                            AND `registry_stations`.`providerId` = `stations`.`providerId`)
                          OR `registry_stations`.`streamUrl` = `stations`.`streamUrl`
                        LIMIT 1
                      ), ''),
                      `countryCode` = COALESCE((
                        SELECT `registry_stations`.`countryCode`
                        FROM `registry_stations`
                        WHERE
                          (`stations`.`provider` != '' AND `stations`.`providerId` != ''
                            AND `registry_stations`.`provider` = `stations`.`provider`
                            AND `registry_stations`.`providerId` = `stations`.`providerId`)
                          OR `registry_stations`.`streamUrl` = `stations`.`streamUrl`
                        LIMIT 1
                      ), '')
                    WHERE `tags` = '' AND `description` = '' AND `country` = '' AND `countryCode` = ''
                    """.trimIndent(),
                )
                dropStationsFts(db)
                createStationsFts(db)
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                dropRegistryFts(db)
                db.execSQL("DROP TABLE IF EXISTS `registry_stations`")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `stations` ADD COLUMN `playCount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `stations` ADD COLUMN `lastPlayedAt` INTEGER NOT NULL DEFAULT 0")
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

        private fun dropStationsFts(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_stations_fts_BEFORE_UPDATE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_stations_fts_BEFORE_DELETE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_stations_fts_AFTER_UPDATE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_stations_fts_AFTER_INSERT")
            db.execSQL("DROP TABLE IF EXISTS `stations_fts`")
        }

        private fun dropRegistryFts(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_registry_stations_fts_BEFORE_UPDATE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_registry_stations_fts_BEFORE_DELETE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_registry_stations_fts_AFTER_UPDATE")
            db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_registry_stations_fts_AFTER_INSERT")
            db.execSQL("DROP TABLE IF EXISTS `registry_stations_fts`")
        }

        private fun createStationsFts(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `stations_fts` USING FTS4(" +
                    "`name` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `provider` TEXT NOT NULL, " +
                    "`providerId` TEXT NOT NULL, `tags` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                    "`country` TEXT NOT NULL, " +
                    "tokenize=unicode61 `remove_diacritics=1`, content=`stations`)",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_stations_fts_BEFORE_UPDATE " +
                    "BEFORE UPDATE ON `stations` BEGIN DELETE FROM `stations_fts` WHERE `docid`=OLD.`rowid`; END",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_stations_fts_BEFORE_DELETE " +
                    "BEFORE DELETE ON `stations` BEGIN DELETE FROM `stations_fts` WHERE `docid`=OLD.`rowid`; END",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_stations_fts_AFTER_UPDATE " +
                    "AFTER UPDATE ON `stations` BEGIN INSERT INTO `stations_fts`(`docid`, `name`, `streamUrl`, `provider`, `providerId`, `tags`, `description`, `country`) " +
                    "VALUES (NEW.`rowid`, NEW.`name`, NEW.`streamUrl`, NEW.`provider`, NEW.`providerId`, NEW.`tags`, NEW.`description`, NEW.`country`); END",
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_stations_fts_AFTER_INSERT " +
                    "AFTER INSERT ON `stations` BEGIN INSERT INTO `stations_fts`(`docid`, `name`, `streamUrl`, `provider`, `providerId`, `tags`, `description`, `country`) " +
                    "VALUES (NEW.`rowid`, NEW.`name`, NEW.`streamUrl`, NEW.`provider`, NEW.`providerId`, NEW.`tags`, NEW.`description`, NEW.`country`); END",
            )
            db.execSQL("INSERT INTO `stations_fts`(`stations_fts`) VALUES('rebuild')")
        }

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
