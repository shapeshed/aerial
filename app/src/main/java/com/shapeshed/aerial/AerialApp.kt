package com.shapeshed.aerial

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val SHOW_STREAM_BITRATE_KEY = booleanPreferencesKey("show_stream_bitrate")
val SHOW_HOME_KEY = booleanPreferencesKey("show_home")

/**
 * Hilt host. Deliberately holds no collaborators of its own: the object graph lives in
 * [com.shapeshed.aerial.ui.UiModule] and is reached through injection, so a component can never
 * quietly pick up a second instance of something (the bug this class used to allow, where
 * `networkMonitor` here and the one Hilt provided were different objects).
 *
 * The one exception is Coil: [SingletonImageLoader.Factory] is a Coil service-provider interface,
 * not a DI seam, so it is the one place the application has to hand a graph object back out.
 */
@HiltAndroidApp
class AerialApp :
    Application(),
    SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun newImageLoader(context: Context): ImageLoader = imageLoader
}
