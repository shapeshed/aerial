package com.shapeshed.aerial.ui

import android.app.Application
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shapeshed.aerial.data.SHOW_HOME_KEY
import com.shapeshed.aerial.data.SHOW_STREAM_BITRATE_KEY
import com.shapeshed.aerial.data.StationListUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject internal constructor(
    application: Application,
    private val dataStore: DataStore<Preferences>,
    private val backupManager: SettingsBackupManager,
    private val stationListUpdater: StationListUpdater,
) : AndroidViewModel(application) {
    private val _result = MutableStateFlow<SettingsResult?>(null)
    private val result: StateFlow<SettingsResult?> = _result.asStateFlow()

    // Tracked separately from [result]: a backup result landing mid-download must not clear
    // the in-flight flag, or the row would re-enable and a second update could start.
    private val isUpdatingStations = MutableStateFlow(false)

    val settings: StateFlow<SettingsUiState?> = combine(
        dataStore.data.map(::settingsUiState),
        result,
        isUpdatingStations,
    ) { settings, result, updating -> settings.copy(result = result, isUpdatingStations = updating) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setShowStreamBitrate(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SHOW_STREAM_BITRATE_KEY] = enabled }
        }
    }

    fun setShowHome(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[SHOW_HOME_KEY] = enabled }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _result.value = when (backupManager.export(uri)) {
                is BackupOperationResult.Success -> SettingsResult.Exported
                is BackupOperationResult.Failure -> SettingsResult.ExportFailed
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _result.value = when (val result = backupManager.import(uri)) {
                is BackupOperationResult.Success -> SettingsResult.Imported(result.value)
                is BackupOperationResult.Failure -> SettingsResult.ImportFailed
            }
        }
    }

    fun updateStationList() {
        if (!isUpdatingStations.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                _result.value = stationListUpdater.update()
                    ?.let { SettingsResult.StationsUpdated(it) }
                    ?: SettingsResult.StationsUpdateFailed
            } finally {
                isUpdatingStations.value = false
            }
        }
    }

    fun onResultShown(result: SettingsResult) {
        _result.compareAndSet(result, null)
    }
}

internal fun settingsUiState(preferences: Preferences): SettingsUiState = SettingsUiState(
    showStreamBitrate = preferences[SHOW_STREAM_BITRATE_KEY] ?: false,
    showHome = preferences[SHOW_HOME_KEY] ?: true,
)

sealed interface SettingsResult {
    data object Exported : SettingsResult
    data class Imported(val stationCount: Int) : SettingsResult
    data object ExportFailed : SettingsResult
    data object ImportFailed : SettingsResult
    data class StationsUpdated(val stationCount: Int) : SettingsResult
    data object StationsUpdateFailed : SettingsResult
}

data class SettingsUiState(
    val showStreamBitrate: Boolean = false,
    val showHome: Boolean = true,
    val result: SettingsResult? = null,
    val isUpdatingStations: Boolean = false,
)
