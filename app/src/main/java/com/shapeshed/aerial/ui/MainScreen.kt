package com.shapeshed.aerial.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Nightlife
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.ListItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun countryName(code: String): String =
    java.util.Locale.Builder().setRegion(code).build().getDisplayCountry().ifBlank { code }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onAddStation: () -> Unit,
    onSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val currentTrackTitle by viewModel.currentTrackTitle.collectAsStateWithLifecycle()
    val currentTrackArtworkData by viewModel.currentTrackArtworkData.collectAsStateWithLifecycle()
    val currentTrackArtworkUrl by viewModel.currentTrackArtworkUrl.collectAsStateWithLifecycle()
    val nowPlayingInfo by viewModel.nowPlayingInfo.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val recentlyAddedStationId by viewModel.recentlyAddedStationId.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val showNowPlaying by viewModel.showNowPlaying.collectAsStateWithLifecycle()
    val registrySearchResults by viewModel.registrySearchResults.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val featuredStations by viewModel.featuredStations.collectAsStateWithLifecycle()
    val selectedCountries: Set<String> by viewModel.selectedCountries.collectAsStateWithLifecycle()
    val selectedTags: Set<String> by viewModel.selectedTags.collectAsStateWithLifecycle()
    val availableCountries: List<String> by viewModel.availableCountries.collectAsStateWithLifecycle()

    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberContainedSearchBarState()
    val isSearchExpanded by remember { derivedStateOf { searchBarState.currentValue == SearchBarValue.Expanded } }
    val searchQueryText by remember { derivedStateOf { textFieldState.text.toString() } }
    var activeFilterPicker by remember { mutableStateOf<FilterPickerType?>(null) }
    var countryFilterQuery by remember { mutableStateOf("") }
    var genreFilterQuery by remember { mutableStateOf("") }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val savedStreamUrls = remember(stations) { stations.map { it.streamUrl }.toSet() }

    val stationContentBottomPadding = if (currentStation != null) 128.dp else 0.dp
    val activeNowPlayingInfo = nowPlayingInfo?.takeIf { it.stationId == currentStation?.id }

    BackHandler(enabled = showNowPlaying) { viewModel.setShowNowPlaying(false) }
    BackHandler(enabled = isSearchExpanded && activeFilterPicker == null) {
        textFieldState.edit { replace(0, length, "") }
        scope.launch { searchBarState.animateToCollapsed() }
    }
    BackHandler(enabled = activeFilterPicker != null) {
        activeFilterPicker = null
    }
    LaunchedEffect(Unit) { viewModel.connect(context) }
    LaunchedEffect(searchQueryText) {
        viewModel.searchRegistry(searchQueryText)
    }
    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) viewModel.searchRegistry(searchQueryText)
    }
    LaunchedEffect(recentlyAddedStationId) {
        val stationId = recentlyAddedStationId ?: return@LaunchedEffect
        delay(1_500)
        viewModel.clearRecentlyAddedStation(stationId)
    }
    fun openRegistrySearch() {
        scope.launch { searchBarState.animateToExpanded() }
    }

    val searchInputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { viewModel.saveRecentSearch(it) },
            placeholder = { Text("Search stations") },
            leadingIcon = {
                if (isSearchExpanded) {
                    IconButton(
                        onClick = {
                            textFieldState.edit { replace(0, length, "") }
                            scope.launch { searchBarState.animateToCollapsed() }
                        },
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    ) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            },
            trailingIcon = {
                if (isSearchExpanded && searchQueryText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            textFieldState.edit { replace(0, length, "") }
                        },
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                val appIconBitmap = remember {
                    val drawable = context.packageManager.getApplicationIcon(context.packageName)
                    android.graphics.Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        android.graphics.Bitmap.Config.ARGB_8888,
                    ).also { bmp ->
                        val canvas = android.graphics.Canvas(bmp)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(28.dp),
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = appIconBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape),
                    )
                    Text(
                        text = "Aerial",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    label = { Text("Add a station") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onAddStation()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSettings()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        },
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true },
    ) {
        Scaffold(
            modifier = Modifier.semantics { traversalIndex = 0f },
            contentWindowInsets = WindowInsets.navigationBars,
        ) { padding ->
            Column(Modifier.fillMaxSize()) {
                SearchBar(
                    state = searchBarState,
                    inputField = searchInputField,
                    colors = SearchBarDefaults.containedColors(searchBarState),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 8.dp, bottom = 8.dp),
                )

                if (!isOnline) {
                    NoNetworkState()
                } else {
                    HomeContent(
                        stations = stations,
                        currentStation = currentStation,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        allTags = allTags,
                        featuredStations = featuredStations,
                        bottomPadding = padding.calculateBottomPadding() + stationContentBottomPadding,
                        onPlay = { viewModel.play(it) },
                        onAddTapped = ::openRegistrySearch,
                        onCategoryTap = { viewModel.playRandomFromCategory(it) },
                        onFeaturedStationTap = { viewModel.playFromRegistry(it) },
                    )
                }
            }
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
                val miniPlayerTitle = activeNowPlayingInfo?.track?.artist
                    ?: activeNowPlayingInfo?.programmeTitle
                    ?: station.name
                val miniPlayerSubtitle = playbackError
                    ?: if (isBuffering) "Buffering…"
                    else when {
                        activeNowPlayingInfo?.track?.title != null &&
                            activeNowPlayingInfo.track.title != miniPlayerTitle -> activeNowPlayingInfo.track.title
                        activeNowPlayingInfo?.programmeSubtitle != null &&
                            activeNowPlayingInfo.programmeSubtitle != miniPlayerTitle -> activeNowPlayingInfo.programmeSubtitle
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
                            val miniArtworkModel = with(activeNowPlayingInfo) {
                                when {
                                    this?.track?.artworkData != null -> this.track.artworkData
                                    this?.track?.artworkUrl?.isNotBlank() == true -> this.track.artworkUrl
                                    this?.artworkData != null -> this.artworkData
                                    this?.artworkUrl?.isNotBlank() == true -> this.artworkUrl
                                    else -> null
                                }
                            }
                            var miniArtworkFailed by remember(miniArtworkModel) { mutableStateOf(false) }
                            AnimatedContent(
                                targetState = if (miniArtworkFailed) null else miniArtworkModel,
                                transitionSpec = {
                                    fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                                },
                                label = "miniPlayerArtwork",
                            ) { model ->
                                if (model != null) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.size(52.dp),
                                    ) {
                                        AsyncImage(
                                            model = model,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            onError = { miniArtworkFailed = true },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                } else {
                                    StationAvatar(station = station, isActive = true, size = 52.dp)
                                }
                            }
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
                                .clickable { viewModel.setShowNowPlaying(true) }
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
            visible = showNowPlaying,
            enter = slideInVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize(),
        ) {
            val station = currentStation
            if (station != null) {
                NowPlayingScreen(
                    station = station,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    nowPlayingInfo = activeNowPlayingInfo,
                    currentTrackTitle = currentTrackTitle,
                    currentTrackArtworkData = currentTrackArtworkData,
                    currentTrackArtworkUrl = currentTrackArtworkUrl,
                    onToggle = { viewModel.togglePlayback() },
                    onToggleFavorite = { viewModel.toggleFavorite(station) },
                    onDismiss = { viewModel.setShowNowPlaying(false) },
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
            }
        }

        ExpandedFullScreenContainedSearchBar(
            state = searchBarState,
            inputField = searchInputField,
        ) {
            if (!isOnline) {
                NoNetworkState()
            } else if (activeFilterPicker != null) {
                AnimatedContent(
                    targetState = activeFilterPicker,
                    transitionSpec = {
                        slideInVertically { it }.togetherWith(slideOutVertically { it })
                    },
                    label = "filterPicker",
                ) { picker ->
                    when (picker) {
                        FilterPickerType.Country -> FilterPickerScreen(
                            title = "Country",
                            searchLabel = "Search countries",
                            query = countryFilterQuery,
                            onQueryChange = { countryFilterQuery = it },
                            items = availableCountries,
                            selectedItems = selectedCountries,
                            displayName = ::countryName,
                            onToggle = { viewModel.toggleCountryFilter(it) },
                            onClear = { viewModel.clearCountryFilter() },
                            onBack = { activeFilterPicker = null },
                        )
                        FilterPickerType.Genre -> FilterPickerScreen(
                            title = "Genre",
                            searchLabel = "Search genres",
                            query = genreFilterQuery,
                            onQueryChange = { genreFilterQuery = it },
                            items = allTags,
                            selectedItems = selectedTags,
                            displayName = { it },
                            onToggle = { viewModel.toggleTagFilter(it) },
                            onClear = { viewModel.clearTagFilter() },
                            onBack = { activeFilterPicker = null },
                        )
                        null -> Unit
                    }
                }
            } else {
                val hasFilters = selectedCountries.isNotEmpty() || selectedTags.isNotEmpty()
                SearchFilterRow(
                    selectedCountries = selectedCountries,
                    selectedTags = selectedTags,
                    onCountryClick = { activeFilterPicker = FilterPickerType.Country },
                    onGenreClick = { activeFilterPicker = FilterPickerType.Genre },
                    onClearAll = { viewModel.clearAllFilters() },
                    hasFilters = hasFilters,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (searchQueryText.isBlank() && !hasFilters) {
                    RecentSearches(
                        searches = recentSearches,
                        onSelect = { query ->
                            textFieldState.edit { replace(0, length, query) }
                            viewModel.searchRegistry(query)
                        },
                        onRemove = { viewModel.removeRecentSearch(it) },
                    )
                } else {
                    RegistrySearchResults(
                        results = registrySearchResults,
                        savedStreamUrls = savedStreamUrls,
                        onPlay = { station ->
                            viewModel.saveRecentSearch(searchQueryText)
                            viewModel.playFromRegistry(station)
                            textFieldState.edit { replace(0, length, "") }
                            viewModel.clearAllFilters()
                            scope.launch { searchBarState.animateToCollapsed() }
                        },
                        onAdd = { viewModel.addFromRegistry(it) },
                        onRemove = { viewModel.removeFromRegistry(it) },
                        bottomPadding = 0.dp,
                    )
                }
            }
        }
    }
    } // end ModalNavigationDrawer

}


@Composable
private fun RecentSearches(
    searches: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (searches.isEmpty()) return
    LazyColumn {
        items(items = searches, key = { it }) { query ->
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(query) },
                leadingContent = {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                headlineContent = {
                    Text(query, style = MaterialTheme.typography.bodyLarge)
                },
                trailingContent = {
                    IconButton(
                        onClick = { onRemove(query) },
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                    }
                },
            )
        }
    }
}

@Composable
private fun RegistrySearchResults(
    results: List<RegistryStation>,
    savedStreamUrls: Set<String>,
    onPlay: (RegistryStation) -> Unit,
    onAdd: (RegistryStation) -> Unit,
    onRemove: (RegistryStation) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    if (results.isEmpty()) {
        HomeEmptyState(
            text = "No stations found",
            supportingText = "Try searching by name, country, or genre.",
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomPadding),
        ) {
            items(
                items = results,
                key = { it.id },
                contentType = { "registry-result" },
            ) { station ->
                val alreadySaved = station.streamUrl in savedStreamUrls
                RegistryResultItem(
                    station = station,
                    alreadySaved = alreadySaved,
                    onTap = { onPlay(station) },
                    onAdd = { onAdd(station) },
                    onRemove = { onRemove(station) },
                )
            }
        }
    }
}

@Composable
private fun RegistryResultItem(
    station: RegistryStation,
    alreadySaved: Boolean,
    onTap: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        leadingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                if (station.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = station.logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Radio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = if (station.country.isNotBlank()) {
            { Text(station.country, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        trailingContent = {
            IconButton(
                onClick = if (alreadySaved) onRemove else onAdd,
                shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
            ) {
                Icon(
                    imageVector = if (alreadySaved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (alreadySaved) "Remove from favorites" else "Save to favorites",
                    tint = if (alreadySaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    )
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
private fun HomeContent(
    stations: List<Station>,
    currentStation: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    allTags: List<String>,
    featuredStations: List<com.shapeshed.aerial.data.RegistryStation>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onPlay: (Station) -> Unit,
    onAddTapped: () -> Unit,
    onCategoryTap: (String) -> Unit,
    onFeaturedStationTap: (com.shapeshed.aerial.data.RegistryStation) -> Unit,
) {
    val tileColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val tileContentColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
    ) {
        item("tap-to-listen") {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Your favorites",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                )
                val totalTiles = stations.size + 1
                val rows = (0 until totalTiles).toList().chunked(3)
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    rows.forEach { rowIndices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowIndices.forEach { idx ->
                                if (idx < stations.size) {
                                    val station = stations[idx]
                                    StationTile(
                                        station = station,
                                        tileColor = tileColor,
                                        contentColor = tileContentColor,
                                        isActive = currentStation?.id == station.id,
                                        isPlaying = isPlaying && currentStation?.id == station.id,
                                        isBuffering = isBuffering && currentStation?.id == station.id,
                                        onClick = { onPlay(station) },
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    AddTile(onClick = onAddTapped, modifier = Modifier.weight(1f))
                                }
                            }
                            repeat(3 - rowIndices.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        if (stations.isEmpty() && featuredStations.isNotEmpty()) {
            item("get-started-header") {
                Text(
                    text = "Get started",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                )
            }
            item("get-started-row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(featuredStations) { station ->
                        FeaturedStationCard(
                            station = station,
                            onClick = { onFeaturedStationTap(station) },
                        )
                    }
                }
            }
        }

        item("just-listen-header") {
            Text(
                text = "Just listen",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            )
        }
        item("just-listen-pills") {
            CategoryGrid(
                tags = allTags.take(10),
                onCategoryTap = onCategoryTap,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun StationTile(
    station: Station,
    tileColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    isActive: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        val logoModel = when {
            station.logoPath.startsWith("http") -> station.logoPath
            station.logoPath.isNotEmpty() -> File(station.logoPath)
            else -> null
        }
        var logoFailed by remember(logoModel) { mutableStateOf(false) }

        Surface(
            onClick = onClick,
            color = tileColor,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (logoModel != null && !logoFailed) {
                    AsyncImage(
                        model = logoModel,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        onError = { logoFailed = true },
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)),
                    )
                }
                val indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                when {
                    isBuffering -> CircularWavyProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = indicatorColor,
                        trackColor = indicatorColor.copy(alpha = 0.3f),
                    )
                    isActive && isPlaying -> EqualizerBars(
                        color = indicatorColor,
                        modifier = Modifier.size(width = 36.dp, height = 28.dp),
                        barCount = 3,
                    )
                }
            }
        }
        Text(
            text = station.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}

@Composable
private fun AddTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Find a station",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = "Find one to listen to",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
        )
    }
}

@Composable
private fun CategoryGrid(
    tags: List<String>,
    onCategoryTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    val tagIcons = mapOf(
        "News" to Icons.AutoMirrored.Rounded.Article,
        "Sport" to Icons.Rounded.SportsSoccer,
        "Pop" to Icons.Rounded.Star,
        "Rock" to Icons.Rounded.ElectricBolt,
        "Jazz" to Icons.Rounded.Piano,
        "Classical" to Icons.Rounded.LibraryMusic,
        "Dance" to Icons.Rounded.Nightlife,
        "Soul" to Icons.Rounded.Spa,
        "Country" to Icons.Rounded.Landscape,
        "Electronic" to Icons.Rounded.GraphicEq,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        tags.chunked(2).forEach { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                pair.forEach { tag ->
                    Card(
                        onClick = { onCategoryTap(tag) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f).height(72.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = tagIcons[tag] ?: Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(tag, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SearchFilterRow(
    selectedCountries: Set<String>,
    selectedTags: Set<String>,
    onCountryClick: () -> Unit,
    onGenreClick: () -> Unit,
    onClearAll: () -> Unit,
    hasFilters: Boolean,
    modifier: Modifier = Modifier,
) {
    fun chipLabel(selected: Set<String>, fallback: String, displayName: (String) -> String = { it }): String = when (selected.size) {
        0 -> fallback
        1 -> displayName(selected.first())
        else -> "${displayName(selected.first())}+${selected.size - 1}"
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        FilterChip(
            selected = selectedCountries.isNotEmpty(),
            onClick = onCountryClick,
            label = { Text(chipLabel(selectedCountries, "Country", ::countryName)) },
            trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        FilterChip(
            selected = selectedTags.isNotEmpty(),
            onClick = onGenreClick,
            label = { Text(chipLabel(selectedTags, "Genre")) },
            trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        if (hasFilters) {
            TextButton(onClick = onClearAll) { Text("Clear all") }
        }
    }
}

private enum class FilterPickerType { Country, Genre }

@Composable
private fun FilterPickerScreen(
    title: String,
    searchLabel: String,
    query: String,
    onQueryChange: (String) -> Unit,
    items: List<String>,
    selectedItems: Set<String>,
    displayName: (String) -> String,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val normalizedQuery = query.trim()
    val filteredItems = remember(items, selectedItems, normalizedQuery) {
        items
            .filter { normalizedQuery.isBlank() || displayName(it).contains(normalizedQuery, ignoreCase = true) }
            .sortedWith(
                compareByDescending<String> { it in selectedItems }
                    .thenBy { displayName(it) },
            )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            IconButton(
                onClick = onBack,
                shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (selectedItems.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            label = { Text(searchLabel) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (filteredItems.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                HomeEmptyState(
                    text = "No matches",
                    supportingText = "Try a different search.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            )
            {
                items(filteredItems, key = { it }) { item ->
                    val checked = item in selectedItems
                    ListItem(
                        modifier = Modifier.clickable { onToggle(item) },
                        headlineContent = { Text(displayName(item)) },
                        trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedStationCard(
    station: com.shapeshed.aerial.data.RegistryStation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(96.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (station.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = station.logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = station.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = station.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun StationAvatar(
    station: Station,
    isActive: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val logoPath = station.logoPath
    val logoModel = when {
        logoPath.startsWith("http") -> logoPath
        logoPath.isNotEmpty() -> File(logoPath)
        else -> null
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
