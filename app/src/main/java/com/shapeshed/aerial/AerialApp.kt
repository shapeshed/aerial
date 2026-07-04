package com.shapeshed.aerial

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import com.shapeshed.aerial.data.RadioFranceProvider
import com.shapeshed.aerial.data.RinseProvider
import com.shapeshed.aerial.data.RteProvider
import com.shapeshed.aerial.data.WirelessProvider
import android.util.Log
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val REGISTRY_LAST_SYNC_KEY = longPreferencesKey("registry_last_network_sync")
val ENRICH_METADATA_KEY = booleanPreferencesKey("enrich_metadata")
val SHOW_STREAM_BITRATE_KEY = booleanPreferencesKey("show_stream_bitrate")
private const val REGISTRY_SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000L

class AerialApp : Application(), SingletonImageLoader.Factory {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "Aerial/${BuildConfig.VERSION_NAME} (Android)")
                    .build(),
            )
        }
        .build()
    private val db by lazy { StationDatabase.get(this) }
    val repository by lazy { StationRepository(db.stationDao()) }
    val registryRepository by lazy { RegistryRepository(db.registryDao(), okHttpClient) }
    val settingsDataStore get() = dataStore
    val networkMonitor by lazy { NetworkMonitor(this) }
    val providers: List<Provider> = listOf(BbcProvider(), BauerProvider(), GlobalPlayerProvider(okHttpClient), WirelessProvider(), RadioFranceProvider(), RinseProvider(), RteProvider(), MusicBrainzProvider())

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching {
                if (registryRepository.isEmpty() || BuildConfig.DEBUG) {
                    registryRepository.syncFromAssets(this@AerialApp)
                }
                if (!BuildConfig.DEBUG) {
                    val lastSync = dataStore.data.first()[REGISTRY_LAST_SYNC_KEY] ?: 0L
                    if (System.currentTimeMillis() - lastSync > REGISTRY_SYNC_INTERVAL_MS) {
                        val stations = registryRepository.syncFromNetwork()
                        if (stations != null) {
                            repository.updateStreamUrlsFromRegistry(stations)
                            dataStore.edit { it[REGISTRY_LAST_SYNC_KEY] = System.currentTimeMillis() }
                        }
                    }
                }
            }.onFailure { e ->
                Log.e("AerialApp", "Registry bootstrap failed", e)
            }
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        // Some hosts (e.g. Wikimedia) reject requests with no/generic User-Agent (403),
        // so station logos are fetched with the same identified client used elsewhere.
        return ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .build()
    }
}
