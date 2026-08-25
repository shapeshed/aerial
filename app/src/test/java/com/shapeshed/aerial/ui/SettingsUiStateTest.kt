package com.shapeshed.aerial.ui

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.shapeshed.aerial.SHOW_HOME_KEY
import com.shapeshed.aerial.SHOW_STREAM_BITRATE_KEY
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUiStateTest {
    @Test
    fun missingPreferencesUseDocumentedDefaults() {
        val state = settingsUiState(emptyPreferences())

        assertTrue(state.showHome)
        assertFalse(state.showStreamBitrate)
    }

    @Test
    fun persistedBooleanPreferencesAreAppliedTogether() {
        val preferences = mutablePreferencesOf(
            SHOW_HOME_KEY to false,
            SHOW_STREAM_BITRATE_KEY to true,
        )

        val state = settingsUiState(preferences)

        assertFalse(state.showHome)
        assertTrue(state.showStreamBitrate)
    }
}
