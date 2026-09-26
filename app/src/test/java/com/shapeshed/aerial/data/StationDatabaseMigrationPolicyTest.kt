package com.shapeshed.aerial.data

import org.junit.Assert.assertTrue
import org.junit.Test

class StationDatabaseMigrationPolicyTest {
    @Test
    fun everySchemaVersionHasAnExplicitMigrationPath() {
        val coveredVersions = StationDatabase.supportedMigrations
            .flatMap { listOf(it.startVersion, it.endVersion) }
            .toSet()

        for (version in 1 until CURRENT_DATABASE_VERSION) {
            assertTrue(
                "Missing migration coverage for database version $version",
                version in coveredVersions,
            )
        }
    }

    private companion object {
        const val CURRENT_DATABASE_VERSION = 16
    }
}
