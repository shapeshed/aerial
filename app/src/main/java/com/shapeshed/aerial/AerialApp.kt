package com.shapeshed.aerial

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.shapeshed.aerial.data.StationDatabase
import com.shapeshed.aerial.data.StationRepository

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AerialApp : Application() {
    val repository by lazy { StationRepository(StationDatabase.get(this).stationDao()) }
    val settingsDataStore get() = dataStore
}
