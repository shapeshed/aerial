package com.shapeshed.aerial.ui

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.dataStore
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.StationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UiModule {
    @Provides
    @Singleton
    fun provideStationRepository(application: Application): StationRepository =
        (application as AerialApp).repository

    @Provides
    @Singleton
    fun provideRegistryRepository(application: Application): RegistryRepository =
        (application as AerialApp).registryRepository

    @Provides
    @Singleton
    fun provideArtworkLoader(application: Application): ArtworkLoader =
        CoilArtworkLoader(application)

    @Provides
    @Singleton
    fun provideSettingsDataStore(application: Application): DataStore<Preferences> =
        application.dataStore

    @Provides
    @Singleton
    fun provideSettingsBackupManager(application: Application): SettingsBackupManager {
        val app = application as AerialApp
        return ZipSettingsBackupManager(app, app.repository, app.settingsDataStore)
    }
}
