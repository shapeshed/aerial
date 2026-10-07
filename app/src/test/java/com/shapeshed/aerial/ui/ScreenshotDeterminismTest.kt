package com.shapeshed.aerial.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Pins screenshot previews to deterministic inputs.
 *
 * `validateDebugScreenshotTest` compares each rendered preview against a committed golden. A
 * preview that reads build metadata or the clock changes what it renders even when the UI has not
 * changed, so the comparison fails on an unrelated commit. That is what broke 0.7.4: the Settings
 * screenshot rendered `BuildConfig.VERSION_NAME`, so bumping the version failed three tall Settings
 * goldens and looked like a UI regression.
 *
 * A golden should only change when the screen itself changes. This test fails if a screenshot
 * source reaches for build config, the wall clock, or randomness. Comments are ignored, so
 * explaining the rule in a preview does not trip it.
 */
class ScreenshotDeterminismTest {

    @Test
    fun screenshotPreviewsUseDeterministicInputs() {
        val root = screenshotSourceRoot()
        assumeTrue("screenshot sources are not on disk", root.isDirectory)

        val violations = root.walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                val code = stripComments(file.readText())
                val path = file.relativeTo(root).path
                FORBIDDEN.flatMap { (pattern, description) ->
                    pattern.findAll(code).map { "$path: $description" }
                }
            }
            .toList()

        assertEquals(
            "Screenshot previews must not depend on build metadata or the clock, or the goldens " +
                "churn on unrelated commits. Use a fixed value instead.",
            emptyList<String>(),
            violations,
        )
    }

    private fun stripComments(source: String): String = source
        .replace(BLOCK_COMMENT, "")
        .lineSequence()
        .map { it.trimStart() }
        .filterNot { it.startsWith("//") }
        .joinToString("\n")

    private fun screenshotSourceRoot(): File {
        System.getProperty("aerial.screenshotTestSourceDir")?.let { return File(it) }
        // Gradle runs unit tests with the module directory as the working directory.
        return File("src/screenshotTest/java")
    }

    private companion object {
        val BLOCK_COMMENT = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)

        /** Nondeterministic inputs that would change a golden without any UI change. */
        val FORBIDDEN = mapOf(
            Regex("\\bBuildConfig\\b") to "reads BuildConfig (version, build label or debug flag)",
            Regex("\\bSystem\\.currentTimeMillis\\b") to "reads the wall clock",
            Regex("\\bSystem\\.nanoTime\\b") to "reads the monotonic clock",
            Regex("\\bSystemClock\\b") to "reads the device uptime",
            Regex("\\bInstant\\.now\\b") to "reads the wall clock",
            Regex("\\bClock\\.\\w+") to "uses a clock",
            Regex("\\bLocalDate\\.now\\b|\\bLocalDateTime\\.now\\b") to "reads the date",
            Regex("\\bRandom\\b") to "uses randomness",
            Regex("\\bUUID\\.randomUUID\\b") to "generates a random UUID",
        )
    }
}
