package com.shapeshed.aerial.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.shapeshed.aerial.data.Station
import java.io.File

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
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val monochromeLogos by viewModel.monochromeLogos.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()

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
        targetValue = if (currentStation != null) 112.dp else 16.dp,
        animationSpec = tween(300),
        label = "fabBottom",
    )
    val fabOffsetY by animateDpAsState(
        targetValue = if (fabVisible) 0.dp else fabBottomPadding + 128.dp,
        animationSpec = tween(220),
        label = "fabOffsetY",
    )
    val stationContentBottomPadding =
        if (currentStation != null) 108.dp else 0.dp

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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
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
                            IconButton(onClick = { searching = false; searchQuery = "" }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close search")
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                }
                            }
                        },
                    )
                } else {
                    TopAppBar(
                        title = { Text("Aerial") },
                        actions = {
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Rounded.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = onSettings) {
                                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
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
                        gridItems(filteredStations, key = { it.id }) { station ->
                            StationCard(
                                station = station,
                                isActive = currentStation?.id == station.id,
                                isPlaying = isPlaying && currentStation?.id == station.id,
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
                            start = 8.dp,
                            end = 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredStations, key = { it.id }) { station ->
                            StationItem(
                                station = station,
                                isActive = currentStation?.id == station.id,
                                isPlaying = isPlaying && currentStation?.id == station.id,
                                isBuffering = isBuffering && currentStation?.id == station.id,
                                supportingText = if (currentStation?.id == station.id)
                                    playbackError ?: currentTrackTitle ?: when {
                                        isBuffering -> "Buffering"
                                        isPlaying -> "Playing"
                                        else -> "Paused"
                                    }
                                else
                                    null,
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

        FloatingActionButtonMenu(
            expanded = fabMenuExpanded,
            button = {
                ToggleFloatingActionButton(
                    checked = fabMenuExpanded,
                    onCheckedChange = { fabMenuExpanded = it },
                    containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                        initialColor = MaterialTheme.colorScheme.primary,
                        finalColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    val iconColor = ToggleFloatingActionButtonDefaults.iconColor(
                        initialColor = MaterialTheme.colorScheme.onPrimary,
                        finalColor = MaterialTheme.colorScheme.onPrimary,
                    )
                    val iconRotation by animateFloatAsState(
                        targetValue = if (fabMenuExpanded) 45f else 0f,
                        animationSpec = tween(220),
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
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            currentStation?.let { station ->
                PlayerBar(
                    station = station,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    currentTrackTitle = currentTrackTitle,
                    playbackError = playbackError,
                    monochromeLogos = monochromeLogos,
                    onToggle = { viewModel.togglePlayback() },
                    onExpand = { showNowPlaying = true },
                )
            }
        }

        AnimatedVisibility(
            visible = showNowPlaying && currentStation != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize(),
        ) {
            currentStation?.let { station ->
                NowPlayingScreen(
                    station = station,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    bitrateKbps = bitrateKbps,
                    currentTrackTitle = currentTrackTitle,
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
        FilterChip(
            selected = showFavoritesOnly,
            onClick = onToggleFavorites,
            label = { Text("Favourites") },
            leadingIcon = {
                Icon(
                    imageVector = if (showFavoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
        Spacer(Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = !isGridView,
                onClick = { onSetGridView(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {},
                label = {
                    Icon(
                        Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = "List view",
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            SegmentedButton(
                selected = isGridView,
                onClick = { onSetGridView(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {},
                label = {
                    Icon(
                        Icons.Rounded.GridView,
                        contentDescription = "Grid view",
                        modifier = Modifier.size(18.dp),
                    )
                },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp),
            )
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
            Button(onClick = onGetStarted) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerBar(
    station: Station,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentTrackTitle: String?,
    playbackError: String?,
    monochromeLogos: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
) {
    val motionScheme = MotionScheme.expressive()
    val haptic = LocalHapticFeedback.current
    val isActive = isPlaying || isBuffering

    val cornerRadius by animateDpAsState(
        targetValue = if (isActive) 50.dp else 20.dp,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "cornerRadius",
    )

    val buttonScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(150),
        label = "buttonScale",
    )

    Surface(
        onClick = onExpand,
        shape = RoundedCornerShape(cornerRadius),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationAvatar(station = station, isActive = true, size = 52.dp, monochrome = monochromeLogos)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playbackError
                        ?: if (isBuffering) "Buffering…"
                        else currentTrackTitle ?: if (isPlaying) "Playing" else "Paused",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isActive)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(14.dp))

            val playPauseCorner by animateDpAsState(
                targetValue = if (isActive) 50.dp else 14.dp,
                animationSpec = motionScheme.defaultSpatialSpec(),
                label = "playPauseCorner",
            )

            Surface(
                shape = RoundedCornerShape(playPauseCorner),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer { scaleX = buttonScale; scaleY = buttonScale }
                    .clip(RoundedCornerShape(playPauseCorner))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = isBuffering to isPlaying,
                        label = "playPauseIcon",
                    ) { (buffering, playing) ->
                        if (buffering) {
                            LoadingIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
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
        }
    }
}

@Composable
fun StationAvatar(
    station: Station,
    isActive: Boolean,
    size: Dp,
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
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    ) {
        if (logoModel != null && !logoFailed) {
            val imageRequest = remember(logoModel) { ImageRequest.Builder(context).data(logoModel).build() }
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
    monochromeLogos: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val cornerRadius by animateDpAsState(
        targetValue = if (isActive) 28.dp else 18.dp,
        animationSpec = tween(250),
        label = "stationItemCorner",
    )
    val containerColor = if (isActive)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor = if (isActive)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface
    val supportingColor = if (isActive)
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        tonalElevation = if (isActive) 3.dp else 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                StationAvatar(
                    station = station,
                    isActive = isActive,
                    size = 50.dp,
                    monochrome = monochromeLogos,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supportingText != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = supportingColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isActive && (isBuffering || isPlaying)) {
                if (isBuffering) {
                    LoadingIndicator(
                        color = contentColor,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp),
                    )
                } else {
                    EqualizerBars(
                        color = contentColor,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(width = 18.dp, height = 16.dp),
                        barCount = 3,
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            Box {
                IconButton(onClick = {
                    showMenu = true
                }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = contentColor,
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = {
                        showMenu = false
                    },
                    shape = MaterialTheme.shapes.medium,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationCard(
    station: Station,
    isActive: Boolean,
    isPlaying: Boolean,
    monochromeLogos: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 3.dp else 0.dp,
            pressedElevation = 4.dp,
            focusedElevation = if (isActive) 3.dp else 0.dp,
            hoveredElevation = 1.dp,
        ),
        shape = MaterialTheme.shapes.large,
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
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                        ) {
                            EqualizerBars(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp),
                                barCount = 3,
                            )
                        }
                    }
                }
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = {
                    showMenu = true
                }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = {
                        showMenu = false
                    },
                    shape = MaterialTheme.shapes.medium,
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
