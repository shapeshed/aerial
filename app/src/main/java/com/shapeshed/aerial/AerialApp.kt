package com.shapeshed.aerial

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shapeshed.aerial.data.BauerProvider
import com.shapeshed.aerial.data.BbcProvider
import com.shapeshed.aerial.data.GlobalPlayerProvider
import com.shapeshed.aerial.data.MusicBrainzProvider
import com.shapeshed.aerial.data.Provider
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.StationDatabase
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.WirelessProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val REGISTRY_LAST_SYNC_KEY = longPreferencesKey("registry_last_network_sync")
private const val REGISTRY_SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000L

class AerialApp : Application() {
    private val db by lazy { StationDatabase.get(this) }
    val repository by lazy { StationRepository(db.stationDao()) }
    val registryRepository by lazy { RegistryRepository(db.registryDao()) }
    val settingsDataStore get() = dataStore
    var showNowPlayingOnResume: Boolean = false
    val networkMonitor by lazy { NetworkMonitor(this) }
    val providers: List<Provider> = listOf(BbcProvider(), BauerProvider(), GlobalPlayerProvider(), WirelessProvider(), MusicBrainzProvider())

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching {
                if (registryRepository.isEmpty()) {
                    registryRepository.syncFromAssets(this@AerialApp)
                }
                val lastSync = dataStore.data.first()[REGISTRY_LAST_SYNC_KEY] ?: 0L
                if (System.currentTimeMillis() - lastSync > REGISTRY_SYNC_INTERVAL_MS) {
                    val stations = registryRepository.syncFromNetwork()
                    if (stations != null) {
                        repository.updateStreamUrlsFromRegistry(stations)
                        dataStore.edit { it[REGISTRY_LAST_SYNC_KEY] = System.currentTimeMillis() }
                    }
                }
            }
        }
    }
}
