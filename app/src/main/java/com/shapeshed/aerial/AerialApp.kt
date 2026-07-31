package com.shapeshed.aerial

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shapeshed.aerial.data.AERIAL_USER_AGENT
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.RegistryDatabase
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.StationDatabase
import com.shapeshed.aerial.data.StationRepository
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import okhttp3.OkHttpClient

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val SHOW_STREAM_BITRATE_KEY = booleanPreferencesKey("show_stream_bitrate")
val SHOW_HOME_KEY = booleanPreferencesKey("show_home")
val FAVORITES_GRID_COLUMNS_KEY = intPreferencesKey("favorites_grid_columns")
const val FAVORITES_GRID_COLUMNS_DEFAULT = 3
val FAVORITES_GRID_COLUMNS_RANGE = 2..8

class AerialApp : Application(), SingletonImageLoader.Factory {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", AERIAL_USER_AGENT)
                    .build(),
            )
        }
        .build()
    private val db by lazy { StationDatabase.get(this) }
    private val registryDb by lazy { RegistryDatabase.get(this, BuildConfig.VERSION_CODE) }
    val repository by lazy { StationRepository(db.stationDao(), db.playHistoryDao()) }
    val registryRepository by lazy { RegistryRepository(registryDb.registryDao()) }
    val settingsDataStore get() = dataStore
    val networkMonitor by lazy { NetworkMonitor(this) }
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
