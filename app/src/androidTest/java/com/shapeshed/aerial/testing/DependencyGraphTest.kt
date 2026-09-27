package com.shapeshed.aerial.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.PlayerService
import com.shapeshed.aerial.widget.AerialWidgetActionReceiver
import com.shapeshed.aerial.widget.AerialWidgetReceiver
import com.shapeshed.aerial.widget.DefaultWidgetUpdater
import com.shapeshed.aerial.widget.WidgetUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins the object graph that `UiModule` owns.
 *
 * These assertions are about wiring, not behaviour. They exist because the graph used to be split
 * between Hilt and lazily-cached fields on `AerialApp`, which let two `NetworkMonitor` instances
 * (and therefore two connectivity callbacks) exist at once: `AerialApp.networkMonitor` was the one
 * only instrumented tests saw, while `UiModule` built a second one for production. Resolving every
 * collaborator twice and asserting identity is what makes that class of bug fail here.
 */
@RunWith(AndroidJUnit4::class)
class DependencyGraphTest {

    @get:Rule
    val testEnvironment = AerialTestEnvironmentRule()

    @Test
    fun everyCollaboratorProductionNeedsResolves() {
        val graph = AerialTestEnvironment.graph()

        assertNotNull("StationRepository must be bound", graph.repository())
        assertNotNull("RegistryRepository must be bound", graph.registryRepository())
        assertNotNull("NetworkMonitor must be bound", graph.networkMonitor())
        assertNotNull("DataStore must be bound", graph.settingsDataStore())
        assertNotNull("StringProvider must be bound", graph.stringProvider())
        assertNotNull("WidgetUpdater must be bound", graph.widgetUpdater())
    }

    @Test
    fun eachCollaboratorIsASingleSharedInstance() {
        val graph = AerialTestEnvironment.graph()

        assertSame("NetworkMonitor must not be constructed twice", graph.networkMonitor(), graph.networkMonitor())
        assertSame("StationRepository must be shared", graph.repository(), graph.repository())
        assertSame("RegistryRepository must be shared", graph.registryRepository(), graph.registryRepository())
        assertSame("DataStore must be shared", graph.settingsDataStore(), graph.settingsDataStore())
        assertSame("WidgetUpdater must be shared", graph.widgetUpdater(), graph.widgetUpdater())
    }

    @Test
    fun widgetUpdaterIsBackedByTheDebouncedImplementation() {
        assertTrue(
            "the graph must not bind a stub updater",
            AerialTestEnvironment.graph().widgetUpdater() is DefaultWidgetUpdater,
        )
    }

    @Test
    fun serviceAndWidgetReceiversAreHiltEntryPoints() {
        // Hilt rewrites these to generated subclasses; if the annotation were dropped while the
        // manifest kept declaring the component, injection would fail at runtime instead.
        assertHiltEntryPoint("PlayerService", PlayerService::class.java)
        assertHiltEntryPoint("AerialWidgetReceiver", AerialWidgetReceiver::class.java)
        assertHiltEntryPoint("AerialWidgetActionReceiver", AerialWidgetActionReceiver::class.java)
    }

    @Test
    fun aerialAppNoLongerExposesTheObjectGraph() {
        // The bug this migration fixed: `AerialApp` cached collaborators as public fields while
        // `UiModule` bound the same types separately, so two `NetworkMonitor`s existed. Any
        // collaborator that reappears as a field on the application can drift from the graph
        // again, so assert none of them are there.
        val fields = AerialApp::class.java.declaredFields.map { it.name }

        // Guard against the assertion below passing because the reflection found nothing at all.
        assertTrue(
            "reflection must see AerialApp's own fields, got $fields",
            fields.contains("imageLoader"),
        )
        assertEquals(
            "AerialApp must hold no collaborators; bind them in UiModule instead",
            emptyList<String>(),
            GRAPH_MEMBERS_ON_APPLICATION.filter(fields::contains),
        )
    }

    @Test
    fun coilResolvesTheGraphsImageLoaderRatherThanBuildingASecond() {
        // `AerialApp` is Coil's `SingletonImageLoader.Factory`, so this is the one place the
        // application hands a graph object back out. If it built its own loader instead, Compose,
        // the widget and Media3's bitmap loader would stop sharing an image cache and the SVG /
        // User-Agent behaviour that `UiModule` configures would apply to only some of them.
        val app = AerialTestEnvironment.app()

        assertSame(
            "Coil must use the same ImageLoader the graph holds",
            AerialTestEnvironment.graph().imageLoader(),
            app.newImageLoader(app),
        )
    }

    @Test
    fun widgetUpdaterIsUsableThroughTheInterface() {
        // Compile-time proof that the bound type satisfies the capability the UI and the service
        // depend on, rather than on the concrete class.
        val updater: WidgetUpdater = AerialTestEnvironment.graph().widgetUpdater()
        updater.request()
    }

    private fun assertHiltEntryPoint(name: String, type: Class<*>) {
        val superclass = type.superclass
        assertTrue(
            "$name must be a Hilt entry point but extends ${superclass?.name}",
            superclass?.simpleName?.startsWith("Hilt_") == true,
        )
    }

    private companion object {
        /** Collaborators `AerialApp` used to cache, and must never cache again. */
        val GRAPH_MEMBERS_ON_APPLICATION = listOf(
            "repository",
            "registryRepository",
            "settingsDataStore",
            "networkMonitor",
            "okHttpClient",
            "applicationScope",
        )
    }
}
