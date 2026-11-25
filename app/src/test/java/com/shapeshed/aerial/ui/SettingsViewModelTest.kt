package com.shapeshed.aerial.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.shapeshed.aerial.data.StationListUpdater
import com.shapeshed.aerial.testing.MemoryDataStore
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

        assertEquals(SettingsResult.Exported, viewModel.settings.value?.result)

        viewModel.onResultShown(SettingsResult.Exported)
        runCurrent()

        assertNull(viewModel.settings.value?.result)
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

        assertEquals(SettingsResult.Imported(3), viewModel.settings.value?.result)
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

        assertEquals(SettingsResult.ExportFailed, viewModel.settings.value?.result)
    }

    @Test
    fun cancellationDoesNotBecomeABackupFailure() = runTest {
        val backupManager = FakeSettingsBackupManager(cancelExport = true)
        val viewModel = viewModel(backupManager)
        backgroundScope.launch { viewModel.settings.collect() }
        runCurrent()

        viewModel.exportBackup(mock())
        runCurrent()

        assertNull(viewModel.settings.value?.result)
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

        assertEquals(SettingsResult.ImportFailed, viewModel.settings.value?.result)
    }

    @Test
    fun stationListUpdateReportsTheImportedCount() = runTest {
        val viewModel = viewModel(FakeSettingsBackupManager(), FakeStationListUpdater(stationCount = 44_142))
        backgroundScope.launch { viewModel.settings.collect() }
        runCurrent()

        viewModel.updateStationList()
        runCurrent()

        assertEquals(SettingsResult.StationsUpdated(44_142), viewModel.settings.value?.result)
    }

    @Test
    fun stationListUpdateWithoutAReplacementKeepsTheBundledList() = runTest {
        val viewModel = viewModel(FakeSettingsBackupManager(), FakeStationListUpdater(stationCount = null))
        backgroundScope.launch { viewModel.settings.collect() }
        runCurrent()

        viewModel.updateStationList()
        runCurrent()

        assertEquals(SettingsResult.StationsUpdateFailed, viewModel.settings.value?.result)
    }

    @Test
    fun backupResultDuringAnUpdateKeepsTheUpdateRunning() = runTest {
        val updater = BlockingStationListUpdater()
        val viewModel = viewModel(FakeSettingsBackupManager(), updater)
        backgroundScope.launch { viewModel.settings.collect() }
        runCurrent()

        viewModel.updateStationList()
        runCurrent()
        assertTrue(viewModel.settings.value!!.isUpdatingStations)

        // A backup result landing mid-download must not clear the in-flight flag.
        viewModel.exportBackup(mock())
        runCurrent()
        assertEquals(SettingsResult.Exported, viewModel.settings.value?.result)
        assertTrue(viewModel.settings.value!!.isUpdatingStations)

        viewModel.updateStationList()
        runCurrent()
        assertEquals(1, updater.starts)

        updater.finish(44_142)
        runCurrent()
        assertFalse(viewModel.settings.value!!.isUpdatingStations)
        assertEquals(SettingsResult.StationsUpdated(44_142), viewModel.settings.value?.result)
    }

    private fun viewModel(
        backupManager: SettingsBackupManager,
        stationListUpdater: StationListUpdater = FakeStationListUpdater(),
    ): SettingsViewModel = SettingsViewModel(
        mock<Application>(),
        MemoryDataStore(),
        backupManager,
        stationListUpdater,
    ).also(viewModels::add)

    private class FakeStationListUpdater(private val stationCount: Int? = 0) : StationListUpdater {
        override suspend fun update(): Int? = stationCount
    }

    /** Holds the update open so the busy flag can be observed, and counts how many times one starts. */
    private class BlockingStationListUpdater : StationListUpdater {
        private val result = CompletableDeferred<Int?>()
        var starts = 0
            private set

        override suspend fun update(): Int? {
            starts++
            return result.await()
        }

        fun finish(stationCount: Int?) = result.complete(stationCount)
    }

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
}
