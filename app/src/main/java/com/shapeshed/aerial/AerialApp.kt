package com.shapeshed.aerial

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.shapeshed.aerial.data.BauerProvider
import com.shapeshed.aerial.data.BbcProvider
import com.shapeshed.aerial.data.GlobalPlayerProvider
import com.shapeshed.aerial.data.MusicBrainzProvider
import com.shapeshed.aerial.data.Provider
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.RadioBrowserApi
import com.shapeshed.aerial.data.StationDatabase
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.WirelessProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AerialApp : Application() {
    val repository by lazy { StationRepository(StationDatabase.get(this).stationDao()) }
    val settingsDataStore get() = dataStore
    var showNowPlayingOnResume: Boolean = false
    val networkMonitor by lazy { NetworkMonitor(this) }
    val providers: List<Provider> = listOf(BbcProvider(), BauerProvider(), GlobalPlayerProvider(), WirelessProvider(), MusicBrainzProvider())

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
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
