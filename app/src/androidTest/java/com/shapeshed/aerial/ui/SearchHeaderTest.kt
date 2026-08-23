package com.shapeshed.aerial.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SearchBarDefaults.InputField
import androidx.compose.runtime.Composable
import com.shapeshed.aerial.ui.theme.AerialTheme
import org.junit.Rule
import org.junit.Test

class SearchHeaderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun headerExposesSearchFieldAndSettingsAction() {
        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                SearchHeaderForTest()
            }
        }

        composeRule.onNodeWithText("Search stations").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun SearchHeaderForTest() {
    val state = rememberContainedSearchBarState()
    val textState = rememberTextFieldState()
    AppBarWithSearch(
        state = state,
        inputField = {
            InputField(
                textFieldState = textState,
                searchBarState = state,
                onSearch = {},
                placeholder = { androidx.compose.material3.Text("Search stations") },
            )
        },
        actions = {
            androidx.compose.material3.IconButton(onClick = {}) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                )
            }
        },
        colors = SearchBarDefaults.appBarWithSearchColors(),
        scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
    )
}
