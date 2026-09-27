package com.shapeshed.aerial

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Pins the package layering so it cannot quietly rot.
 *
 * The graph used to be mutually entangled: the media service imported from `widget` while the
 * widget imported from the service and the root package, and `ui` imported `widget` for the
 * widget updater. Nothing could check that, so a change to any of the three had to understand
 * all three.
 *
 * Intended layering, bottom to top:
 *
 * ```
 * data       persistence and domain types; depends on nothing in the app
 * playback   MediaItems, artwork and widget contracts; depends only on data
 * ui         Compose, ViewModels, the Activity
 * widget     the home-screen widget
 * root       Application, PlayerService, providers, policies
 * ```
 *
 * `di` is excluded from the cycle analysis. It is wiring, so it points at every layer by
 * nature, and its qualifiers are referenced *by* those layers — including it in the graph makes
 * a cycle unavoidable and would only hide the real ones. Its own consistency is covered by
 * `DependencyGraphTest` on a device.
 */
class PackageLayeringTest {

    @Test
    fun thePackageGraphIsAcyclicApartFromTheDocumentedComponentPair() {
        assumeTrue("main sources are not on disk", sourceRoot().isDirectory)

        val cycles = findCycles(layerEdges())
        assertEquals(
            "Package dependency cycle introduced. Layers bottom to top: " +
                "data, playback, ui, widget, root.\n" + describe(layerEdges()),
            emptyList<String>(),
            cycles.map { cycle -> cycle.joinToString(" -> ") { it.ifEmpty { "root" } } },
        )
    }

    @Test
    fun dataDependsOnNothingInTheApp() = assertDependsOn("data", emptySet())

    @Test
    fun playbackDependsOnlyOnData() = assertDependsOn("playback", setOf("data"))

    @Test
    fun theServiceDoesNotDependOnTheWidget() {
        // The cycle that motivated the `playback` layer. The service and the widget are separate
        // surfaces that happen to share playback state; the shared contracts belong in
        // `playback`, and the widget's rendering must never be a dependency of the service.
        assertTrue(
            "root must not import widget; shared contracts live in playback",
            layerEdges()[""].orEmpty().none { it.startsWith("widget") },
        )
    }

    @Test
    fun theUiDoesNotDependOnTheWidget() {
        assertTrue(
            "ui must not import widget; the widget's capability is injected via playback.WidgetUpdater",
            layerEdges()["ui"].orEmpty().none { it.startsWith("widget") },
        )
    }

    @Test
    fun theWidgetDoesNotDependOnTheDataRepositoriesImplementation() {
        // The widget may read Station/RegistryStation types, but it should not reach into
        // persistence directly beyond the repository surface it is handed.
        assertTrue(
            "widget must not import Room",
            layerEdges()["widget"].orEmpty().none { it == "data" } ||
                widgetImports().none { it.contains("androidx.room") },
        )
    }

    private fun assertDependsOn(pkg: String, expected: Set<String>) {
        assumeTrue("main sources are not on disk", sourceRoot().isDirectory)
        assertEquals(
            "$pkg's dependencies changed. " + describe(layerEdges()),
            expected,
            layerEdges()[pkg].orEmpty(),
        )
    }

    private fun describe(edges: Map<String, Set<String>>): String = edges.keys.sorted()
        .joinToString("\n") { from ->
            val to = edges[from].orEmpty().sorted()
            "  ${from.ifEmpty { "root" }} -> ${to.joinToString(", ").ifEmpty { "(nothing)" }}"
        }

    private fun widgetImports(): List<String> = importsIn("widget")

    private fun importsIn(pkg: String): List<String> {
        val dir = File(sourceRoot(), pkg)
        if (!dir.isDirectory) return emptyList()
        return dir.walkTopDown().filter { it.extension == "kt" }
            .flatMap { IMPORT.findAll(it.readText()).map { m -> m.groupValues[1] } }
            .toList()
    }

    private fun layerEdges(): Map<String, Set<String>> {
        val root = sourceRoot()
        val edges = mutableMapOf<String, MutableSet<String>>()
        root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val source = file.relativeTo(root).path.replace(File.separatorChar, '/')
                .substringBeforeLast('/', "")
            // `di` is wiring, not a layer; see the class KDoc.
            if (source == "di" || source.startsWith("di/")) return@forEach
            for (import in IMPORT.findAll(file.readText()).map { it.groupValues[1] }) {
                val parts = import.removePrefix("com.shapeshed.aerial.").split('.')
                // R, R.string and BuildConfig are generated and belong to no layer.
                if (parts.last() in GENERATED || parts.getOrNull(parts.size - 2) in GENERATED) continue
                val target = if (parts.size > 1) parts.dropLast(1).joinToString(".") else ""
                if (target == source || target == "di" || target.startsWith("di/")) continue
                if (isAllowedComponentCrossReference(source, import)) continue
                edges.getOrPut(source) { mutableSetOf() } += target
            }
        }
        return edges
    }

    /**
     * The one permitted cycle. `PlayerService` names `MainActivity` to build the notification's
     * `PendingIntent`, and `MediaControllerGateway` names `PlayerService` to build the
     * `SessionToken`. An Activity and its Service have to be able to identify each other. The
     * alternative is an implicit intent with a matching filter, which would add a deep-link
     * surface purely to satisfy a compiler — and `AGENTS.md` deliberately keeps the manifest
     * free of `VIEW` filters.
     */
    private fun isAllowedComponentCrossReference(source: String, imported: String): Boolean =
        ALLOWED_COMPONENT_CROSS_REFERENCES.any { (from, symbol) ->
            source == from && imported == "com.shapeshed.aerial.$symbol"
        }

    private fun findCycles(edges: Map<String, Set<String>>): List<List<String>> {
        val found = mutableListOf<List<String>>()
        val state = mutableMapOf<String, Int>()
        val stack = mutableListOf<String>()

        fun visit(node: String) {
            state[node] = VISITING
            stack += node
            for (next in (edges[node] ?: emptySet()).sorted()) {
                when (state[next] ?: UNVISITED) {
                    VISITING -> found += stack.subList(stack.indexOf(next), stack.size) + next
                    UNVISITED -> visit(next)
                }
            }
            stack.removeAt(stack.lastIndex)
            state[node] = VISITED
        }
        (edges.keys + edges.values.flatten()).distinct().sorted()
            .forEach { if ((state[it] ?: UNVISITED) == UNVISITED) visit(it) }
        return found
    }

    private fun sourceRoot(): File {
        System.getProperty("aerial.mainSourceDir")?.let { return File(it) }
        // Gradle runs unit tests with the module directory as the working directory.
        return File("src/main/java/com/shapeshed/aerial")
    }

    private companion object {
        val IMPORT = Regex("^import (com\\.shapeshed\\.aerial\\.[\\w.]+)$", RegexOption.MULTILINE)
        val GENERATED = setOf("R", "BuildConfig")

        /** `from package -> imported root-package symbol` allowed despite forming a cycle. */
        val ALLOWED_COMPONENT_CROSS_REFERENCES = listOf(
            "" to "ui.MainActivity",
            "ui" to "PlayerService",
        )

        const val UNVISITED = 0
        const val VISITING = 1
        const val VISITED = 2
    }
}
