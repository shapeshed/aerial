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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil3.compose.SubcomposeAsyncImage
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
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val grayscaleLogos by viewModel.grayscaleLogos.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()

    var showNowPlaying by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var stationPendingDelete by remember { mutableStateOf<Station?>(null) }
    val searchFocusRequester = remember { FocusRequester() }

    val filteredStations = remember(stations, searchQuery) {
        if (searchQuery.isBlank()) stations
        else stations.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val fabBottomPadding by animateDpAsState(
        targetValue = if (currentStation != null) 96.dp else 0.dp,
        animationSpec = tween(300),
        label = "fabBottom",
    )

    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
    BackHandler(enabled = searching) { searching = false; searchQuery = "" }
    BackHandler(enabled = fabMenuExpanded) { fabMenuExpanded = false }
    BackHandler(enabled = stationPendingDelete != null) { stationPendingDelete = null }

    LaunchedEffect(Unit) { viewModel.connect(context) }
    LaunchedEffect(searching) {
        if (searching) searchFocusRequester.requestFocus()
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
                            IconButton(onClick = { viewModel.toggleFavoritesFilter() }) {
                                Icon(
                                    imageVector = if (showFavoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (showFavoritesOnly) "Show all stations" else "Show favorites",
                                    tint = if (showFavoritesOnly) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                )
                            }
                            IconButton(onClick = { viewModel.setGridView(!isGridView) }) {
                                Icon(
                                    imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                    contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
                                )
                            }
                            Box {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                        onClick = { showOverflowMenu = false; onSettings() },
                                    )
                                }
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
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            top = 0.dp,
                            bottom = padding.calculateBottomPadding() + if (currentStation != null) 96.dp else 0.dp,
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
                                grayscaleLogos = grayscaleLogos,
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
                        contentPadding = PaddingValues(
                            top = 0.dp,
                            bottom = padding.calculateBottomPadding() + if (currentStation != null) 96.dp else 0.dp,
                            start = 8.dp,
                            end = 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(filteredStations, key = { it.id }) { station ->
                            Column(modifier = Modifier.animateItem()) {
                                StationItem(
                                    station = station,
                                    isActive = currentStation?.id == station.id,
                                    isPlaying = isPlaying && currentStation?.id == station.id,
                                    grayscaleLogos = grayscaleLogos,
                                    onClick = { viewModel.play(station) },
                                    onEdit = { onEditStation(station.id) },
                                    onDelete = { stationPendingDelete = station },
                                    onToggleFavorite = { viewModel.toggleFavorite(station) },
                                )
                                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
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
                    val rotation by animateFloatAsState(
                        targetValue = if (fabMenuExpanded) 45f else 0f,
                        animationSpec = tween(300),
                        label = "fabRotation",
                    )
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = if (fabMenuExpanded) "Close menu" else "Add station",
                        modifier = Modifier.graphicsLayer { rotationZ = rotation },
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp + fabBottomPadding),
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
                    grayscaleLogos = grayscaleLogos,
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
                    grayscaleLogos = grayscaleLogos,
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
    grayscaleLogos: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
) {
    val motionScheme = MotionScheme.expressive()
    val haptic = LocalHapticFeedback.current

    val cornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 50.dp else 20.dp,
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationAvatar(station = station, isActive = true, size = 36.dp, grayscale = grayscaleLogos)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isBuffering) "Buffering…"
                           else currentTrackTitle ?: if (isPlaying) "Playing" else "Paused",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))

            val playPauseCorner by animateDpAsState(
                targetValue = if (isPlaying) 50.dp else 14.dp,
                animationSpec = motionScheme.defaultSpatialSpec(),
                label = "playPauseCorner",
            )

            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(playPauseCorner),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer { scaleX = buttonScale; scaleY = buttonScale }
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(playPauseCorner))
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 3.dp,
                            )
                        } else {
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp),
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
    grayscale: Boolean = false,
) {
    val context = LocalContext.current
    val logoPath = station.logoPath
    val hasLogo = logoPath.isNotEmpty()
    val logoModel = when {
        logoPath.startsWith("http") -> logoPath
        logoPath.isNotEmpty() -> File(logoPath)
        else -> null
    }
    val grayscaleFilter = if (grayscale) androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
    ) else null

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
        if (hasLogo && logoModel != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(logoModel).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = grayscaleFilter,
                modifier = Modifier.fillMaxSize(),
                error = {
                    Icon(
                        imageVector = Icons.Rounded.Radio,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(size * 0.55f),
                    )
                },
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Radio,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationItem(
    station: Station,
    isActive: Boolean,
    isPlaying: Boolean,
    grayscaleLogos: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(station.name, style = MaterialTheme.typography.bodyLarge)
        },
        leadingContent = {
            Box {
                StationAvatar(
                    station = station,
                    isActive = isActive,
                    size = 40.dp,
                    grayscale = grayscaleLogos,
                )
                if (isActive && isPlaying) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)),
                    ) {
                        EqualizerBars(
                            isPlaying = true,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                            barCount = 3,
                        )
                    }
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
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
                        onClick = { showMenu = false; onToggleFavorite() },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = { showMenu = false; onEdit() },
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
                        onClick = { showMenu = false; onDelete() },
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.surfaceContainerHigh
            else
                androidx.compose.ui.graphics.Color.Transparent,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationCard(
    station: Station,
    isActive: Boolean,
    isPlaying: Boolean,
    grayscaleLogos: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                 else null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                        grayscale = grayscaleLogos,
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
                                isPlaying = true,
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
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
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
                        onClick = { showMenu = false; onToggleFavorite() },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = { showMenu = false; onEdit() },
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
                        onClick = { showMenu = false; onDelete() },
                    )
                }
            }
        }
    }
}
