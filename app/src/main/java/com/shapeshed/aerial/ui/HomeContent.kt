package com.shapeshed.aerial.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.window.core.layout.WindowSizeClass
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.*

@Composable
internal fun HomeEmptyState(
    text: String,
    supportingText: String,
    icon: ImageVector = Icons.Rounded.Radio,
) {
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}



internal fun shouldFocusRecentlyPlayedItem(previousKeys: List<String>, currentKeys: List<String>): Boolean =
    previousKeys.isNotEmpty() && currentKeys.firstOrNull() != previousKeys.firstOrNull()

internal fun shouldFocusFavoritesItem(previousKeys: List<String>, currentKeys: List<String>): Boolean =
    previousKeys.isNotEmpty() && currentKeys.firstOrNull() != previousKeys.firstOrNull()

internal fun shouldScrollFavoritesToTop(leadingItemChanged: Boolean, isScrollInProgress: Boolean): Boolean =
    leadingItemChanged && !isScrollInProgress

@Composable
internal fun HomeTabContent(
    forYouStations: List<com.shapeshed.aerial.data.RegistryStation>,
    // Null when the selection isn't country-specific; the header drops the country.
    forYouCountry: String?,
    recentlyPlayedStations: List<com.shapeshed.aerial.data.RegistryStation>,
    listState: LazyGridState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onMoodTap: (CuratedMood) -> Unit,
    onRecentlyPlayedStationTap: (com.shapeshed.aerial.data.RegistryStation) -> Unit,
    onFeaturedStationTap: (com.shapeshed.aerial.data.RegistryStation) -> Unit,
    onForYouViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalContentPadding =
        if (currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
                WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
            )
        ) {
            24.dp
        } else {
            16.dp
        }
    // Recently Played can arrive after the grid's first composition. Re-anchor only when the
    // user has not moved the Home list at all; never interrupt an in-progress or mid-list scroll.
    val hasRecentlyPlayed = recentlyPlayedStations.isNotEmpty()
    var hadRecentlyPlayed by rememberSaveable { mutableStateOf(hasRecentlyPlayed) }
    LaunchedEffect(hasRecentlyPlayed) {
        if (hasRecentlyPlayed && !hadRecentlyPlayed &&
            !listState.isScrollInProgress &&
            listState.firstVisibleItemIndex <= 2 &&
            listState.firstVisibleItemScrollOffset == 0
        ) {
            listState.scrollToItem(0)
        }
        hadRecentlyPlayed = hasRecentlyPlayed
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        state = listState,
        contentPadding = PaddingValues(
            start = horizontalContentPadding,
            end = horizontalContentPadding,
            bottom = bottomPadding + 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        if (recentlyPlayedStations.isNotEmpty()) {
            item("recently-played-header", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.recently_played),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            item("recently-played-row", span = { GridItemSpan(maxLineSpan) }) {
                val rowState = rememberLazyListState()
                var previousRecentKeys by remember { mutableStateOf(emptyList<String>()) }
                val recentKeys = remember(recentlyPlayedStations) {
                    recentlyPlayedStations.map { "${it.provider}:${it.providerId}" }
                }
                LaunchedEffect(recentKeys) {
                    if (shouldFocusRecentlyPlayedItem(previousRecentKeys, recentKeys)) {
                        rowState.animateScrollToItem(0)
                    }
                    previousRecentKeys = recentKeys
                }
                LazyRow(
                    state = rowState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    lazyItems(
                        items = recentlyPlayedStations,
                        key = { "recent-${it.provider}-${it.providerId}" },
                        contentType = { "for-you-station" },
                    ) { station ->
                        ForYouStationCard(
                            station = station,
                            onClick = { onRecentlyPlayedStationTap(station) },
                        )
                    }
                }
            }
        }

        if (forYouStations.isNotEmpty()) {
            item("for-you-header", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        // The localized country name is the header; plain "For you" when
                        // the selection isn't country-specific.
                        text = forYouCountry ?: stringResource(R.string.for_you),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onForYouViewAll,
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            // The icon carries the action's label for screen readers.
                            contentDescription = stringResource(R.string.view_all),
                        )
                    }
                }
            }
            item("for-you-row", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    lazyItems(
                        items = forYouStations,
                        key = { "for-you-${it.provider}-${it.providerId}-${it.name}-${it.streamUrl}" },
                        contentType = { "for-you-station" },
                    ) { station ->
                        ForYouStationCard(
                            station = station,
                            onClick = { onFeaturedStationTap(station) },
                        )
                    }
                }
            }
        }

        item("moods-header", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.listen_by_mood),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        gridItems(
            items = CURATED_MOODS,
            key = { "mood-${it.id}" },
            contentType = { "mood-card" },
        ) { mood ->
            MoodCard(
                mood = mood,
                onClick = { onMoodTap(mood) },
            )
        }
    }
}

@Composable
private fun MoodCard(
    mood: CuratedMood,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Same neutral tonal surface as the favourites station tiles, rather than an accent
    // colour — text/icon pairing matches how every other neutral surface in the app reads
    // (onSurface for primary text, onSurfaceVariant for supporting text and icon glyphs).
    val titleColor = MaterialTheme.colorScheme.onSurface
    val supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.height(132.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top-right, clear of the title/description anchored bottom-left, so the icon
            // never sits behind the text on these narrow tiles. Card clips its content to its
            // own shape, so the oversized icon is simply cut off at the corner — no manual
            // clipping needed to get the "bleeding off the edge" effect.
            Icon(
                imageVector = mood.icon,
                contentDescription = null,
                tint = supportingColor.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(108.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-24).dp),
            )
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            ) {
                Text(
                    text = stringResource(mood.titleRes),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(mood.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun MoodDetailScreen(
    stations: List<RegistryStation>,
    currentStation: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    savedStreamUrls: Set<String>,
    savedRegistryKeys: Set<RegistryStationKey>,
    bottomPadding: Dp,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    onAddStation: (RegistryStation) -> Unit,
    onRemoveStation: (RegistryStation) -> Unit,
    onPlayStation: (RegistryStation) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item("mood-actions") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.play))
                }
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save_all))
                }
            }
        }
                lazyItems(
            items = stations,
            key = { "${it.provider}-${it.providerId}-${it.name}-${it.streamUrl}" },
            contentType = { "mood-station" },
        ) { station ->
            val isActive = currentStation?.let { active ->
                active.streamUrl == station.streamUrl ||
                    (active.provider.isNotBlank() &&
                        active.providerId.isNotBlank() &&
                        active.provider == station.provider &&
                        active.providerId == station.providerId)
            } ?: false
            val isSaved = station.streamUrl in savedStreamUrls || station.savedKey() in savedRegistryKeys
            MoodStationRow(
                station = station,
                isActive = isActive,
                isPlaying = isPlaying && isActive,
                isBuffering = isBuffering && isActive,
                onPlay = { onPlayStation(station) },
                isSaved = isSaved,
                onToggleFavorite = {
                    if (isSaved) onRemoveStation(station) else onAddStation(station)
                },
            )
        }
    }
}

@Composable
private fun MoodStationRow(
    station: RegistryStation,
    isActive: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlay: () -> Unit,
    isSaved: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pauseLabel = stringResource(R.string.pause)
    Surface(
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (isActive) 0.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            modifier = Modifier.clickable(onClick = onPlay),
            leadingContent = {
                StationLogoCircle(
                    logoModel = logoModelFor(station.logoUrl),
                    size = 50.dp,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Radio,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp),
                    )
                }
            },
            supportingContent = {
                val countryLabel = station.countryCode.takeIf { it.isNotBlank() }
                    ?.let { countryName(it, LocalConfiguration.current.locales[0]) }
                    ?: station.country
                if (countryLabel.isNotBlank()) {
                    Text(
                        text = countryLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive && (isPlaying || isBuffering)) {
                        when {
                            isBuffering -> CircularWavyProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f),
                            )
                            isPlaying -> EqualizerBars(
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .size(width = 28.dp, height = 22.dp)
                                    .semantics { contentDescription = pauseLabel },
                                barCount = 3,
                            )
                        }
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = stringResource(
                                if (isSaved) R.string.remove_from_favorites else R.string.save_to_favorites,
                            ),
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else {
                                if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            },
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
