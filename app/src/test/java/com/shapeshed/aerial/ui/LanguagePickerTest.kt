package com.shapeshed.aerial.ui

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
}
