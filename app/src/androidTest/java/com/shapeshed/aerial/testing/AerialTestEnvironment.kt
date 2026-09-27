package com.shapeshed.aerial.testing

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.dataStore
import com.shapeshed.aerial.di.AerialGraph
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking

/**
 * Shared setup for tests that need Aerial's real application dependencies.
 *
 * Collaborators come from Hilt's singleton component through [AerialGraph], not from fields on
 * `AerialApp`. Production owns the graph in `UiModule`, so tests resolve the very same bindings:
 * a collaborator that production needs but no module provides fails here at compile time, and one
 * that is bound twice surfaces as a duplicate-binding error rather than as two live instances.
 */
object AerialTestEnvironment {

    fun app(): AerialApp = ApplicationProvider.getApplicationContext<AerialApp>().also { app ->
        check(app.packageName == TEST_APPLICATION_ID) {
            "Tests must use the isolated application $TEST_APPLICATION_ID, " +
                "but found ${app.packageName}."
        }
    }

    fun graph(): AerialGraph = EntryPointAccessors.fromApplication(app(), AerialGraph::class.java)

    fun resetPreferences() {
        runBlocking {
            app().dataStore.edit { preferences -> preferences.clear() }
        }
    }

    private const val TEST_APPLICATION_ID = "com.shapeshed.aerial.deviceTest"
}
