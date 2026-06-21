package com.shapeshed.aerial.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.ListItem
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.aerial.data.RadioBrowserStation

private sealed class ContentState {
    object Prompt : ContentState()
    object Loading : ContentState()
    data class Error(val error: DiscoveryError) : ContentState()
    data class Results(val items: List<RadioBrowserStation>) : ContentState()
    object Empty : ContentState()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddStationScreen(
    showBitrate: Boolean = false,
    discoveryViewModel: RadioDiscoveryViewModel,
    onAddDiscovered: (RadioBrowserStation) -> Unit,
    onDismiss: () -> Unit,
) {
    val query by discoveryViewModel.query.collectAsStateWithLifecycle()
    val results by discoveryViewModel.results.collectAsStateWithLifecycle()
    val isLoading by discoveryViewModel.isLoading.collectAsStateWithLifecycle()
    val error: DiscoveryError? by discoveryViewModel.error.collectAsStateWithLifecycle()
    val searchedOnce by discoveryViewModel.searchedOnce.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find a station") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            DiscoverContent(
                showBitrate = showBitrate,
                query = query,
                results = results,
                isLoading = isLoading,
                error = error,
                searchedOnce = searchedOnce,
                onQueryChange = discoveryViewModel::onQueryChange,
                onSearch = discoveryViewModel::search,
                onRetry = discoveryViewModel::retry,
                onAddStation = onAddDiscovered,
                onDismiss = onDismiss,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscoverContent(
    showBitrate: Boolean = false,
    query: String,
    results: List<RadioBrowserStation>,
    isLoading: Boolean,
    error: DiscoveryError?,
    searchedOnce: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onAddStation: (RadioBrowserStation) -> Unit,
    onDismiss: () -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme
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
                    expanded = true,
                    onExpandedChange = { if (!it) onDismiss() },
                    placeholder = { Text("Station name") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    },
                )
            },
            expanded = true,
            onExpandedChange = { if (!it) onDismiss() },
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
            windowInsets = WindowInsets(0),
        ) {
            val contentState: ContentState = when {
                isLoading -> ContentState.Loading
                error != null -> ContentState.Error(error)
                results.isNotEmpty() -> ContentState.Results(results)
                searchedOnce -> ContentState.Empty
                else -> ContentState.Prompt
            }

            AnimatedContent(
                targetState = contentState,
                transitionSpec = {
                    (fadeIn(motionScheme.defaultEffectsSpec()) +
                        scaleIn(motionScheme.defaultSpatialSpec(), initialScale = 0.95f))
                        .togetherWith(fadeOut(motionScheme.defaultEffectsSpec()))
                },
                contentKey = { it::class },
                label = "discovery-content",
                modifier = Modifier.fillMaxWidth(),
            ) { state ->
                when (state) {
                    ContentState.Prompt -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(88.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Rounded.Radio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                        Text(
                            text = "Find radio stations",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Search by station name.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    ContentState.Loading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ContainedLoadingIndicator()
                    }

                    is ContentState.Error -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val errorIcon = when (state.error.kind) {
                            ErrorKind.CONNECTIVITY -> Icons.Rounded.WifiOff
                            ErrorKind.SERVICE -> Icons.Rounded.CloudOff
                            ErrorKind.GENERIC -> Icons.Rounded.Warning
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(88.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = errorIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                        Text(
                            text = state.error.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = onRetry,
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Try again")
                        }
                    }

                    ContentState.Empty -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(88.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Rounded.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                        Text(
                            text = "No stations found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Try a different name or check the spelling.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is ContentState.Results -> LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(state.items, key = { it.stationuuid }) { station ->
                            ListItem(
                                modifier = Modifier
                                    .clickable { onAddStation(station) },
                                headlineContent = {
                                    Text(
                                        station.name,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = {
                                    val country = station.displayCountry().takeIf { it.isNotEmpty() }
                                    val qualityLabel = when {
                                        showBitrate && station.bitrate > 0 -> "${station.bitrate} kbps"
                                        !showBitrate && station.bitrate >= 128 -> "HD"
                                        else -> null
                                    }
                                    val parts = listOfNotNull(country, qualityLabel)
                                    if (parts.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = country ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (qualityLabel != null && country != null) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (!showBitrate && station.bitrate >= 128)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                                ) {
                                                    Text(
                                                        text = qualityLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (!showBitrate && station.bitrate >= 128)
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        else
                                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    )
                                                }
                                            } else if (qualityLabel != null) {
                                                Text(
                                                    text = qualityLabel,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                },
                                trailingContent = {
                                    FilledTonalIconButton(
                                        onClick = { onAddStation(station) },
                                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Add ${station.name}")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

    }
}
