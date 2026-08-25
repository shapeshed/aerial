package com.shapeshed.aerial.ui

import android.app.Application
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shapeshed.aerial.SHOW_HOME_KEY
import com.shapeshed.aerial.SHOW_STREAM_BITRATE_KEY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel internal constructor(
    application: Application,
    private val dataStore: DataStore<Preferences>,
    private val backupManager: SettingsBackupManager,
) : AndroidViewModel(application) {
    private val _backupResult = MutableStateFlow<SettingsBackupResult?>(null)
    private val backupResult: StateFlow<SettingsBackupResult?> = _backupResult.asStateFlow()

    val settings: StateFlow<SettingsUiState?> = combine(
        dataStore.data.map(::settingsUiState),
        backupResult,
    ) { settings, result -> settings.copy(backupResult = result) }
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
            _backupResult.value = when (backupManager.export(uri)) {
                is BackupOperationResult.Success -> SettingsBackupResult.Exported
                is BackupOperationResult.Failure -> SettingsBackupResult.ExportFailed
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupResult.value = when (val result = backupManager.import(uri)) {
                is BackupOperationResult.Success -> SettingsBackupResult.Imported(result.value)
                is BackupOperationResult.Failure -> SettingsBackupResult.ImportFailed
            }
        }
    }

    fun onBackupResultShown(result: SettingsBackupResult) {
        _backupResult.compareAndSet(result, null)
    }
}

internal fun settingsUiState(preferences: Preferences): SettingsUiState = SettingsUiState(
    showStreamBitrate = preferences[SHOW_STREAM_BITRATE_KEY] ?: false,
    showHome = preferences[SHOW_HOME_KEY] ?: true,
)

sealed interface SettingsBackupResult {
    data object Exported : SettingsBackupResult
    data class Imported(val stationCount: Int) : SettingsBackupResult
    data object ExportFailed : SettingsBackupResult
    data object ImportFailed : SettingsBackupResult
}

data class SettingsUiState(
    val showStreamBitrate: Boolean = false,
    val showHome: Boolean = true,
    val backupResult: SettingsBackupResult? = null,
)
