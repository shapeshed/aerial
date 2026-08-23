package com.shapeshed.aerial.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.aerial.ui.theme.AerialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FilterPickerSheetContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectingOneItemNotifiesSelectionAndClosesPicker() {
        var toggled = ""
        var completed = 0
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                FilterPickerSheetContent(
                    title = "Country",
                    searchLabel = "Search countries",
                    query = "",
                    onQueryChange = {},
                    items = listOf("de", "fr", "gb"),
                    selectedItems = emptySet(),
                    displayName = { mapOf("de" to "Germany", "fr" to "France", "gb" to "United Kingdom").getValue(it) },
                    onToggle = { toggled = it },
                    onSelectionComplete = { completed++ },
                    onClear = {},
                )
            }
        }

        composeRule.onNodeWithText("France").performClick()

        composeRule.runOnIdle {
            assertEquals("fr", toggled)
            assertEquals(1, completed)
        }
    }
}
