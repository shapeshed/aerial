package com.shapeshed.aerial.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.ui.theme.AerialTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class HomeTabContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun homeCardsPropagateStationAndMoodSelections() {
        val recentlyPlayed = registryStation("recent", "Recently played station")
        val recommended = registryStation("recommended", "Recommended station")
        var selectedStation: RegistryStation? = null
        var selectedMood: CuratedMood? = null

        composeRule.setContent {
            AerialTheme(dynamicColor = false) {
                HomeTabContent(
                    forYouStations = listOf(recommended),
                    forYouCountry = "United Kingdom",
                    recentlyPlayedStations = listOf(recentlyPlayed),
                    listState = rememberLazyGridState(),
                    bottomPadding = 0.dp,
                    onMoodTap = { selectedMood = it },
                    onRecentlyPlayedStationTap = { selectedStation = it },
                    onFeaturedStationTap = { selectedStation = it },
                    onForYouViewAll = {},
                )
            }
        }

        composeRule.onNodeWithText(recentlyPlayed.name).performClick()
        assertSame(recentlyPlayed, selectedStation)

        composeRule.onNodeWithText(recommended.name).performClick()
        assertSame(recommended, selectedStation)

        val relax = composeRule.activity.getString(R.string.mood_relax)
        composeRule.onNodeWithText(relax).performScrollTo().performClick()
        assertEquals("relax", selectedMood?.id)
    }

    private fun registryStation(providerId: String, name: String) = RegistryStation(
        name = name,
        streamUrl = "https://example.test/$providerId",
        provider = "test",
        providerId = providerId,
    )
}
