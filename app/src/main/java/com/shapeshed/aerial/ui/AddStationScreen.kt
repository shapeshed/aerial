package com.shapeshed.aerial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.aerial.data.RadioBrowserStation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStationScreen(
    discoveryViewModel: RadioDiscoveryViewModel,
    onAddDiscovered: (RadioBrowserStation) -> Unit,
    onDismiss: () -> Unit,
) {
    val query by discoveryViewModel.query.collectAsStateWithLifecycle()
    val results by discoveryViewModel.results.collectAsStateWithLifecycle()
    val isLoading by discoveryViewModel.isLoading.collectAsStateWithLifecycle()
    val error by discoveryViewModel.error.collectAsStateWithLifecycle()
    val searchedOnce by discoveryViewModel.searchedOnce.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find a station") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            DiscoverContent(
                query = query,
                results = results,
                isLoading = isLoading,
                error = error,
                searchedOnce = searchedOnce,
                onQueryChange = discoveryViewModel::onQueryChange,
                onSearch = discoveryViewModel::search,
                onAddStation = onAddDiscovered,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverContent(
    query: String,
    results: List<RadioBrowserStation>,
    isLoading: Boolean,
    error: String?,
    searchedOnce: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAddStation: (RadioBrowserStation) -> Unit,
) {
    val expanded by remember(results, isLoading, error, searchedOnce) {
        derivedStateOf { results.isNotEmpty() || isLoading || error != null || searchedOnce }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { onSearch() },
                    expanded = expanded,
                    onExpandedChange = {},
                    placeholder = { Text("Station name") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    },
                )
            },
            expanded = expanded,
            onExpandedChange = {},
            modifier = Modifier.fillMaxWidth(),
            windowInsets = WindowInsets(0),
        ) {
            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                error != null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                results.isEmpty() && searchedOnce -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No stations found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(results, key = { it.stationuuid }) { station ->
                        ListItem(
                            modifier = Modifier.clickable { onAddStation(station) },
                            headlineContent = { Text(station.name) },
                            supportingContent = {
                                val parts = listOfNotNull(
                                    station.country.takeIf { it.isNotEmpty() },
                                    station.codec.takeIf { it.isNotEmpty() },
                                    "${station.bitrate} kbps".takeIf { station.bitrate > 0 },
                                )
                                Text(parts.joinToString(" · "))
                            },
                            trailingContent = {
                                FilledTonalIconButton(
                                    onClick = { onAddStation(station) },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add ${station.name}")
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        if (!expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Find radio stations by name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
