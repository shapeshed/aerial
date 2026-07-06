package com.shapeshed.aerial.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.res.stringResource
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Localized country name from the stored ISO code via ICU, in the app's current locale.
// Cached per (code, language) so Locale.Builder isn't called on every row recomposition.
private val countryNameCache = mutableMapOf<String, String>()
private fun countryName(code: String, locale: java.util.Locale): String =
    countryNameCache.getOrPut("$code|${locale.language}") {
        java.util.Locale.Builder().setRegion(code).build().getDisplayCountry(locale).ifBlank { code }
    }

// The curated genre tags are a fixed set, so their display is localized while the English tag
// stays the matching key. Unknown tags fall back to their raw value.
@Composable
private fun rememberTagLabels(): Map<String, String> = mapOf(
    "News" to stringResource(R.string.tag_news),
    "Sport" to stringResource(R.string.tag_sport),
    "Pop" to stringResource(R.string.tag_pop),
    "Rock" to stringResource(R.string.tag_rock),
    "Jazz" to stringResource(R.string.tag_jazz),
    "Classical" to stringResource(R.string.tag_classical),
    "Dance" to stringResource(R.string.tag_dance),
    "Soul" to stringResource(R.string.tag_soul),
    "Country" to stringResource(R.string.tag_country),
    "Electronic" to stringResource(R.string.tag_electronic),
)

// Fixed tag→icon map — hoisted to avoid allocating a new Map on every CategoryGrid recomposition.
private val CATEGORY_ICONS = mapOf(
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

private enum class MoodArtwork {
    Coast,
    Mountains,
    Sunrise,
    Road,
    City,
    Runner,
}

private data class CuratedMood(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val detailDescriptionRes: Int = descriptionRes,
    val tags: List<String>,
    val icon: ImageVector,
    val artwork: MoodArtwork,
)

private val CURATED_MOODS = listOf(
    CuratedMood(
        id = "relax",
        titleRes = R.string.mood_relax,
        descriptionRes = R.string.mood_relax_desc,
        detailDescriptionRes = R.string.mood_relax_detail_desc,
        tags = listOf("Jazz", "Soul", "Classical"),
        icon = Icons.Rounded.Spa,
        artwork = MoodArtwork.Coast,
    ),
    CuratedMood(
        id = "focus",
        titleRes = R.string.mood_focus,
        descriptionRes = R.string.mood_focus_desc,
        tags = listOf("Classical", "Electronic", "Jazz"),
        icon = Icons.Rounded.GraphicEq,
        artwork = MoodArtwork.Mountains,
    ),
    CuratedMood(
        id = "morning",
        titleRes = R.string.mood_morning,
        descriptionRes = R.string.mood_morning_desc,
        tags = listOf("Pop", "Soul", "News"),
        icon = Icons.Rounded.Star,
        artwork = MoodArtwork.Sunrise,
    ),
    CuratedMood(
        id = "driving",
        titleRes = R.string.mood_driving,
        descriptionRes = R.string.mood_driving_desc,
        tags = listOf("Rock", "Pop", "Country"),
        icon = Icons.Rounded.Landscape,
        artwork = MoodArtwork.Road,
    ),
    CuratedMood(
        id = "late_night",
        titleRes = R.string.mood_late_night,
        descriptionRes = R.string.mood_late_night_desc,
        tags = listOf("Jazz", "Electronic", "Soul"),
        icon = Icons.Rounded.Nightlife,
        artwork = MoodArtwork.City,
    ),
    CuratedMood(
        id = "workout",
        titleRes = R.string.mood_workout,
        descriptionRes = R.string.mood_workout_desc,
        tags = listOf("Dance", "Electronic", "Rock"),
        icon = Icons.Rounded.ElectricBolt,
        artwork = MoodArtwork.Runner,
    ),
)

private data class RegistryStationKey(
    val provider: String,
    val providerId: String,
)

private fun RegistryStation.savedKey(): RegistryStationKey? =
    RegistryStationKey(provider, providerId).takeIf {
        it.provider.isNotBlank() && it.providerId.isNotBlank()
    }

private fun Station.savedKey(): RegistryStationKey? =
    RegistryStationKey(provider, providerId).takeIf {
        it.provider.isNotBlank() && it.providerId.isNotBlank()
    }

private fun RegistryStation.toPlaybackStation(): Station = Station(
    name = name,
    streamUrl = streamUrl,
    logoPath = logoUrl,
    provider = provider,
    providerId = providerId,
    tags = tags,
    description = description,
    country = country,
    countryCode = countryCode,
)

private fun Station.matchesStation(other: Station): Boolean =
    streamUrl == other.streamUrl ||
        (provider.isNotBlank() &&
            providerId.isNotBlank() &&
            provider == other.provider &&
            providerId == other.providerId)

enum class HomeViewMode {
    Cards,
    List,
}

private const val TAB_HOME = 0
private const val TAB_FAVORITES = 1

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onAddStation: () -> Unit,
    onEditStation: (Long) -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val currentTrackTitle by viewModel.currentTrackTitle.collectAsStateWithLifecycle()
    val currentTrackArtist by viewModel.currentTrackArtist.collectAsStateWithLifecycle()
    val currentTrackArtworkData by viewModel.currentTrackArtworkData.collectAsStateWithLifecycle()
    val currentTrackArtworkUrl by viewModel.currentTrackArtworkUrl.collectAsStateWithLifecycle()
    val currentBitrateKbps by viewModel.currentBitrateKbps.collectAsStateWithLifecycle()
    val nowPlayingInfo by viewModel.nowPlayingInfo.collectAsStateWithLifecycle()
    val nowPlayingDisplay by viewModel.nowPlayingDisplay.collectAsStateWithLifecycle()
    val sleepTimer by viewModel.sleepTimer.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val recentlyAddedStationId by viewModel.recentlyAddedStationId.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val showNowPlaying by viewModel.showNowPlaying.collectAsStateWithLifecycle()
    val registrySearchResults by viewModel.registrySearchResults.collectAsStateWithLifecycle()
    val favoriteSearchResults by viewModel.favoriteSearchResults.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val featuredStations by viewModel.featuredStations.collectAsStateWithLifecycle()
    val ukForYouStations by viewModel.ukForYouStations.collectAsStateWithLifecycle()
    val defaultStations by viewModel.defaultStations.collectAsStateWithLifecycle()
    val curatedMoodStations by viewModel.curatedMoodStations.collectAsStateWithLifecycle()
    val homeViewMode by viewModel.homeViewMode.collectAsStateWithLifecycle()
    val favoritesSort by viewModel.favoritesSort.collectAsStateWithLifecycle()
    val showStreamBitrate by viewModel.showStreamBitrate.collectAsStateWithLifecycle()
    val selectedCountries: Set<String> by viewModel.selectedCountries.collectAsStateWithLifecycle()
    val selectedTags: Set<String> by viewModel.selectedTags.collectAsStateWithLifecycle()
    val availableCountries: List<String> by viewModel.availableCountries.collectAsStateWithLifecycle()
    val appLocale = LocalConfiguration.current.locales[0]
    val tagLabels = rememberTagLabels()

    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberContainedSearchBarState()
    val isSearchExpanded by remember { derivedStateOf { searchBarState.currentValue == SearchBarValue.Expanded } }
    val searchQueryText by remember { derivedStateOf { textFieldState.text.toString() } }
    var showCountrySheet by remember { mutableStateOf(false) }
    var showGenreSheet by remember { mutableStateOf(false) }
    var contextStation by remember { mutableStateOf<Station?>(null) }
    var stationToDelete by remember { mutableStateOf<Station?>(null) }
    var selectedMoodId by rememberSaveable { mutableStateOf<String?>(null) }
    val countrySheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val genreSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    var countryFilterQuery by remember { mutableStateOf("") }
    var genreFilterQuery by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    val savedStreamUrls = remember(stations) { stations.map { it.streamUrl }.toSet() }
    val savedRegistryKeys = remember(stations) { stations.mapNotNull { it.savedKey() }.toSet() }

    val selectedTab by viewModel.selectedHomeTab.collectAsStateWithLifecycle()
    // Hoisted so each tab keeps its scroll position across tab switches.
    val homeListState = rememberLazyListState()
    val favoritesListState = rememberLazyListState()

    var miniPlayerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val stationContentBottomPadding = if (currentStation != null) with(density) { miniPlayerHeightPx.toDp() } else 0.dp
    val selectedMood = selectedMoodId?.let { id -> CURATED_MOODS.firstOrNull { it.id == id } }
    val selectedMoodStations = selectedMoodId?.let { curatedMoodStations[it] }.orEmpty()
    val activeNowPlayingInfo = nowPlayingInfo?.takeIf { it.stationId == currentStation?.id }
    val moodSwipeStations = remember(selectedMoodStations) {
        selectedMoodStations.map { it.toPlaybackStation() }
    }

    BackHandler(enabled = showNowPlaying) { viewModel.setShowNowPlaying(false) }
    BackHandler(enabled = selectedMood != null && !showNowPlaying) { selectedMoodId = null }
    BackHandler(enabled = isSearchExpanded && !showCountrySheet && !showGenreSheet) {
        textFieldState.edit { replace(0, length, "") }
        scope.launch { searchBarState.animateToCollapsed() }
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

    fun openCountrySearch(countryCode: String) {
        textFieldState.edit { replace(0, length, "") }
        viewModel.searchRegistry("")
        viewModel.setCountryFilter(countryCode)
        scope.launch { searchBarState.animateToExpanded() }
    }

    val searchInputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { viewModel.saveRecentSearch(it) },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = {
                if (isSearchExpanded) {
                    IconButton(
                        onClick = {
                            textFieldState.edit { replace(0, length, "") }
                            scope.launch { searchBarState.animateToCollapsed() }
                        },
                        shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                } else {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                when {
                    isSearchExpanded && searchQueryText.isNotEmpty() -> {
                        IconButton(
                            onClick = { textFieldState.edit { replace(0, length, "") } },
                            shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                    !isSearchExpanded -> {
                        IconButton(
                            onClick = onSettings,
                            shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true },
    ) {
        Scaffold(
            modifier = Modifier.semantics { traversalIndex = 0f },
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                // surfaceContainerLow matches the reference bar (Google Drive) rather than
                // the component's default surfaceContainer.
                ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    ShortNavigationBarItem(
                        selected = selectedTab == TAB_HOME,
                        onClick = {
                            selectedMoodId = null
                            viewModel.setSelectedHomeTab(TAB_HOME)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == TAB_HOME) Icons.Rounded.Home else Icons.Outlined.Home,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.tab_home)) },
                    )
                    ShortNavigationBarItem(
                        selected = selectedTab == TAB_FAVORITES,
                        onClick = {
                            selectedMoodId = null
                            viewModel.setSelectedHomeTab(TAB_FAVORITES)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == TAB_FAVORITES) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.tab_favorites)) },
                    )
                }
            },
        ) { padding ->
            // The wrapper Box insets the tab content and lets the mini player float above
            // the navigation bar; the search/now-playing overlays outside it still cover it.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
            ) {
            Column(Modifier.fillMaxSize()) {
                if (selectedMood == null) {
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
                }

                if (!isOnline) {
                    NoNetworkState()
                } else if (selectedMood != null) {
                    MoodDetailScreen(
                        mood = selectedMood,
                        stations = selectedMoodStations,
                        currentStation = currentStation,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        savedStreamUrls = savedStreamUrls,
                        savedRegistryKeys = savedRegistryKeys,
                        bottomPadding = stationContentBottomPadding,
                        onBack = { selectedMoodId = null },
                        onPlay = { selectedMoodStations.firstOrNull()?.let(viewModel::playFromRegistry) },
                        onSave = { selectedMoodStations.forEach(viewModel::addFromRegistry) },
                        onTogglePlayback = { viewModel.togglePlayback() },
                        onAddStation = { viewModel.addFromRegistry(it) },
                        onRemoveStation = { viewModel.removeFromRegistry(it) },
                        onPlayStation = { viewModel.playFromRegistry(it) },
                    )
                } else if (selectedTab == TAB_HOME) {
                    val forYouCountryCode = appLocale.country.takeIf { it.isNotBlank() } ?: "GB"
                    HomeTabContent(
                        allTags = allTags,
                        featuredStations = featuredStations,
                        forYouStations = ukForYouStations.takeIf { forYouCountryCode.equals("GB", ignoreCase = true) }
                            ?: featuredStations,
                        forYouCountry = countryName(forYouCountryCode, appLocale),
                        listState = homeListState,
                        bottomPadding = stationContentBottomPadding,
                        onCategoryTap = { viewModel.playRandomFromCategory(it) },
                        onMoodTap = { selectedMoodId = it.id },
                        onFeaturedStationTap = { viewModel.playFromRegistry(it) },
                        onForYouViewAll = { openCountrySearch(forYouCountryCode) },
                    )
                } else {
                    FavoritesTabContent(
                        stations = stations,
                        currentStation = currentStation,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        homeViewMode = homeViewMode,
                        favoritesSort = favoritesSort,
                        listState = favoritesListState,
                        bottomPadding = stationContentBottomPadding,
                        onPlay = { viewModel.play(it) },
                        onHomeViewModeChange = { viewModel.setHomeViewMode(it) },
                        onSortSelected = { viewModel.setFavoritesSort(it) },
                        onAddTapped = ::openRegistrySearch,
                        onStationLongPress = { contextStation = it },
                    )
                }
            }

        val miniPlayerState = remember { MutableTransitionState(currentStation != null) }
        miniPlayerState.targetState = currentStation != null
        AnimatedVisibility(
            visibleState = miniPlayerState,
            enter = slideInVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .semantics { traversalIndex = 1f }
                .onSizeChanged { miniPlayerHeightPx = it.height }
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        ) {
            currentStation?.let { station ->
                val miniPlayerTitle = nowPlayingDisplay.title.ifBlank { station.name }
                val miniPlayerSubtitle = playbackError
                    ?: if (isBuffering) stringResource(R.string.buffering) else nowPlayingDisplay.subtitle
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
                            val miniArtworkModel = if (!isPlaying) null else with(activeNowPlayingInfo) {
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
                                                contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
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
        }

        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize(),
        ) {
            val station = currentStation
            if (station != null) {
                // Swipe order frozen for the lifetime of the pane: under the Last/Most played
                // sorts, playing a station immediately re-sorts the live list, which would make
                // consecutive swipes ping-pong between the same two stations.
                val favoriteSwipeStations = remember { viewModel.stations.value }
                val useMoodSwipeStations = selectedMood != null &&
                    moodSwipeStations.size > 1 &&
                    moodSwipeStations.any { moodStation -> station.matchesStation(moodStation) }
                val swipeStations = if (useMoodSwipeStations) moodSwipeStations else favoriteSwipeStations
                NowPlayingScreen(
                    station = station,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    nowPlayingInfo = activeNowPlayingInfo,
                    currentTrackTitle = currentTrackTitle,
                    currentTrackArtist = currentTrackArtist,
                    currentTrackArtworkData = currentTrackArtworkData,
                    currentTrackArtworkUrl = currentTrackArtworkUrl,
                    currentBitrateKbps = currentBitrateKbps,
                    showStreamBitrate = showStreamBitrate,
                    sleepTimer = sleepTimer,
                    swipeStations = swipeStations,
                    onPlayStation = { viewModel.play(it) },
                    onToggle = { viewModel.togglePlayback() },
                    onToggleFavorite = { viewModel.toggleFavorite(station) },
                    onSetSleepTimer = { viewModel.setSleepTimer(it) },
                    onCancelSleepTimer = { viewModel.cancelSleepTimer() },
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
            } else {
                val hasFilters = selectedCountries.isNotEmpty() || selectedTags.isNotEmpty()
                SearchFilterRow(
                    selectedCountries = selectedCountries,
                    selectedTags = selectedTags,
                    onCountryClick = { showCountrySheet = true },
                    onGenreClick = { showGenreSheet = true },
                    onClearAll = { viewModel.clearAllFilters() },
                    hasFilters = hasFilters,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (searchQueryText.isBlank() && !hasFilters) {
                    if (recentSearches.isNotEmpty()) {
                        RecentSearches(
                            searches = recentSearches,
                            onSelect = { query ->
                                textFieldState.edit { replace(0, length, query) }
                                viewModel.searchRegistry(query)
                            },
                            onRemove = { viewModel.removeRecentSearch(it) },
                        )
                    } else {
                        // Cold start with no history: show an A-Z browse list so the view
                        // isn't empty before the user has typed anything.
                        DefaultSearchResults(
                            stations = defaultStations,
                            savedStreamUrls = savedStreamUrls,
                            savedRegistryKeys = savedRegistryKeys,
                            onPlay = { station ->
                                viewModel.playFromRegistry(station)
                                textFieldState.edit { replace(0, length, "") }
                                scope.launch { searchBarState.animateToCollapsed() }
                            },
                            onAdd = { viewModel.addFromRegistry(it) },
                            onRemove = { viewModel.removeFromRegistry(it) },
                        )
                    }
                } else {
                    RegistrySearchResults(
                        favoriteResults = favoriteSearchResults,
                        results = registrySearchResults,
                        savedStreamUrls = savedStreamUrls,
                        savedRegistryKeys = savedRegistryKeys,
                        onFavoritePlay = { station ->
                            viewModel.saveRecentSearch(searchQueryText)
                            viewModel.play(station)
                            textFieldState.edit { replace(0, length, "") }
                            scope.launch { searchBarState.animateToCollapsed() }
                        },
                        onPlay = { station ->
                            viewModel.saveRecentSearch(searchQueryText)
                            viewModel.playFromRegistry(station)
                            textFieldState.edit { replace(0, length, "") }
                            scope.launch { searchBarState.animateToCollapsed() }
                        },
                        onAdd = { viewModel.addFromRegistry(it) },
                        onRemove = { viewModel.removeFromRegistry(it) },
                        bottomPadding = 0.dp,
                        onAddManually = {
                            scope.launch { searchBarState.animateToCollapsed() }
                            onAddStation()
                        },
                    )
                }
            }
        }

        if (showCountrySheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showCountrySheet = false
                    countryFilterQuery = ""
                },
                sheetState = countrySheetState,
            ) {
                FilterPickerSheetContent(
                    title = stringResource(R.string.filter_country),
                    searchLabel = stringResource(R.string.search_countries),
                    query = countryFilterQuery,
                    onQueryChange = { countryFilterQuery = it },
                    items = availableCountries,
                    selectedItems = selectedCountries,
                    displayName = { countryName(it, appLocale) },
                    onToggle = { viewModel.toggleCountryFilter(it) },
                    onClear = { viewModel.clearCountryFilter() },
                )
            }
        }

        if (showGenreSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showGenreSheet = false
                    genreFilterQuery = ""
                },
                sheetState = genreSheetState,
            ) {
                FilterPickerSheetContent(
                    title = stringResource(R.string.filter_genre),
                    searchLabel = stringResource(R.string.search_genres),
                    query = genreFilterQuery,
                    onQueryChange = { genreFilterQuery = it },
                    items = allTags,
                    selectedItems = selectedTags,
                    displayName = { tagLabels[it] ?: it },
                    onToggle = { viewModel.toggleTagFilter(it) },
                    onClear = { viewModel.clearTagFilter() },
                )
            }
        }

        contextStation?.let { station ->
            ModalBottomSheet(
                onDismissRequest = { contextStation = null },
                sheetState = rememberBottomSheetState(
                    initialValue = SheetValue.Hidden,
                    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                ),
                dragHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                StationContextSheet(
                    station = station,
                    onEdit = {
                        contextStation = null
                        onEditStation(station.id)
                    },
                    onDelete = {
                        stationToDelete = station
                        contextStation = null
                    },
                )
            }
        }

        stationToDelete?.let { station ->
            AlertDialog(
                onDismissRequest = { stationToDelete = null },
                title = { Text(stringResource(R.string.remove_station_title)) },
                text = { Text(stringResource(R.string.remove_station_message, station.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteStation(station)
                        stationToDelete = null
                    }) {
                        Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { stationToDelete = null }) { Text(stringResource(R.string.action_cancel)) }
                },
            )
        }
    }

}


// Pre-search browse list (A-Z subsection of the registry) shown on a cold start when there
// are no recent searches, so the search view isn't empty before the user types.
@Composable
private fun DefaultSearchResults(
    stations: List<RegistryStation>,
    savedStreamUrls: Set<String>,
    savedRegistryKeys: Set<RegistryStationKey>,
    onPlay: (RegistryStation) -> Unit,
    onAdd: (RegistryStation) -> Unit,
    onRemove: (RegistryStation) -> Unit,
) {
    if (stations.isEmpty()) return
    LazyColumn {
        items(
            items = stations,
            key = { it.id },
            contentType = { "registry-result" },
        ) { station ->
            RegistryResultItem(
                station = station,
                alreadySaved = station.streamUrl in savedStreamUrls || station.savedKey() in savedRegistryKeys,
                onTap = { onPlay(station) },
                onAdd = { onAdd(station) },
                onRemove = { onRemove(station) },
            )
        }
    }
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
private fun RegistrySearchResults(
    favoriteResults: List<Station>,
    results: List<RegistryStation>,
    savedStreamUrls: Set<String>,
    savedRegistryKeys: Set<RegistryStationKey>,
    onFavoritePlay: (Station) -> Unit,
    onPlay: (RegistryStation) -> Unit,
    onAdd: (RegistryStation) -> Unit,
    onRemove: (RegistryStation) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onAddManually: (() -> Unit)? = null,
) {
    if (favoriteResults.isEmpty() && results.isEmpty()) {
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(32.dp),
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
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomPadding),
        ) {
            if (favoriteResults.isNotEmpty()) {
                item("favorite-results-header") {
                    Text(
                        text = stringResource(R.string.favorites_header),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                items(
                    items = favoriteResults,
                    key = { "favorite-${it.id}" },
                    contentType = { "favorite-result" },
                ) { station ->
                    FavoriteResultItem(
                        station = station,
                        onTap = { onFavoritePlay(station) },
                    )
                }
                if (results.isNotEmpty()) {
                    item("registry-results-header") {
                        Text(
                            text = stringResource(R.string.search_stations_header),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                }
            }
            items(
                items = results,
                key = { it.id },
                contentType = { "registry-result" },
            ) { station ->
                val alreadySaved = station.streamUrl in savedStreamUrls || station.savedKey() in savedRegistryKeys
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
private fun FavoriteResultItem(
    station: Station,
    onTap: () -> Unit,
) {
    val countryLabel = station.countryCode.takeIf { it.isNotBlank() }
        ?.let { countryName(it, LocalConfiguration.current.locales[0]) }
        ?: station.country
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        leadingContent = {
            StationAvatar(station = station, isActive = false, size = 50.dp)
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
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
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
    onTap: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    // Localize the country from its ISO code (falling back to the registry's own name).
    val countryLabel = station.countryCode.takeIf { it.isNotBlank() }
        ?.let { countryName(it, LocalConfiguration.current.locales[0]) }
        ?: station.country
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        leadingContent = {
            StationLogoCircle(
                logoModel = station.logoUrl.takeIf { it.isNotBlank() },
                size = 50.dp,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
        supportingContent = if (countryLabel.isNotBlank()) {
            { Text(countryLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        trailingContent = {
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
private fun HomeTabContent(
    allTags: List<String>,
    featuredStations: List<com.shapeshed.aerial.data.RegistryStation>,
    forYouStations: List<com.shapeshed.aerial.data.RegistryStation>,
    forYouCountry: String,
    listState: LazyListState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onCategoryTap: (String) -> Unit,
    onMoodTap: (CuratedMood) -> Unit,
    onFeaturedStationTap: (com.shapeshed.aerial.data.RegistryStation) -> Unit,
    onForYouViewAll: () -> Unit,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
    ) {
        if (forYouStations.isNotEmpty()) {
            item("for-you-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.for_you_country, forYouCountry),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onForYouViewAll) {
                        Text(text = stringResource(R.string.view_all))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            item("for-you-row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(
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

        if (featuredStations.isNotEmpty()) {
            item("get-started-header") {
                Text(
                    text = stringResource(R.string.get_started),
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

        item("moods-header") {
            Text(
                text = stringResource(R.string.listen_by_mood),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
            )
        }
        item("moods-grid") {
            MoodGrid(
                moods = CURATED_MOODS,
                onMoodTap = onMoodTap,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item("just-listen-header") {
            Text(
                text = stringResource(R.string.just_listen),
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
private fun MoodGrid(
    moods: List<CuratedMood>,
    onMoodTap: (CuratedMood) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier,
    ) {
        moods.chunked(2).forEach { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                pair.forEach { mood ->
                    MoodCard(
                        mood = mood,
                        onClick = { onMoodTap(mood) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MoodCard(
    mood: CuratedMood,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.height(132.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MoodArtwork(
                artwork = mood.artwork,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.08f),
                            1f to MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
                        ),
                    ),
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = mood.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(mood.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(mood.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodDetailScreen(
    mood: CuratedMood,
    stations: List<RegistryStation>,
    currentStation: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    savedStreamUrls: Set<String>,
    savedRegistryKeys: Set<RegistryStationKey>,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    onTogglePlayback: () -> Unit,
    onAddStation: (RegistryStation) -> Unit,
    onRemoveStation: (RegistryStation) -> Unit,
    onPlayStation: (RegistryStation) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item("mood-hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
            ) {
                MoodArtwork(
                    artwork = mood.artwork,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.08f),
                                1f to MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                            ),
                        ),
                )
                IconButton(
                    onClick = onBack,
                    shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = 12.dp, top = 8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                ) {
                    Text(
                        text = stringResource(mood.titleRes),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(mood.detailDescriptionRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(0.72f),
                    )
                }
            }
        }
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
        items(
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
                onTogglePlayback = onTogglePlayback,
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
    onTogglePlayback: () -> Unit,
    isSaved: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pauseLabel = stringResource(R.string.pause)
    ListItem(
        modifier = modifier.fillMaxWidth().clickable(onClick = onPlay),
        leadingContent = {
            // Circle rather than a rounded square, matching the search and favourites rows.
            StationLogoCircle(
                logoModel = station.logoUrl.takeIf { it.isNotBlank() },
                size = 56.dp,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
        supportingContent = {
            if (station.country.isNotBlank()) {
                Text(
                    text = station.country,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = if (isPlaying) onTogglePlayback else onPlay,
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
                    onClick = onToggleFavorite,
                    shapes = IconButtonShapes(IconButtonDefaults.smallRoundShape, IconButtonDefaults.smallPressedShape),
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(if (isSaved) R.string.remove_from_favorites else R.string.save_to_favorites),
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) {
        Text(
            text = station.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MoodArtwork(
    artwork: MoodArtwork,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier = modifier) {
        val skyTop = when (artwork) {
            MoodArtwork.Coast -> lerp(scheme.tertiaryContainer, scheme.primaryContainer, 0.18f)
            MoodArtwork.Mountains -> lerp(scheme.secondaryContainer, scheme.surfaceVariant, 0.35f)
            MoodArtwork.Sunrise -> lerp(scheme.tertiaryContainer, Color(0xFFFFC879), 0.32f)
            MoodArtwork.Road -> lerp(scheme.primaryContainer, scheme.secondaryContainer, 0.36f)
            MoodArtwork.City -> lerp(scheme.primary, scheme.surface, 0.64f)
            MoodArtwork.Runner -> lerp(scheme.secondaryContainer, scheme.tertiaryContainer, 0.28f)
        }
        val skyBottom = when (artwork) {
            MoodArtwork.City -> lerp(scheme.surface, scheme.primaryContainer, 0.4f)
            else -> lerp(scheme.surface, skyTop, 0.58f)
        }
        drawRect(
            brush = Brush.verticalGradient(
                listOf(skyTop, skyBottom),
            ),
        )

        val sunColor = scheme.tertiary.copy(alpha = 0.28f)
        drawCircle(
            color = sunColor,
            radius = size.minDimension * 0.16f,
            center = Offset(size.width * 0.72f, size.height * 0.35f),
        )

        when (artwork) {
            MoodArtwork.Coast -> drawCoastMood(scheme)
            MoodArtwork.Mountains -> drawMountainMood(scheme)
            MoodArtwork.Sunrise -> drawSunriseMood(scheme)
            MoodArtwork.Road -> drawRoadMood(scheme)
            MoodArtwork.City -> drawCityMood(scheme)
            MoodArtwork.Runner -> drawRunnerMood(scheme)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoastMood(
    scheme: androidx.compose.material3.ColorScheme,
) {
    drawLayeredHills(
        back = scheme.primary.copy(alpha = 0.18f),
        front = scheme.primary.copy(alpha = 0.34f),
    )
    drawRect(
        color = scheme.primary.copy(alpha = 0.22f),
        topLeft = Offset(0f, size.height * 0.62f),
        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.38f),
    )
    repeat(4) { index ->
        val y = size.height * (0.68f + index * 0.07f)
        drawLine(
            color = scheme.onSurface.copy(alpha = 0.12f),
            start = Offset(size.width * 0.18f, y),
            end = Offset(size.width * 0.86f, y + size.height * 0.02f),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMountainMood(
    scheme: androidx.compose.material3.ColorScheme,
) {
    repeat(3) { index ->
        val top = size.height * (0.28f + index * 0.11f)
        val alpha = 0.16f + index * 0.1f
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(size.width * 0.22f, top + size.height * 0.18f)
            lineTo(size.width * 0.48f, top)
            lineTo(size.width * 0.75f, top + size.height * 0.2f)
            lineTo(size.width, top + size.height * 0.08f)
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path, scheme.primary.copy(alpha = alpha))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunriseMood(
    scheme: androidx.compose.material3.ColorScheme,
) {
    drawLayeredHills(
        back = scheme.tertiary.copy(alpha = 0.18f),
        front = scheme.primary.copy(alpha = 0.24f),
    )
    drawCircle(
        color = scheme.tertiary.copy(alpha = 0.26f),
        radius = size.minDimension * 0.22f,
        center = Offset(size.width * 0.78f, size.height * 0.66f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoadMood(
    scheme: androidx.compose.material3.ColorScheme,
) {
    drawLayeredHills(
        back = scheme.secondary.copy(alpha = 0.18f),
        front = scheme.primary.copy(alpha = 0.28f),
    )
    val road = Path().apply {
        moveTo(size.width * 0.46f, size.height)
        lineTo(size.width * 0.58f, size.height * 0.55f)
        lineTo(size.width * 0.68f, size.height * 0.55f)
        lineTo(size.width * 0.86f, size.height)
        close()
    }
    drawPath(road, scheme.onSurface.copy(alpha = 0.16f))
    drawLine(
        color = scheme.surface.copy(alpha = 0.72f),
        start = Offset(size.width * 0.65f, size.height),
        end = Offset(size.width * 0.62f, size.height * 0.6f),
        strokeWidth = 2.dp.toPx(),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCityMood(
    scheme: androidx.compose.material3.ColorScheme,
) {
    drawRect(
        color = scheme.primary.copy(alpha = 0.22f),
        topLeft = Offset(0f, size.height * 0.52f),
        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.48f),
    )
    val widths = listOf(0.12f, 0.18f, 0.1f, 0.16f, 0.14f, 0.2f)
    var x = size.width * 0.06f
    widths.forEachIndexed { index, widthFraction ->
        val buildingWidth = size.width * widthFraction
        val top = size.height * (0.42f + (index % 3) * 0.08f)
        drawRect(
            color = scheme.onSurface.copy(alpha = 0.22f),
            topLeft = Offset(x, top),
            size = androidx.compose.ui.geometry.Size(buildingWidth, size.height - top),
        )
        x += buildingWidth + size.width * 0.035f
    }
    drawCircle(
        color = scheme.tertiary.copy(alpha = 0.34f),
        radius = size.minDimension * 0.08f,
        center = Offset(size.width * 0.76f, size.height * 0.24f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRunnerMood(
    scheme: androidx.compose.material3.ColorScheme,
) {
    drawLayeredHills(
        back = scheme.secondary.copy(alpha = 0.18f),
        front = scheme.tertiary.copy(alpha = 0.2f),
    )
    val bodyColor = scheme.onSurface.copy(alpha = 0.28f)
    val center = Offset(size.width * 0.74f, size.height * 0.48f)
    drawCircle(bodyColor, size.minDimension * 0.05f, center.copy(y = center.y - size.height * 0.12f))
    drawLine(bodyColor, center.copy(y = center.y - size.height * 0.06f), center.copy(x = center.x - size.width * 0.04f, y = center.y + size.height * 0.1f), 4.dp.toPx())
    drawLine(bodyColor, center, center.copy(x = center.x - size.width * 0.13f, y = center.y + size.height * 0.02f), 4.dp.toPx())
    drawLine(bodyColor, center, center.copy(x = center.x + size.width * 0.1f, y = center.y + size.height * 0.06f), 4.dp.toPx())
    drawLine(bodyColor, center.copy(x = center.x - size.width * 0.04f, y = center.y + size.height * 0.1f), center.copy(x = center.x - size.width * 0.15f, y = center.y + size.height * 0.24f), 4.dp.toPx())
    drawLine(bodyColor, center.copy(x = center.x - size.width * 0.04f, y = center.y + size.height * 0.1f), center.copy(x = center.x + size.width * 0.1f, y = center.y + size.height * 0.24f), 4.dp.toPx())
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLayeredHills(
    back: Color,
    front: Color,
) {
    val backPath = Path().apply {
        moveTo(0f, size.height * 0.66f)
        cubicTo(size.width * 0.2f, size.height * 0.48f, size.width * 0.38f, size.height * 0.72f, size.width * 0.56f, size.height * 0.55f)
        cubicTo(size.width * 0.75f, size.height * 0.38f, size.width * 0.88f, size.height * 0.58f, size.width, size.height * 0.45f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(backPath, back)
    val frontPath = Path().apply {
        moveTo(0f, size.height * 0.78f)
        cubicTo(size.width * 0.18f, size.height * 0.62f, size.width * 0.32f, size.height * 0.84f, size.width * 0.5f, size.height * 0.68f)
        cubicTo(size.width * 0.7f, size.height * 0.5f, size.width * 0.83f, size.height * 0.78f, size.width, size.height * 0.62f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(frontPath, front)
}

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
private fun FavoritesTabContent(
    stations: List<Station>,
    currentStation: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    homeViewMode: HomeViewMode,
    favoritesSort: FavoritesSort,
    listState: LazyListState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onPlay: (Station) -> Unit,
    onHomeViewModeChange: (HomeViewMode) -> Unit,
    onSortSelected: (FavoritesSort) -> Unit,
    onAddTapped: () -> Unit,
    onStationLongPress: (Station) -> Unit,
) {
    val tileColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val activeTileColor = MaterialTheme.colorScheme.primaryContainer
    val tileContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val favoriteRows = remember(stations) { (0 until stations.size + 1).toList().chunked(3) }
    val showFavoriteActivityIndicators by remember {
        derivedStateOf { !listState.isScrollInProgress }
    }
    var showSortSheet by remember { mutableStateOf(false) }

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

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
    ) {
        item("favorites-header") {
            // No headline — the Favourites tab already names the screen (Material tabs
            // convention); just the sort selector and the view-mode toggle.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
                TextButton(
                    onClick = { showSortSheet = true },
                    colors = ButtonDefaults.textButtonColors(
                        // Same colour as the checked pill of the view-mode switcher beside
                        // it, so the two header controls read as one family.
                        contentColor = ToggleButtonDefaults.tonalToggleButtonColors().checkedContainerColor,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(favoritesSortLabel(favoritesSort))
                }
                Spacer(modifier = Modifier.weight(1f))
                HomeViewModeToggle(
                    selected = homeViewMode,
                    onSelected = onHomeViewModeChange,
                )
            }
        }
        if (homeViewMode == HomeViewMode.Cards) {
            items(
                items = favoriteRows,
                key = { rowIndices -> "favorite-card-row-${rowIndices.first()}" },
                contentType = { "favorite-card-row" },
            ) { rowIndices ->
                FavoriteStationCardRow(
                    rowIndices = rowIndices,
                    isLastRow = rowIndices == favoriteRows.last(),
                    stations = stations,
                    currentStation = currentStation,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    showActivityIndicator = showFavoriteActivityIndicators,
                    tileColor = tileColor,
                    activeTileColor = activeTileColor,
                    tileContentColor = tileContentColor,
                    onPlay = onPlay,
                    onAddTapped = onAddTapped,
                    onStationLongPress = onStationLongPress,
                )
            }
        } else {
            items(
                items = stations,
                key = { station -> "favorite-list-${station.id}" },
                contentType = { "favorite-list-row" },
            ) { station ->
                val isActive = currentStation?.id == station.id
                StationListRow(
                    station = station,
                    isActive = isActive,
                    isPlaying = isPlaying && isActive,
                    isBuffering = isBuffering && isActive,
                    // Stable false for inactive rows — see the card-mode note above.
                    showActivityIndicator = showFavoriteActivityIndicators && isActive,
                    onClick = { onPlay(station) },
                    onLongClick = { onStationLongPress(station) },
                )
            }
            item("favorite-list-add", contentType = "favorite-list-add") {
                AddStationListRow(
                    onClick = onAddTapped,
                )
            }
        }
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
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
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
                    colors = ToggleButtonDefaults.tonalToggleButtonColors(),
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
                    colors = ToggleButtonDefaults.tonalToggleButtonColors(),
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
private fun FavoriteStationCardRow(
    rowIndices: List<Int>,
    isLastRow: Boolean,
    stations: List<Station>,
    currentStation: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    showActivityIndicator: Boolean,
    tileColor: androidx.compose.ui.graphics.Color,
    activeTileColor: androidx.compose.ui.graphics.Color,
    tileContentColor: androidx.compose.ui.graphics.Color,
    onPlay: (Station) -> Unit,
    onAddTapped: () -> Unit,
    onStationLongPress: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = if (isLastRow) 8.dp else 12.dp,
        ),
    ) {
        rowIndices.forEach { idx ->
            if (idx < stations.size) {
                val station = stations[idx]
                val isActive = currentStation?.id == station.id
                StationTile(
                    station = station,
                    tileColor = if (isActive) activeTileColor else tileColor,
                    contentColor = tileContentColor,
                    isActive = isActive,
                    isPlaying = isPlaying && isActive,
                    isBuffering = isBuffering && isActive,
                    // Only the active tile can show an indicator, so inactive tiles get a
                    // stable false and skip the recomposition when scrolling starts/stops.
                    showActivityIndicator = showActivityIndicator && isActive,
                    onClick = { onPlay(station) },
                    onLongClick = { onStationLongPress(station) },
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

@Composable
private fun AddStationListRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        leadingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        supportingContent = { Text(stringResource(R.string.find_one_to_listen)) },
    ) {
        Text(stringResource(R.string.find_a_station))
    }
}

@Composable
private fun StationListRow(
    station: Station,
    isActive: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    showActivityIndicator: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val appLocale = LocalConfiguration.current.locales[0]
    val countryLabel = station.countryCode.takeIf { it.isNotBlank() }
        ?.let { countryName(it, appLocale) }
        ?: station.country
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            when {
                !showActivityIndicator -> Unit
                isBuffering -> CircularWavyProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
                isActive && isPlaying -> EqualizerBars(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(width = 30.dp, height = 24.dp),
                    barCount = 3,
                )
            }
        },
    ) {
        Text(
            text = station.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    showActivityIndicator: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
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
            color = tileColor,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                ),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
                if (logoModel != null && !logoFailed) {
                    AsyncImage(
                        model = logoModel,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        onError = { logoFailed = true },
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                    )
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)),
                        )
                    }
                }
                val indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                when {
                    !showActivityIndicator -> Unit
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
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    contentDescription = stringResource(R.string.find_a_station),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.find_one_to_listen),
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
    val tagLabels = rememberTagLabels()
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
                                imageVector = CATEGORY_ICONS[tag] ?: Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(tagLabels[tag] ?: tag, style = MaterialTheme.typography.titleMedium)
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
    val appLocale = LocalConfiguration.current.locales[0]
    val tagLabels = rememberTagLabels()
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
            label = { Text(chipLabel(selectedCountries, stringResource(R.string.filter_country)) { countryName(it, appLocale) }) },
            leadingIcon = if (selectedCountries.isNotEmpty()) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null,
            trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        FilterChip(
            selected = selectedTags.isNotEmpty(),
            onClick = onGenreClick,
            label = { Text(chipLabel(selectedTags, stringResource(R.string.filter_genre)) { tagLabels[it] ?: it }) },
            leadingIcon = if (selectedTags.isNotEmpty()) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
            } else null,
            trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        if (hasFilters) {
            TextButton(onClick = onClearAll) { Text(stringResource(R.string.clear_all)) }
        }
    }
}

@Composable
private fun StationContextSheet(
    station: Station,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Text(
            text = station.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        HorizontalDivider()
        ListItem(
            leadingContent = {
                Icon(Icons.Rounded.Edit, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onEdit),
        ) {
            Text(stringResource(R.string.action_edit))
        }
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            modifier = Modifier.clickable(onClick = onDelete),
        ) {
            Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FilterPickerSheetContent(
    title: String,
    searchLabel: String,
    query: String,
    onQueryChange: (String) -> Unit,
    items: List<String>,
    selectedItems: Set<String>,
    displayName: (String) -> String,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
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
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, bottom = 4.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (selectedItems.isNotEmpty()) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.action_clear)) }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(searchLabel) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
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
                    text = stringResource(R.string.no_matches),
                    supportingText = stringResource(R.string.no_matches_desc),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(filteredItems, key = { it }) { item ->
                    val checked = item in selectedItems
                    ListItem(
                        modifier = Modifier.clickable { onToggle(item) },
                        trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                    ) {
                        Text(displayName(item))
                    }
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
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (station.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = station.logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
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
private fun ForYouStationCard(
    station: com.shapeshed.aerial.data.RegistryStation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        // Tonal, border-less container on the expressive extra-large radius, matching the
        // app's other tiles.
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.width(140.dp).height(132.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp).fillMaxSize(),
        ) {
            StationLogoCircle(
                logoModel = station.logoUrl.takeIf { it.isNotBlank() },
                size = 60.dp,
            ) {
                Text(
                    text = station.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val genre = station.tags.split(" ").firstOrNull { it.isNotBlank() } ?: station.country
                if (genre.isNotBlank()) {
                    Text(
                        text = genre.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// Circular station logo on a white plate. The plate shows through transparent regions of
// third-party artwork so every logo sits on a consistent background, and it is never a
// visible ring because the artwork fills the circle. Stations without a usable logo keep a
// tonal circle behind the fallback content instead of the white plate.
@Composable
fun StationLogoCircle(
    logoModel: Any?,
    size: Dp,
    modifier: Modifier = Modifier,
    fallbackBackground: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    fallback: @Composable () -> Unit,
) {
    var logoFailed by remember(logoModel) { mutableStateOf(false) }
    val showLogo = logoModel != null && !logoFailed
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (showLogo) androidx.compose.ui.graphics.Color.White else fallbackBackground),
    ) {
        if (showLogo) {
            AsyncImage(
                model = logoModel,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { logoFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            fallback()
        }
    }
}

@Composable
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
    val imageRequest = logoModel?.let {
        remember(context, it) { ImageRequest.Builder(context).data(it).build() }
    }
    StationLogoCircle(
        logoModel = imageRequest,
        size = size,
        modifier = modifier,
        fallbackBackground = if (isActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Icon(
            imageVector = Icons.Rounded.Radio,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
