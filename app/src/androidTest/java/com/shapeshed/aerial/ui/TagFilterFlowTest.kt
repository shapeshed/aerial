package com.shapeshed.aerial.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.ui.theme.AerialTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Exercises picker visibility, ViewModel filter state, and displayed results across selections. */
class TagFilterFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var viewModel: MainViewModel
    private lateinit var stations: List<RegistryStation>

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<AerialApp>()
        runBlocking { app.settingsDataStore.updateData { it.toMutablePreferences().apply { clear() } } }

        stations = listOf(
            registryStation("Rock Station", "rock"),
            registryStation("Jazz Station", "jazz"),
            registryStation("Pop Station", "pop"),
        )
        viewModel = MainViewModel(app, app.repository, app.registryRepository, app.settingsDataStore)
    }

    @Test
    fun selectingMultipleTagsAndRemovingThemRestoresUnfilteredResults() {
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                TagFilterHarness(viewModel, stations)
            }
        }

        composeRule.onNodeWithText("Rock Station").assertIsDisplayed()
        composeRule.onNodeWithText("Jazz Station").assertIsDisplayed()
        composeRule.onNodeWithText("Pop Station").assertIsDisplayed()

        openPicker()
        selectTag("Rock")
        composeRule.onNodeWithText("Rock Station").assertIsDisplayed()
        composeRule.onAllNodesWithText("Jazz Station").assertCountEquals(0)

        openPicker()
        selectTag("Jazz")
        composeRule.onNodeWithText("Rock Station").assertIsDisplayed()
        composeRule.onNodeWithText("Jazz Station").assertIsDisplayed()
        composeRule.onAllNodesWithText("Pop Station").assertCountEquals(0)

        openPicker()
        selectTag("Jazz")
        openPicker()
        selectTag("Rock")
        composeRule.onNodeWithText("Rock Station").assertIsDisplayed()
        composeRule.onNodeWithText("Jazz Station").assertIsDisplayed()
        composeRule.onNodeWithText("Pop Station").assertIsDisplayed()

        composeRule.runOnIdle { assertEquals(emptySet<String>(), viewModel.selectedTags.value) }
    }

    private fun openPicker() {
        composeRule.onNodeWithText("Tags").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeRule.onNodeWithText("Search tags").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            } catch (_: IllegalStateException) {
                false
            }
        }
    }

    private fun selectTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeRule.onNodeWithText(tag).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            } catch (_: IllegalStateException) {
                false
            }
        }
        composeRule.onNodeWithText(tag).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeRule.onAllNodesWithText("Search tags").fetchSemanticsNodes().isEmpty()
            } catch (_: IllegalStateException) {
                false
            }
        }
    }

    @Composable
    private fun TagFilterHarness(viewModel: MainViewModel, stations: List<RegistryStation>) {
        var pickerOpen by remember { mutableStateOf(false) }
        val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
        val results = stations.filter { station ->
            selectedTags.isEmpty() || selectedTags.any { tag -> station.tags.split(",").contains(tag) }
        }
        Column {
            Button(onClick = { pickerOpen = true }) { Text("Tags") }
            results.forEach { Text(it.name) }
            if (pickerOpen) {
                FilterPickerSheetContent(
                    title = "Tags",
                    searchLabel = "Search tags",
                    query = "",
                    onQueryChange = {},
                    items = listOf("jazz", "pop", "rock"),
                    selectedItems = selectedTags,
                    displayName = { it.replaceFirstChar(Char::uppercase) },
                    onToggle = viewModel::toggleTagFilter,
                    onSelectionComplete = { pickerOpen = false },
                    onClear = viewModel::clearTagFilter,
                )
            }
        }
    }

    private fun registryStation(name: String, tag: String) = RegistryStation(
        name = name,
        streamUrl = "https://example.invalid/${tag}",
        tags = tag,
        provider = "tag-flow-test",
        providerId = tag,
    )
}
