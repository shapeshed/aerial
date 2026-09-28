package com.shapeshed.aerial.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguagePickerTest {
    @Test
    fun supportedLanguagesHaveUniqueTagsAndLabels() {
        assertFalse(APP_LANGUAGES.isEmpty())
        assertEquals(APP_LANGUAGES.size, APP_LANGUAGES.map { it.tag.lowercase() }.toSet().size)
        assertTrue(APP_LANGUAGES.all { it.tag.isNotBlank() && it.autonym.isNotBlank() })
    }

    @Test
    fun languageListIncludesDefaultEnglishAndRegionalEnglish() {
        assertEquals("English (US)", APP_LANGUAGES.first { it.tag == "en" }.autonym)
        assertEquals("English (UK)", APP_LANGUAGES.first { it.tag == "en-GB" }.autonym)
    }

    @Test
    fun everySupportedLanguageShipsTranslations() {
        // A tag in APP_LANGUAGES with no translated resources offers the user a language that
        // silently falls back to English at runtime.
        val missing = APP_LANGUAGES.map { it.tag }.filter { !hasTranslations(it) }
        assertTrue("no translations for: $missing", missing.isEmpty())
    }

    @Test
    fun theInAppListAndTheSystemLocaleConfigAgree() {
        // APP_LANGUAGES drives the in-app picker below API 33; locales_config.xml drives the
        // system picker from API 33. If they drift, Finnish-style translations become reachable
        // on one Android version and not the other. The comment in LanguagePicker.kt says these
        // must stay in sync, and that comment was not enough: adding a locale to only one place
        // is exactly the mistake it warns about, and it shipped.
        val declared = declaredLocaleTags()
        val listed = APP_LANGUAGES.map { it.tag }.toSet()
        assertEquals(
            "locales_config.xml and APP_LANGUAGES disagree; each needs the new locale",
            listed,
            declared,
        )
    }

    private fun declaredLocaleTags(): Set<String> {
        val file = localesConfig()
        assertTrue("expected res/xml/locales_config.xml at $file", file.isFile)
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val nodes = document.getElementsByTagName("locale")
        return (0 until nodes.length)
            .mapNotNull { nodes.item(it)?.attributes?.getNamedItem("android:name")?.nodeValue }
            .toSet()
    }

    private fun localesConfig(): File = resDir().resolve("xml/locales_config.xml")

    /**
     * Maps a BCP-47 tag to the Android resource qualifier that holds its translations.
     *
     * `en` is the default locale and has no `values-en` directory at all, and a region subtag
     * becomes `r` + uppercase, so `en-GB` and `zh-CN` live in `values-en-rGB` and
     * `values-zh-rCN`. Testing the raw tag found all three of those as "missing", which was the
     * test being wrong rather than the app.
     */
    private fun hasTranslations(tag: String): Boolean {
        if (tag == "en") return resDir().resolve("values").isDirectory
        val qualifier = tag.replace("-", "-r")
        return resDir().resolve("values-$qualifier").isDirectory
    }

    private fun resDir(): File {
        // Same mechanism PackageLayeringTest uses to find sources from a JVM test; Gradle runs
        // unit tests with the module directory as the working directory.
        return File("src/main/res")
    }
}
