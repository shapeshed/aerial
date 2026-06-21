package com.shapeshed.aerial.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val monochromeLogos by viewModel.monochromeLogos.collectAsStateWithLifecycle()
    val showBitrate by viewModel.showBitrate.collectAsStateWithLifecycle()
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let { viewModel.exportBackup(context, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importBackup(context, it) }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Text(
                    text = "Display",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                item {
                    ListItem(
                        modifier = Modifier.clickable {
                            viewModel.setMonochromeLogos(!monochromeLogos)
                        },
                        headlineContent = { Text("Monochrome logos") },
                        supportingContent = { Text("Show station logos in monochrome") },
                        trailingContent = {
                            Switch(
                                checked = monochromeLogos,
                                onCheckedChange = { viewModel.setMonochromeLogos(it) },
                            )
                        },
                    )
                    HorizontalDivider()
                }
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { viewModel.setShowBitrate(!showBitrate) },
                    headlineContent = { Text("Show bitrate") },
                    supportingContent = { Text("Show exact bitrate instead of HD/SD quality badge") },
                    trailingContent = {
                        Switch(
                            checked = showBitrate,
                            onCheckedChange = { viewModel.setShowBitrate(it) },
                        )
                    },
                )
                HorizontalDivider()
            }
            item {
                Text(
                    text = "Data",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        exportLauncher.launch("aerial-backup.zip")
                    },
                    leadingContent = {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null)
                    },
                    headlineContent = { Text("Export backup") },
                    supportingContent = { Text("Save stations, settings, and local logos") },
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                    },
                    leadingContent = {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null)
                    },
                    headlineContent = { Text("Import backup") },
                    supportingContent = { Text("Merge stations and restore settings from a backup") },
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Radio Browser") },
                    supportingContent = { Text("Station discovery powered by radio-browser.info") },
                )
            }
            item {
                Text(
                    text = "Aerial $versionName",
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
