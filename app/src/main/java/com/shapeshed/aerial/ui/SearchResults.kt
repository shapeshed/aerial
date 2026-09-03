package com.shapeshed.aerial.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.request.ImageRequest
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.*

@Composable
internal fun DefaultSearchResults(
    stations: List<RegistryStation>,
    savedStreamUrls: Set<String>,
    savedRegistryKeys: Set<RegistryStationKey>,
    currentStation: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlay: (RegistryStation) -> Unit,
    onPreviewPlay: (RegistryStation) -> Unit,
    onTogglePlayback: () -> Unit,
    onAdd: (RegistryStation) -> Unit,
    onRemove: (RegistryStation) -> Unit,
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    header: @Composable () -> Unit,
) {
    if (stations.isEmpty()) return
    // Search results are a list, not a browsing grid. Keeping one column preserves
    // scan order and prevents tablet layouts from presenting unrelated stations
    // as side-by-side cards.
    LazyVerticalGrid(state = state, columns = GridCells.Fixed(1)) {
        item("search-filter-header", span = { GridItemSpan(maxLineSpan) }) { header() }
        gridItems(
            items = stations,
            key = { it.id },
            contentType = { "registry-result" },
        ) { station ->
            val isActive = currentStation.matches(station)
            RegistryResultItem(
                station = station,
                alreadySaved = station.streamUrl in savedStreamUrls || station.savedKey() in savedRegistryKeys,
                isPlaying = isPlaying && isActive,
                isBuffering = isBuffering && isActive,
                onTap = { onPlay(station) },
                onPreviewPlay = { onPreviewPlay(station) },
                onTogglePlayback = onTogglePlayback,
                onAdd = { onAdd(station) },
                onRemove = { onRemove(station) },
            )
        }
    }
}

// Whether the playing station is this registry entry, matching by stream URL with a
// provider-key fallback for saved stations whose URL was corrected locally.
private fun Station?.matches(registryStation: RegistryStation): Boolean {
    if (this == null) return false
    if (streamUrl == registryStation.streamUrl) return true
    val key = registryStation.savedKey() ?: return false
    return savedKey() == key
}

@Composable
internal fun RecentSearches(
    searches: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    header: @Composable () -> Unit,
) {
    if (searches.isEmpty()) return
    LazyVerticalGrid(state = state, columns = GridCells.Fixed(1)) {
        item("search-filter-header", span = { GridItemSpan(maxLineSpan) }) { header() }
        gridItems(items = searches, key = { it }) { query ->
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(query) },
                leadingContent = {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    IconButton(
                        onClick = { onRemove(query) },
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_remove), modifier = Modifier.size(18.dp))
                    }
                },
            ) {
                Text(query, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
internal fun RegistrySearchResults(
    favoriteResults: List<Station>,
    results: List<RegistryStation>,
    savedStreamUrls: Set<String>,
    savedRegistryKeys: Set<RegistryStationKey>,
    currentStation: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onFavoritePlay: (Station) -> Unit,
    onFavoritePreviewPlay: (Station) -> Unit,
    onPlay: (RegistryStation) -> Unit,
    onPreviewPlay: (RegistryStation) -> Unit,
    onTogglePlayback: () -> Unit,
    onAdd: (RegistryStation) -> Unit,
    onRemove: (RegistryStation) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    header: @Composable () -> Unit,
    onAddManually: (() -> Unit)? = null,
) {
    if (favoriteResults.isEmpty() && results.isEmpty()) {
        Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            header()
            Box(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = androidx.compose.ui.Modifier.size(88.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.Radio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = androidx.compose.ui.Modifier.size(36.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.no_stations_found),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.no_stations_found_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (onAddManually != null) {
                        Spacer(androidx.compose.ui.Modifier.height(4.dp))
                        Button(onClick = onAddManually) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = androidx.compose.ui.Modifier.size(18.dp),
                            )
                            Spacer(androidx.compose.ui.Modifier.width(8.dp))
                            Text(stringResource(R.string.add_your_own_station))
                        }
                    }
                }
            }
        }
    } else {
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(bottom = bottomPadding),
        ) {
            item("search-filter-header", span = { GridItemSpan(maxLineSpan) }) { header() }
            if (favoriteResults.isNotEmpty()) {
                item("favorite-results-header", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.favorites_header),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                gridItems(
                    items = favoriteResults,
                    key = { "favorite-${it.id}" },
                    contentType = { "favorite-result" },
                ) { station ->
                    val isActive = currentStation != null &&
                        (currentStation.id == station.id || currentStation.streamUrl == station.streamUrl)
                    FavoriteResultItem(
                        station = station,
                        isPlaying = isPlaying && isActive,
                        isBuffering = isBuffering && isActive,
                        onTap = { onFavoritePlay(station) },
                        onPreviewPlay = { onFavoritePreviewPlay(station) },
                        onTogglePlayback = onTogglePlayback,
                    )
                }
                if (results.isNotEmpty()) {
                    item("registry-results-header", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.search_stations_header),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                }
            }
            gridItems(
                items = results,
                key = { it.id },
                contentType = { "registry-result" },
            ) { station ->
                val alreadySaved = station.streamUrl in savedStreamUrls || station.savedKey() in savedRegistryKeys
                val isActive = currentStation.matches(station)
                RegistryResultItem(
                    station = station,
                    alreadySaved = alreadySaved,
                    isPlaying = isPlaying && isActive,
                    isBuffering = isBuffering && isActive,
                    onTap = { onPlay(station) },
                    onPreviewPlay = { onPreviewPlay(station) },
                    onTogglePlayback = onTogglePlayback,
                    onAdd = { onAdd(station) },
                    onRemove = { onRemove(station) },
                )
            }
        }
    }
}

@Composable
private fun FavoriteResultItem(
    station: Station,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTap: () -> Unit,
    onPreviewPlay: () -> Unit,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pauseLabel = stringResource(R.string.pause)
    val countryLabel = station.countryCode.takeIf { it.isNotBlank() }
        ?.let { countryName(it, LocalConfiguration.current.locales[0]) }
        ?: station.country
    ListItem(
        modifier = modifier.fillMaxWidth().clickable(onClick = onTap),
        leadingContent = {
            val context = LocalContext.current
            val logoModel = logoModelFor(station.logoPath)
            val imageRequest = logoModel?.let {
                remember(context, it) { ImageRequest.Builder(context).data(it).build() }
            }
            StationLogoCircle(logoModel = imageRequest, size = 50.dp) {
                Text(
                    text = station.name.avatarInitial(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        supportingContent = if (countryLabel.isNotBlank()) {
            {
                Text(
                    text = countryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else null,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = if (isPlaying) onTogglePlayback else onPreviewPlay,
                    shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                ) {
                    when {
                        isBuffering -> CircularWavyProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                        isPlaying -> EqualizerBars(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(width = 28.dp, height = 22.dp)
                                .semantics { contentDescription = pauseLabel },
                            barCount = 3,
                        )
                        else -> Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play))
                    }
                }
                // Static badge, but sized like the icon buttons in the registry rows so the
                // play/heart columns align across the whole results list.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    ) {
        Text(
            text = station.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RegistryResultItem(
    station: RegistryStation,
    alreadySaved: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTap: () -> Unit,
    onPreviewPlay: () -> Unit,
    onTogglePlayback: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pauseLabel = stringResource(R.string.pause)
    // Localize the country from its ISO code (falling back to the registry's own name).
    val countryLabel = station.countryCode.takeIf { it.isNotBlank() }
        ?.let { countryName(it, LocalConfiguration.current.locales[0]) }
        ?: station.country
    ListItem(
        modifier = modifier.fillMaxWidth().clickable(onClick = onTap),
        leadingContent = {
            StationLogoCircle(
                logoModel = logoModelFor(station.logoUrl),
                size = 50.dp,
            ) {
                Text(
                    text = station.name.avatarInitial(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        supportingContent = if (countryLabel.isNotBlank()) {
            { Text(countryLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
            // Preview control (mood-row style): plays in place without closing the search
            // sheet so the station can be auditioned before saving it.
            IconButton(
                onClick = if (isPlaying) onTogglePlayback else onPreviewPlay,
                shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
            ) {
                when {
                    isBuffering -> CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                    isPlaying -> EqualizerBars(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(width = 28.dp, height = 22.dp)
                            .semantics { contentDescription = pauseLabel },
                        barCount = 3,
                    )
                    else -> Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play))
                }
            }
            IconButton(
                onClick = if (alreadySaved) onRemove else onAdd,
                shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
            ) {
                Icon(
                    imageVector = if (alreadySaved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(if (alreadySaved) R.string.remove_from_favorites else R.string.save_to_favorites),
                    tint = if (alreadySaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            }
        },
    ) {
        Text(
            text = station.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
internal fun NoNetworkState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
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
                        imageVector = Icons.Rounded.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.no_internet_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.no_internet_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
