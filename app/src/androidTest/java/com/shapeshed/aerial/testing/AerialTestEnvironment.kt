package com.shapeshed.aerial.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.dataStore
import com.shapeshed.aerial.ui.StringProvider
import com.shapeshed.aerial.widget.WidgetUpdater
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
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

    /** The subset of the production graph that instrumented tests construct components from. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AerialGraph {
        fun repository(): StationRepository
        fun registryRepository(): RegistryRepository
        fun networkMonitor(): NetworkMonitor
        fun settingsDataStore(): DataStore<Preferences>
        fun stringProvider(): StringProvider
        fun widgetUpdater(): WidgetUpdater
    }

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
