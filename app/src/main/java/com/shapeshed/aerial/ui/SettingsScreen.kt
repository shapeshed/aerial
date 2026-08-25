package com.shapeshed.aerial.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Build
import com.shapeshed.aerial.BuildConfig
import com.shapeshed.aerial.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val versionName = BuildConfig.VERSION_NAME
    val snackbarHostState = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::importBackup)
    }

    // Do not render default switch values while DataStore is delivering the persisted
    // snapshot. This prevents a first-entry snap from a default to a saved setting.
    val loadedSettings = settings ?: return
    val backupMessage = when (val result = loadedSettings.backupResult) {
        SettingsBackupResult.Exported -> stringResource(R.string.backup_exported)
        is SettingsBackupResult.Imported -> pluralStringResource(
            R.plurals.backup_imported,
            result.stationCount,
            result.stationCount,
        )
        SettingsBackupResult.ExportFailed -> stringResource(R.string.backup_export_failed)
        SettingsBackupResult.ImportFailed -> stringResource(R.string.backup_import_failed)
        null -> null
    }

    LaunchedEffect(loadedSettings.backupResult, backupMessage) {
        val result = loadedSettings.backupResult ?: return@LaunchedEffect
        val message = backupMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onBackupResultShown(result)
    }

    SettingsContent(
        showStreamBitrate = loadedSettings.showStreamBitrate,
        showHome = loadedSettings.showHome,
        versionName = versionName,
        snackbarHostState = snackbarHostState,
        onShowStreamBitrateChange = viewModel::setShowStreamBitrate,
        onShowHomeChange = viewModel::setShowHome,
        onExport = { exportLauncher.launch("aerial-backup.zip") },
        onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsContent(
    showStreamBitrate: Boolean,
    showHome: Boolean,
    versionName: String,
    snackbarHostState: SnackbarHostState,
    onShowStreamBitrateChange: (Boolean) -> Unit,
    onShowHomeChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
            ) {
            item(contentType = "setting") {
                ListItem(
                    modifier = Modifier.clickable { onShowHomeChange(!showHome) },
                    supportingContent = { Text(stringResource(R.string.show_home_desc)) },
                    trailingContent = {
                        Switch(
                            checked = showHome,
                            onCheckedChange = onShowHomeChange,
                        )
                    },
                ) {
                    Text(stringResource(R.string.show_home))
                }
                HorizontalDivider()
            }
            item(contentType = "setting") {
                ListItem(
                    modifier = Modifier.clickable { onShowStreamBitrateChange(!showStreamBitrate) },
                    supportingContent = { Text(stringResource(R.string.show_stream_bitrate_desc)) },
                    trailingContent = {
                        Switch(
                            checked = showStreamBitrate,
                            onCheckedChange = onShowStreamBitrateChange,
                        )
                    },
                ) {
                    Text(stringResource(R.string.show_stream_bitrate))
                }
                HorizontalDivider()
            }
            // In-app language picker only for pre-Android-13; 13+ uses the system per-app setting.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                item(contentType = "setting") {
                    LanguageSettingRow()
                    HorizontalDivider()
                }
            }
            item(contentType = "section") {
                Text(
                    text = stringResource(R.string.section_data),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            item(contentType = "action") {
                ListItem(
                    modifier = Modifier.clickable(onClick = onExport),
                    leadingContent = {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null)
                    },
                    supportingContent = { Text(stringResource(R.string.export_backup_desc)) },
                ) {
                    Text(stringResource(R.string.export_backup))
                }
                HorizontalDivider()
            }
            item(contentType = "action") {
                ListItem(
                    modifier = Modifier.clickable(onClick = onImport),
                    leadingContent = {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null)
                    },
                    supportingContent = { Text(stringResource(R.string.import_backup_desc)) },
                ) {
                    Text(stringResource(R.string.import_backup))
                }
                HorizontalDivider()
            }
            item(contentType = "footer") {
                Text(
                    text = stringResource(R.string.version_format, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            }
            }
        }
    }
}
