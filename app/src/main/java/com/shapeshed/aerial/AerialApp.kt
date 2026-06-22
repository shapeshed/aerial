package com.shapeshed.aerial

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.RadioBrowserApi
import com.shapeshed.aerial.data.StationDatabase
import com.shapeshed.aerial.data.StationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AerialApp : Application() {
    val repository by lazy { StationRepository(StationDatabase.get(this).stationDao()) }
    val settingsDataStore get() = dataStore
    val networkMonitor by lazy { NetworkMonitor(this) }
    var showNowPlaying: Boolean = false
    var showFavoritesOnly: Boolean = false
    var listScrollIndex: Int = 0
    var listScrollOffset: Int = 0
    var gridScrollIndex: Int = 0
    var gridScrollOffset: Int = 0

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Parse the bundled fallback station list while the splash screen is showing
        // so it's ready in memory before the user reaches the search screen.
        appScope.launch {
            runCatching {
                val json = resources.openRawResource(R.raw.fallback_stations)
                    .bufferedReader()
                    .readText()
                RadioBrowserApi.warmFallback(json)
            }
        }
    }
}
