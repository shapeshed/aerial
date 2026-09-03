package com.shapeshed.aerial.testing

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.shapeshed.aerial.AerialApp
import kotlinx.coroutines.runBlocking

/** Shared setup for tests that need Aerial's real application dependencies. */
object AerialTestEnvironment {
    fun app(): AerialApp = ApplicationProvider.getApplicationContext<AerialApp>().also { app ->
        check(app.packageName == TEST_APPLICATION_ID) {
            "Tests must use the isolated application $TEST_APPLICATION_ID, " +
                "but found ${app.packageName}."
        }
    }

    fun resetPreferences() {
        runBlocking {
            app().settingsDataStore.edit { preferences -> preferences.clear() }
        }
    }

    private const val TEST_APPLICATION_ID = "com.shapeshed.aerial.deviceTest"
}
