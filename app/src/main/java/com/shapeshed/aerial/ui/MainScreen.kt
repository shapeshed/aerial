package com.shapeshed.aerial.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.toPath
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.shapeshed.aerial.data.Station
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onAddStation: () -> Unit,
    onDiscover: () -> Unit,
    onEditStation: (Long) -> Unit,
    onSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val bitrateKbps by viewModel.bitrateKbps.collectAsStateWithLifecycle()
    val currentTrackTitle by viewModel.currentTrackTitle.collectAsStateWithLifecycle()
    val currentTrackArtworkData by viewModel.currentTrackArtworkData.collectAsStateWithLifecycle()
    val currentTrackArtworkUrl by viewModel.currentTrackArtworkUrl.collectAsStateWithLifecycle()
    val nowPlayingInfo by viewModel.nowPlayingInfo.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val monochromeLogos by viewModel.monochromeLogos.collectAsStateWithLifecycle()
    val showBitrate by viewModel.showBitrate.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val recentlyAddedStationId by viewModel.recentlyAddedStationId.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    var showNowPlaying by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var fabVisible by remember { mutableStateOf(true) }
    var stationPendingDelete by remember { mutableStateOf<Station?>(null) }
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    val filteredStations = remember(stations, searchQuery) {
        if (searchQuery.isBlank()) stations
        else stations.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val fabBottomPadding by animateDpAsState(
        targetValue = if (currentStation != null) 120.dp else 16.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "fabBottom",
    )
    val fabOffsetY by animateDpAsState(
        targetValue = if (fabVisible) 0.dp else fabBottomPadding + 128.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "fabOffsetY",
    )
    val stationContentBottomPadding =
        if (currentStation != null) 128.dp else 0.dp
    val activeNowPlayingInfo = nowPlayingInfo?.takeIf { it.stationId == currentStation?.id }

    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
    BackHandler(enabled = searching) { searching = false; searchQuery = "" }
    BackHandler(enabled = fabMenuExpanded) { fabMenuExpanded = false }
    BackHandler(enabled = stationPendingDelete != null) { stationPendingDelete = null }

    LaunchedEffect(Unit) { viewModel.connect(context) }
    LaunchedEffect(searching) {
        if (searching) searchFocusRequester.requestFocus()
    }
    LaunchedEffect(showFavoritesOnly) {
        listState.scrollToItem(0)
        gridState.scrollToItem(0)
    }
    LaunchedEffect(recentlyAddedStationId, filteredStations, isGridView) {
        val stationId = recentlyAddedStationId ?: return@LaunchedEffect
        val index = filteredStations.indexOfFirst { it.id == stationId }
        if (index == -1) return@LaunchedEffect

        if (isGridView) {
            gridState.animateScrollToItem(index)
        } else {
            listState.animateScrollToItem(index)
        }
        delay(1_500)
        viewModel.clearRecentlyAddedStation(stationId)
    }
    LaunchedEffect(isGridView) {
        fabVisible = true
        val getIndex: () -> Int = if (isGridView) {
            { gridState.firstVisibleItemIndex }
        } else {
            { listState.firstVisibleItemIndex }
        }
        val getOffset: () -> Int = if (isGridView) {
            { gridState.firstVisibleItemScrollOffset }
        } else {
            { listState.firstVisibleItemScrollOffset }
        }
        var previousIndex = getIndex()
        var previousOffset = getOffset()
        snapshotFlow { getIndex() to getOffset() }
            .collect { (index, offset) ->
                if (index > previousIndex || (index == previousIndex && offset > previousOffset)) {
                    fabVisible = false
                    fabMenuExpanded = false
                } else if (index < previousIndex || offset < previousOffset) {
                    fabVisible = true
                }
                previousIndex = index
                previousOffset = offset
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true },
    ) {
        Scaffold(
            modifier = Modifier.semantics { traversalIndex = 0f },
            topBar = {
                if (searching) {
                    TopAppBar(
                        title = {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search stations",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        inner()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester),
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { searching = false; searchQuery = "" },
                                shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close search")
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                }
                            }
                        },
                    )
                } else {
                    TopAppBar(
                        title = { Text("Aerial") },
                        actions = {
                            AppBarRow {
                                customItem(
                                    appbarContent = {
                                        IconButton(
                                            onClick = { searching = true },
                                            shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                                        ) {
                                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                                        }
                                    },
                                    menuContent = { menuState ->
                                        DropdownMenuItem(
                                            text = { Text("Search") },
                                            onClick = { searching = true; menuState.dismiss() },
                                        )
                                    },
                                )
                                customItem(
                                    appbarContent = {
                                        IconButton(
                                            onClick = onSettings,
                                            shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                                        ) {
                                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                                        }
                                    },
                                    menuContent = { menuState ->
                                        DropdownMenuItem(
                                            text = { Text("Settings") },
                                            onClick = { onSettings(); menuState.dismiss() },
                                        )
                                    },
                                )
                            }
                        },
                    )
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
            ) {
                if (!isOnline) {
                    NoNetworkState()
                } else {
                if (stations.isNotEmpty()) {
                    StationControlRow(
                        showFavoritesOnly = showFavoritesOnly,
                        isGridView = isGridView,
                        onToggleFavorites = { viewModel.toggleFavoritesFilter() },
                        onSetGridView = { viewModel.setGridView(it) },
                    )
                }
                if (filteredStations.isEmpty()) {
                    if (stations.isEmpty() && searchQuery.isBlank() && !showFavoritesOnly) {
                        NoStationsEmptyState(onGetStarted = onDiscover)
                    } else {
                        HomeEmptyState(
                            text = when {
                                searchQuery.isNotBlank() -> "No matching stations"
                                showFavoritesOnly -> "No favorites yet"
                                else -> "No stations yet"
                            },
                            supportingText = when {
                                searchQuery.isNotBlank() -> "Try a different station name."
                                showFavoritesOnly -> "Tap the heart on a station to find it here."
                                else -> "Add a station to start listening."
                            },
                        )
                    }
                } else if (isGridView) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            top = 0.dp,
                            bottom = padding.calculateBottomPadding() + stationContentBottomPadding,
                            start = 8.dp,
                            end = 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        gridItems(
                            items = filteredStations,
                            key = { it.id },
                            contentType = { "station-card" },
                        ) { station ->
                            StationCard(
                                station = station,
                                isActive = currentStation?.id == station.id,
                                isPlaying = isPlaying && currentStation?.id == station.id,
                                isRecentlyAdded = recentlyAddedStationId == station.id,
                                monochromeLogos = monochromeLogos,
                                onClick = { viewModel.play(station) },
                                onEdit = { onEditStation(station.id) },
                                onDelete = { stationPendingDelete = station },
                                onToggleFavorite = { viewModel.toggleFavorite(station) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            top = 0.dp,
                            bottom = padding.calculateBottomPadding() + stationContentBottomPadding,
                        ),
                    ) {
                        items(
                            items = filteredStations,
                            key = { it.id },
                            contentType = { "station-row" },
                        ) { station ->
                            val stationNowPlayingText = when {
                                activeNowPlayingInfo?.trackTitle != null -> activeNowPlayingInfo.trackTitle
                                activeNowPlayingInfo?.programmeTitle != null -> activeNowPlayingInfo.programmeTitle
                                activeNowPlayingInfo?.title != null -> activeNowPlayingInfo.title
                                else -> currentTrackTitle
                            }
                            StationItem(
                                station = station,
                                isActive = currentStation?.id == station.id,
                                isPlaying = isPlaying && currentStation?.id == station.id,
                                isBuffering = isBuffering && currentStation?.id == station.id,
                                supportingText = if (currentStation?.id == station.id) {
                                    playbackError
                                        ?: stationNowPlayingText
                                        ?: when {
                                            isBuffering -> "Buffering"
                                            isPlaying -> "Playing"
                                            else -> "Paused"
                                        }
                                } else {
                                    null
                                },
                                isRecentlyAdded = recentlyAddedStationId == station.id,
                                monochromeLogos = monochromeLogos,
                                onClick = { viewModel.play(station) },
                                onEdit = { onEditStation(station.id) },
                                onDelete = { stationPendingDelete = station },
                                onToggleFavorite = { viewModel.toggleFavorite(station) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
                }
            }
        }

        FloatingActionButtonMenu(
            expanded = fabMenuExpanded,
            button = {
                ToggleFloatingActionButton(
                    checked = fabMenuExpanded,
                    onCheckedChange = { fabMenuExpanded = it },
                ) {
                    val iconColor = ToggleFloatingActionButtonDefaults.iconColor()
                    val iconRotation by animateFloatAsState(
                        targetValue = if (fabMenuExpanded) 45f else 0f,
                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                        label = "fabIconRotation",
                    )

                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = if (fabMenuExpanded) "Close menu" else "Add station",
                        tint = iconColor(checkedProgress),
                        modifier = Modifier.graphicsLayer { rotationZ = iconRotation },
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = fabOffsetY)
                .padding(end = 16.dp, bottom = fabBottomPadding),
        ) {
            FloatingActionButtonMenuItem(
                onClick = { fabMenuExpanded = false; onAddStation() },
                text = { Text("Add manually") },
                icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            )
            FloatingActionButtonMenuItem(
                onClick = { fabMenuExpanded = false; onDiscover() },
                text = { Text("Find a station") },
                icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            )
        }

        AnimatedVisibility(
            visible = currentStation != null,
            enter = slideInVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .semantics { traversalIndex = 1f }
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        ) {
            currentStation?.let { station ->
                val miniPlayerTitle = activeNowPlayingInfo?.trackArtist
                    ?: activeNowPlayingInfo?.title
                    ?: station.name
                val miniPlayerSubtitle = playbackError
                    ?: if (isBuffering) "Buffering…"
                    else when {
                        activeNowPlayingInfo?.trackTitle != null &&
                            activeNowPlayingInfo.trackTitle != miniPlayerTitle -> activeNowPlayingInfo.trackTitle
                        activeNowPlayingInfo?.subtitle != null &&
                            activeNowPlayingInfo.subtitle != miniPlayerTitle -> activeNowPlayingInfo.subtitle
                        currentTrackTitle != null && currentTrackTitle != miniPlayerTitle -> currentTrackTitle
                        isPlaying -> "Playing"
                        else -> "Paused"
                    }
                BoxWithConstraints {
                    val isActive = isPlaying || isBuffering
                    val playPauseCorner by animateDpAsState(
                        targetValue = if (isActive) 50.dp else 14.dp,
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        label = "playPauseCorner",
                    )
                    // contentPadding(36dp both) + leadingAvatar(52dp) + trailingButton(52dp) = 140dp overhead
                    val columnWidth = (maxWidth - 140.dp).coerceAtLeast(80.dp)
                    HorizontalFloatingToolbar(
                        expanded = true,
                        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        leadingContent = {
                            StationAvatar(station = station, isActive = true, size = 52.dp, monochrome = monochromeLogos)
                        },
                        trailingContent = {
                            Surface(
                                shape = RoundedCornerShape(playPauseCorner),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(playPauseCorner))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.togglePlayback()
                                    },
                            ) {
                                val motionScheme = MaterialTheme.motionScheme
                                Box(contentAlignment = Alignment.Center) {
                                    AnimatedContent(
                                        targetState = isBuffering to isPlaying,
                                        transitionSpec = {
                                            (fadeIn(motionScheme.defaultEffectsSpec()) +
                                                scaleIn(motionScheme.defaultSpatialSpec(), initialScale = 0.85f))
                                                .togetherWith(fadeOut(motionScheme.defaultEffectsSpec()))
                                        },
                                        label = "playPauseIcon",
                                    ) { (buffering, playing) ->
                                        if (buffering) {
                                            CircularWavyProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (playing) "Pause" else "Play",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(30.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    ) {
                        Column(
                            modifier = Modifier
                                .width(columnWidth)
                                .clickable { showNowPlaying = true }
                                .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = miniPlayerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (miniPlayerSubtitle != null) {
                            Text(
                                text = miniPlayerSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            }
        }

        AnimatedVisibility(
            visible = showNowPlaying && currentStation != null,
            enter = slideInVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize(),
        ) {
            currentStation?.let { station ->
                NowPlayingScreen(
                    station = station,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    bitrateKbps = bitrateKbps,
                    showBitrate = showBitrate,
                    nowPlayingInfo = activeNowPlayingInfo,
                    currentTrackTitle = currentTrackTitle,
                    currentTrackArtworkData = currentTrackArtworkData,
                    currentTrackArtworkUrl = currentTrackArtworkUrl,
                    monochromeLogos = monochromeLogos,
                    onToggle = { viewModel.togglePlayback() },
                    onToggleFavorite = { viewModel.toggleFavorite(station) },
                    onDismiss = { showNowPlaying = false },
                )
            }
        }
    }

    stationPendingDelete?.let { station ->
        AlertDialog(
            onDismissRequest = { stationPendingDelete = null },
            title = { Text("Delete station?") },
            text = { Text("Remove ${station.name} from your stations.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStation(station)
                        stationPendingDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { stationPendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun StationControlRow(
    showFavoritesOnly: Boolean,
    isGridView: Boolean,
    onToggleFavorites: () -> Unit,
    onSetGridView: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleButton(
            checked = showFavoritesOnly,
            onCheckedChange = { onToggleFavorites() },
            colors = ToggleButtonDefaults.tonalToggleButtonColors(),
        ) {
            Icon(
                imageVector = if (showFavoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Favourites")
        }
        Spacer(Modifier.weight(1f))
        ButtonGroup(
            overflowIndicator = {},
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            customItem(
                buttonGroupContent = {
                    ToggleButton(
                        checked = !isGridView,
                        onCheckedChange = { if (it) onSetGridView(false) },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ViewList,
                            contentDescription = "List view",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                menuContent = {},
            )
            customItem(
                buttonGroupContent = {
                    ToggleButton(
                        checked = isGridView,
                        onCheckedChange = { if (it) onSetGridView(true) },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        colors = ToggleButtonDefaults.tonalToggleButtonColors(),
                    ) {
                        Icon(
                            Icons.Rounded.GridView,
                            contentDescription = "Grid view",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                menuContent = {},
            )
        }
    }
}

@Composable
private fun NoNetworkState() {
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
                text = "No internet connection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Radio streams require an active internet connection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NoStationsEmptyState(onGetStarted: () -> Unit) {
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
                        imageVector = Icons.Rounded.Radio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Text(
                text = "No stations yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Add a station to start listening.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onGetStarted,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text("Get started")
            }
        }
    }
}

@Composable
private fun HomeEmptyState(
    text: String,
    supportingText: String,
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
                        imageVector = Icons.Rounded.Radio,
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



@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun StationAvatar(
    station: Station,
    isActive: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    monochrome: Boolean = false,
) {
    val context = LocalContext.current
    val logoPath = station.logoPath
    val logoModel = when {
        logoPath.startsWith("http") -> logoPath
        logoPath.isNotEmpty() -> File(logoPath)
        else -> null
    }
    val primary = MaterialTheme.colorScheme.primary
    val themeColorFilter = remember(monochrome, primary) {
        if (!monochrome || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return@remember null
        androidx.compose.ui.graphics.ColorFilter.tint(primary, androidx.compose.ui.graphics.BlendMode.Color)
    }
    var logoFailed by remember(logoModel) { mutableStateOf(false) }

    val iconTint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                   else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    ) {
        if (logoModel != null && !logoFailed) {
            val imageRequest = remember(context, logoModel) { ImageRequest.Builder(context).data(logoModel).build() }
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = themeColorFilter,
                onError = { logoFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Radio,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StationItem(
    station: Station,
    isActive: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    supportingText: String?,
    isRecentlyAdded: Boolean,
    monochromeLogos: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val motionScheme = MaterialTheme.motionScheme
    val containerColor by animateColorAsState(
        targetValue = if (isRecentlyAdded) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f),
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "stationItemContainer",
    )
    val avatarScale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "stationAvatarScale",
    )
    val titleColor = when {
        isRecentlyAdded -> MaterialTheme.colorScheme.onPrimaryContainer
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val actionColor = if (isRecentlyAdded)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant
    val supportingColor = if (isRecentlyAdded)
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = containerColor),
        leadingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer { scaleX = avatarScale; scaleY = avatarScale },
            ) {
                StationAvatar(
                    station = station,
                    isActive = isActive,
                    size = 50.dp,
                    monochrome = monochromeLogos,
                )
                if (isActive && (isBuffering || isPlaying)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)),
                    ) {
                        if (isBuffering) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            )
                        } else {
                            EqualizerBars(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(width = 22.dp, height = 18.dp),
                                barCount = 3,
                            )
                        }
                    }
                }
            }
        },
        headlineContent = {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = if (supportingText != null) {
            {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else null,
        trailingContent = {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = actionColor,
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = {
                        showMenu = false
                    },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (station.isFavorite) "Remove from favorites" else "Add to favorites")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (station.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = if (station.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StationCard(
    station: Station,
    isActive: Boolean,
    isPlaying: Boolean,
    isRecentlyAdded: Boolean,
    monochromeLogos: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val containerColor by animateColorAsState(
        targetValue = if (isRecentlyAdded) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "stationCardContainer",
    )
    val contentColor = if (isRecentlyAdded) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    val titleColor = if (isRecentlyAdded) MaterialTheme.colorScheme.onPrimaryContainer
        else if (isActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isRecentlyAdded) 2.dp else 0.dp,
            pressedElevation = 4.dp,
            focusedElevation = if (isRecentlyAdded) 2.dp else 0.dp,
            hoveredElevation = 1.dp,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.aspectRatio(1f),
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 44.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StationAvatar(
                        station = station,
                        isActive = isActive,
                        size = 72.dp,
                        monochrome = monochromeLogos,
                    )
                    if (isActive && isPlaying) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)),
                        ) {
                            EqualizerBars(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp),
                                barCount = 3,
                            )
                        }
                    }
                }
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = titleColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { showMenu = true },
                    shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = {
                        showMenu = false
                    },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (station.isFavorite) "Remove from favorites" else "Add to favorites")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (station.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = if (station.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
