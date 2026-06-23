package com.shapeshed.aerial

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.shapeshed.aerial.data.BauerMetadataEnricher
import com.shapeshed.aerial.data.BbcMetadataEnricher
import com.shapeshed.aerial.data.GlobalPlayerMetadataEnricher
import com.shapeshed.aerial.data.MetadataEnricher
import com.shapeshed.aerial.data.MusicBrainzEnricher
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.RadioBrowserApi
import com.shapeshed.aerial.data.StationDatabase
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.WirelessMetadataEnricher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AerialApp : Application() {
    val repository by lazy { StationRepository(StationDatabase.get(this).stationDao()) }
    val settingsDataStore get() = dataStore
    val networkMonitor by lazy { NetworkMonitor(this) }
    val enrichers: List<MetadataEnricher> = listOf(BbcMetadataEnricher(), BauerMetadataEnricher(), GlobalPlayerMetadataEnricher(), WirelessMetadataEnricher(), MusicBrainzEnricher())

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
