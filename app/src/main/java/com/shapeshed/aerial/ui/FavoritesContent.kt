package com.shapeshed.aerial.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.*

@Composable
private fun favoritesSortLabel(sort: FavoritesSort): String = stringResource(
    when (sort) {
        FavoritesSort.AZ -> R.string.sort_az
        FavoritesSort.LAST_PLAYED -> R.string.sort_last_played
        FavoritesSort.MOST_PLAYED -> R.string.sort_most_played
    },
)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun FavoritesTabContent(
    stations: List<Station>,
    currentStation: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    homeViewMode: HomeViewMode,
    favoritesSort: FavoritesSort,
    gridState: LazyGridState,
    onScrollToTop: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onPlay: (Station) -> Unit,
    onRemoveFavorite: (Station) -> Unit,
    onHomeViewModeChange: (HomeViewMode) -> Unit,
    onSortSelected: (FavoritesSort) -> Unit,
    onStationLongPress: (Station) -> Unit,
) {
    // Matches Drive's "No starred files": just the illustration and caption, no sort/view
    // controls (meaningless with nothing to sort) and no button — the search bar above is
    // always the way in.
    if (stations.isEmpty()) {
        HomeEmptyState(
            text = stringResource(R.string.no_favorites),
            supportingText = stringResource(R.string.no_favorites_desc),
            icon = Icons.Rounded.FavoriteBorder,
        )
        return
    }

    val tileColor = MaterialTheme.colorScheme.surfaceContainerHigh
    var showSortSheet by remember { mutableStateOf(false) }
    var previousStationKeys by remember { mutableStateOf(emptyList<String>()) }
    val stationKeys = stations.map { it.id.toString() }

    // Play-dependent sorts can move the newly played station while the user is browsing
    // Favorites. Re-anchor the active layout like Home's recently-played shelf, but never
    // interrupt a deliberate mid-list scroll.
    LaunchedEffect(stationKeys, homeViewMode, favoritesSort) {
        val shouldFocus = favoritesSort in setOf(FavoritesSort.LAST_PLAYED, FavoritesSort.MOST_PLAYED) &&
            shouldScrollFavoritesToTop(
                leadingItemChanged = shouldFocusFavoritesItem(previousStationKeys, stationKeys),
                isScrollInProgress = gridState.isScrollInProgress,
            )
        previousStationKeys = stationKeys
        if (!shouldFocus) return@LaunchedEffect
        onScrollToTop()
        gridState.animateScrollToItem(0)
    }

    if (showSortSheet) {
        FavoritesSortSheet(
            current = favoritesSort,
            onSelect = { sort ->
                onSortSelected(sort)
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }

    LazyVerticalGrid(
        columns = if (homeViewMode == HomeViewMode.Cards) {
            GridCells.Adaptive(minSize = 160.dp)
        } else {
            GridCells.Fixed(1)
        },
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding + 16.dp),
        modifier = Modifier.testTag("favorites-content"),
    ) {
        item("favorites-controls", span = { GridItemSpan(maxLineSpan) }) {
            FavoritesHeader(
                favoritesSort = favoritesSort,
                homeViewMode = homeViewMode,
                onSortClick = { showSortSheet = true },
                onHomeViewModeChange = onHomeViewModeChange,
                horizontalPadding = 0.dp,
            )
        }
        gridItems(
            items = stations,
            key = { station -> "favorite-${station.id}" },
            contentType = { "favorite-station" },
        ) { station ->
            val isActive = currentStation?.id == station.id
            Box(modifier = Modifier.animateItem()) {
                when (homeViewMode) {
                    HomeViewMode.Cards -> StationTile(
                        station = station,
                        // The card carries the active state; keep the artwork plate on the
                        // normal tile tone so its circular boundary remains visible.
                        tileColor = tileColor,
                        isActive = isActive,
                        isPlaying = isPlaying && isActive,
                        isBuffering = isBuffering && isActive,
                        onClick = { onPlay(station) },
                        onLongClick = { onStationLongPress(station) },
                        modifier = Modifier
                            .testTag("favorite-card-${station.id}")
                            .padding(bottom = 12.dp),
                    )
                    HomeViewMode.List -> StationListRow(
                        station = station,
                        isActive = isActive,
                        isPlaying = isPlaying && isActive,
                        isBuffering = isBuffering && isActive,
                        onPlay = { onPlay(station) },
                        onDismiss = { onRemoveFavorite(station) },
                        onLongClick = { onStationLongPress(station) },
                        horizontalPadding = 0.dp,
                        modifier = Modifier.testTag("favorite-list-${station.id}"),
                    )
                }
            }
        }
    }
}
@Composable
private fun FavoritesHeader(
    favoritesSort: FavoritesSort,
    homeViewMode: HomeViewMode,
    onSortClick: () -> Unit,
    onHomeViewModeChange: (HomeViewMode) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .testTag("favorites-controls")
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
    ) {
        TextButton(onClick = onSortClick) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(favoritesSortLabel(favoritesSort))
        }
        Spacer(modifier = Modifier.weight(1f))
        HomeViewModeToggle(selected = homeViewMode, onSelected = onHomeViewModeChange)
    }
}

// Single-select sort picker in a modal bottom sheet — the Material 3 pattern used by
// Google apps (list of radio rows under a small title).
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun FavoritesSortSheet(
    current: FavoritesSort,
    onSelect: (FavoritesSort) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = SHEET_ENABLED_VALUES,
        ),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Text(
            text = stringResource(R.string.sort_by),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        FavoritesSort.entries.forEach { sort ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = current == sort,
                        role = Role.RadioButton,
                        onClick = { onSelect(sort) },
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            ) {
                RadioButton(selected = current == sort, onClick = null)
                Text(
                    text = favoritesSortLabel(sort),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun HomeViewModeToggle(
    selected: HomeViewMode,
    onSelected: (HomeViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    ButtonGroup(
        overflowIndicator = {},
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = modifier,
    ) {
        customItem(
            buttonGroupContent = {
                ToggleButton(
                    checked = selected == HomeViewMode.Cards,
                    onCheckedChange = { if (it) onSelected(HomeViewMode.Cards) },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    colors = FilledTonalToggleButtonDefaults.filledTonalToggleButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = stringResource(R.string.home_view_cards),
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            menuContent = {},
        )
        customItem(
            buttonGroupContent = {
                ToggleButton(
                    checked = selected == HomeViewMode.List,
                    onCheckedChange = { if (it) onSelected(HomeViewMode.List) },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    colors = FilledTonalToggleButtonDefaults.filledTonalToggleButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = stringResource(R.string.home_view_list),
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            menuContent = {},
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun StationListRow(
    station: Station,
    isActive: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    val pauseLabel = stringResource(R.string.pause)
    val stationOptionsLabel = stringResource(R.string.station_options)
    val haptic = LocalHapticFeedback.current
    val appLocale = LocalConfiguration.current.locales[0]
    val countryLabel = station.countryCode.takeIf { it.isNotBlank() }
        ?.let { countryName(it, appLocale) }
        ?: station.country
    val dismissState = rememberSwipeToDismissBoxState()
    val swipeBackgroundColor by animateColorAsState(
        targetValue = when (dismissState.targetValue) {
            SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
            SwipeToDismissBoxValue.StartToEnd,
            SwipeToDismissBoxValue.EndToStart,
            -> MaterialTheme.colorScheme.errorContainer
        },
        label = "favorite swipe background",
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(swipeBackgroundColor),
            ) {
                when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd,
                    SwipeToDismissBoxValue.EndToStart,
                    -> Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.remove_from_favorites),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .align(
                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                    Alignment.CenterStart
                                } else {
                                    Alignment.CenterEnd
                                },
                            )
                            .padding(12.dp),
                    )
                    SwipeToDismissBoxValue.Settled -> Unit
                }
            }
        },
        onDismiss = { direction ->
            if (direction != SwipeToDismissBoxValue.Settled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismiss()
            }
        },
    ) {
        Surface(
            color = if (isActive) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 2.dp),
        ) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                modifier = Modifier.combinedClickable(
                    onClick = onPlay,
                    onLongClickLabel = stationOptionsLabel,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                ),
                leadingContent = {
                    StationAvatar(station = station, isActive = isActive, size = 50.dp)
                },
                supportingContent = countryLabel.takeIf { it.isNotBlank() }?.let { label ->
                    {
                        Text(
                            text = label,
                            color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                trailingContent = if (isActive && (isPlaying || isBuffering)) {
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    }
                } else {
                    null
                },
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal const val GRID_LOGO_INSET_FRACTION = 0.85f

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun StationTile(
    station: Station,
    tileColor: androidx.compose.ui.graphics.Color,
    isActive: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showActivityIndicator: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val stationOptionsLabel = stringResource(R.string.station_options)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isActive) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClickLabel = stationOptionsLabel,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
    ) {
        Column {
        val logoModel = logoModelFor(station.logoPath)

        Surface(
            // Same plate treatment as the other artwork surfaces: an adaptive plate behind
            // rendered logos (visible only through transparency), tonal otherwise.
            // Keep the outer circular surface tonal; transparent artwork gets its adaptive
            // plate only on the inset image circle below.
            // Use the base surface inside an active card so the circular artwork border
            // remains visible against the active card container.
            color = if (isActive) MaterialTheme.colorScheme.surfaceContainer else tileColor,
            // Keep the artwork plate circular so transparent regions reveal the tile surface
            // around the logo instead of creating a white square behind it.
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .aspectRatio(1f),
        ) {
            StationLogoContent(
                logoModel = logoModel,
                modifier = Modifier.fillMaxSize(),
                fallbackBackground = if (isActive) MaterialTheme.colorScheme.surfaceContainer else tileColor,
                opaqueArtworkBackground = Color.Transparent,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showActivityIndicator && isActive && (isPlaying || isBuffering)) {
                Spacer(Modifier.width(8.dp))
                if (isBuffering) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f),
                    )
                } else {
                    EqualizerBars(
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(width = 28.dp, height = 22.dp),
                        barCount = 3,
                    )
                }
            }
        }
    }
}
}
