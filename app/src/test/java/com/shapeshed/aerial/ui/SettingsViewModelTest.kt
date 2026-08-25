package com.shapeshed.aerial.ui

import android.app.Application
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()
    private val viewModels = mutableListOf<SettingsViewModel>()

    @Before
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @After
    fun tearDown() {
        val clear = ViewModel::class.java.getDeclaredMethod("clear\$lifecycle_viewmodel")
        viewModels.forEach { clear.invoke(it) }
        Dispatchers.resetMain()
    }

    @Test
    fun exportResultRemainsInUiStateUntilAcknowledged() = runTest {
        val backupManager = FakeSettingsBackupManager()
        val viewModel = viewModel(backupManager)
        backgroundScope.launch { viewModel.settings.collect() }
        runCurrent()

        viewModel.exportBackup(mock())
        runCurrent()

        assertEquals(SettingsBackupResult.Exported, viewModel.settings.value?.backupResult)

        viewModel.onBackupResultShown(SettingsBackupResult.Exported)
        runCurrent()

        assertNull(viewModel.settings.value?.backupResult)
    }

    @Test
    fun importedStationCountPropagatesToUiState() = runTest {
        val backupManager = FakeSettingsBackupManager(
            importResult = BackupOperationResult.Success(3),
        )
        val viewModel = viewModel(backupManager)
        backgroundScope.launch { viewModel.settings.collect() }
        runCurrent()

        viewModel.importBackup(mock())
        runCurrent()

        assertEquals(SettingsBackupResult.Imported(3), viewModel.settings.value?.backupResult)
    }

    @Test
    fun backupFailurePropagatesAsTypedUiState() = runTest {
        val backupManager = FakeSettingsBackupManager(
            exportResult = BackupOperationResult.Failure(IllegalStateException("disk full")),
        )
        val viewModel = viewModel(backupManager)
        backgroundScope.launch { viewModel.settings.collect() }
        runCurrent()

        viewModel.exportBackup(mock())
        runCurrent()

        assertEquals(SettingsBackupResult.ExportFailed, viewModel.settings.value?.backupResult)
    }

    @Test
    fun cancellationDoesNotBecomeABackupFailure() = runTest {
        val backupManager = FakeSettingsBackupManager(cancelExport = true)
        val viewModel = viewModel(backupManager)
        backgroundScope.launch { viewModel.settings.collect() }
        runCurrent()

        viewModel.exportBackup(mock())
        runCurrent()

        assertNull(viewModel.settings.value?.backupResult)
    }

    @Test
    fun settingChangesPropagateThroughTheCombinedUiState() = runTest {
        val viewModel = viewModel(FakeSettingsBackupManager())
        backgroundScope.launch { viewModel.settings.collect() }
        runCurrent()

        viewModel.setShowHome(false)
        viewModel.setShowStreamBitrate(true)
        runCurrent()

        assertFalse(viewModel.settings.value!!.showHome)
        assertTrue(viewModel.settings.value!!.showStreamBitrate)
    }

    @Test
    fun importFailurePropagatesAsTypedUiState() = runTest {
        val backupManager = FakeSettingsBackupManager(
            importResult = BackupOperationResult.Failure(IllegalArgumentException("invalid backup")),
        )
        val viewModel = viewModel(backupManager)
        backgroundScope.launch { viewModel.settings.collect() }
        runCurrent()

        viewModel.importBackup(mock())
        runCurrent()

        assertEquals(SettingsBackupResult.ImportFailed, viewModel.settings.value?.backupResult)
    }

    private fun viewModel(backupManager: SettingsBackupManager): SettingsViewModel =
        SettingsViewModel(mock<Application>(), MemoryDataStore(), backupManager)
            .also(viewModels::add)

    private class FakeSettingsBackupManager(
        private val exportResult: BackupOperationResult<Unit> = BackupOperationResult.Success(Unit),
        private val importResult: BackupOperationResult<Int> = BackupOperationResult.Success(0),
        private val cancelExport: Boolean = false,
    ) : SettingsBackupManager {
        override suspend fun export(uri: Uri): BackupOperationResult<Unit> {
            if (cancelExport) throw CancellationException("cancelled")
            return exportResult
        }

        override suspend fun import(uri: Uri): BackupOperationResult<Int> = importResult
    }

    private class MemoryDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
