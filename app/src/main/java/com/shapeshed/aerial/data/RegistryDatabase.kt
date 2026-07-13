package com.shapeshed.aerial.data

import android.content.Context
import androidx.core.content.edit
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.zip.GZIPInputStream

@Database(
    entities = [RegistryStation::class, RegistryStationFts::class],
    version = 1,
    exportSchema = true,
)
abstract class RegistryDatabase : RoomDatabase() {
    abstract fun registryDao(): RegistryDao

    companion object {
        private const val DATABASE_NAME = "aerial-registry.db"
        private const val PREFS_NAME = "registry_database"
        private const val PREF_ASSET_VERSION = "asset_version"

        @Volatile private var instance: RegistryDatabase? = null

        fun get(context: Context, assetVersion: Int): RegistryDatabase =
            instance ?: synchronized(this) {
                context.prepareRegistryDatabase(assetVersion)
                Room.databaseBuilder(context, RegistryDatabase::class.java, DATABASE_NAME)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }

        private fun Context.prepareRegistryDatabase(assetVersion: Int) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val databaseFile = getDatabasePath(DATABASE_NAME)
            val assetChanged = prefs.getInt(PREF_ASSET_VERSION, -1) != assetVersion
            if (!assetChanged && databaseFile.isFile) return
            if (assetChanged) deleteDatabase(DATABASE_NAME)
            databaseFile.parentFile?.mkdirs()
            runCatching {
                assets.open("registry.db.compressed").use { input ->
                    GZIPInputStream(input).use { gzip ->
                        databaseFile.outputStream().use { output ->
                            gzip.copyTo(output)
                        }
                    }
                }
            }.onFailure {
                databaseFile.delete()
                throw it
            }
            prefs.edit { putInt(PREF_ASSET_VERSION, assetVersion) }
        }
    }
}
