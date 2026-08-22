package com.shapeshed.aerial.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FilterPickerItemsTest {
    @Test
    fun selectedItemsSortFirstThenLabelsSortAlphabetically() {
        val labels = mapOf("gb" to "United Kingdom", "de" to "Germany", "fr" to "France")
        val result = filterPickerItems(
            items = listOf("gb", "de", "fr"),
            selectedItems = setOf("fr"),
            query = "",
            displayName = { labels.getValue(it) },
        )

        assertEquals(listOf("fr", "de", "gb"), result.map(FilterPickerItem::value))
        assertEquals(listOf("France", "Germany", "United Kingdom"), result.map(FilterPickerItem::label))
    }

    @Test
    fun queryMatchesDisplayLabelIgnoringWhitespaceAndCase() {
        val labels = mapOf("gb" to "United Kingdom", "de" to "Germany", "fr" to "France")
        val result = filterPickerItems(
            items = listOf("gb", "de", "fr"),
            selectedItems = emptySet(),
            query = "  KING  ",
            displayName = { labels.getValue(it) },
        )

        assertEquals(listOf(FilterPickerItem("gb", "United Kingdom", selected = false)), result)
    }
}
