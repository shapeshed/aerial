package com.shapeshed.aerial.ui

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.shapeshed.aerial.BuildConfig
import com.shapeshed.aerial.data.AERIAL_USER_AGENT
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.PlaybackSnapshotStore
import com.shapeshed.aerial.data.RegistryDatabase
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RoomTransactor
import com.shapeshed.aerial.data.StationDatabase
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.Transactor
import com.shapeshed.aerial.dataStore
import com.shapeshed.aerial.playback.WidgetUpdater
import com.shapeshed.aerial.widget.DefaultWidgetUpdater
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

/**
 * The single owner of Aerial's object graph.
 *
 * Every process-wide collaborator is constructed here and nowhere else: components such as
 * [com.shapeshed.aerial.PlayerService] and the home-screen widget receivers are Hilt entry
 * points, so nothing reaches for the [Application] to resolve a dependency. Adding a collaborator
 * means adding a binding here (or an `@Inject` constructor), never a lazily-cached field on
 * `AerialApp`.
 */
@Module
@InstallIn(SingletonComponent::class)
object UiModule {

    @Provides
    @Singleton
    fun provideStationDatabase(@ApplicationContext context: Context): StationDatabase = StationDatabase.create(context)

    @Provides
    @Singleton
    fun provideRegistryDatabase(@ApplicationContext context: Context): RegistryDatabase =
        RegistryDatabase.create(context, BuildConfig.VERSION_CODE)

    @Provides
    @Singleton
    fun provideTransactor(database: StationDatabase): Transactor = RoomTransactor(database)

    @Provides
    @Singleton
    fun provideStationRepository(database: StationDatabase, transactor: Transactor): StationRepository =
        StationRepository(database.stationDao(), database.playHistoryDao(), transactor)

    @Provides
    @Singleton
    fun provideRegistryRepository(database: RegistryDatabase): RegistryRepository =
        RegistryRepository(database.registryDao())

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor = NetworkMonitor(context)

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideSettingsBackupManager(
        @ApplicationContext context: Context,
        repository: StationRepository,
        dataStore: DataStore<Preferences>,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): SettingsBackupManager = ZipSettingsBackupManager(context, repository, dataStore, ioDispatcher)

    @Provides
    @Singleton
    fun provideArtworkLoader(@ApplicationContext context: Context): ArtworkLoader = CoilArtworkLoader(context)

    @Provides
    @Singleton
    fun provideStringProvider(@ApplicationContext context: Context): StringProvider =
        StringProvider { id -> context.getString(id) }

    @Provides
    @Singleton
    fun provideMediaControllerGateway(): MediaControllerGateway = DefaultMediaControllerGateway()

    @Provides
    @Singleton
    fun provideWidgetUpdater(
        @ApplicationContext context: Context,
        repository: StationRepository,
        snapshotStore: PlaybackSnapshotStore,
        @ApplicationScope applicationScope: CoroutineScope,
    ): WidgetUpdater = DefaultWidgetUpdater(context, repository, snapshotStore, applicationScope)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /** Lives for the whole process; used for work that must outlive any one screen or service. */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", AERIAL_USER_AGENT)
                    .build(),
            )
        }
        .build()

    /**
     * The single [ImageLoader] the whole process shares. `AerialApp` implements Coil's
     * `SingletonImageLoader.Factory` and hands this instance back, so every surface — Compose,
     * the widget, and Media3's bitmap loader — decodes artwork identically.
     */
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context, okHttpClient: OkHttpClient): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
                // Some hosts (e.g. Wikimedia) reject requests with no/generic User-Agent (403),
                // so station logos are fetched with the same identified client used elsewhere.
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .build()
}
