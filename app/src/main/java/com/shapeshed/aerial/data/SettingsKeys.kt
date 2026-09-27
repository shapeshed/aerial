package com.shapeshed.aerial.data

import androidx.datastore.preferences.core.booleanPreferencesKey

/** Whether the mini player shows the stream's current bitrate. */
val SHOW_STREAM_BITRATE_KEY = booleanPreferencesKey("show_stream_bitrate")

/** Whether the home tab is shown, or only favourites. */
val SHOW_HOME_KEY = booleanPreferencesKey("show_home")
