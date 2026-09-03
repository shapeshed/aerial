package com.shapeshed.aerial.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalToggleButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AppBarWithSearch
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import androidx.compose.ui.res.stringResource
import com.shapeshed.aerial.R
import com.shapeshed.aerial.navigation.AerialNavigator
import com.shapeshed.aerial.navigation.AerialRoute
import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

// Localized country name from the stored ISO code via ICU, in the app's current locale.
// Cached per (code, language) so Locale.Builder isn't called on every row recomposition.
private val countryNameCache = java.util.concurrent.ConcurrentHashMap<String, String>()
internal fun countryName(code: String, locale: java.util.Locale): String =
    countryNameCache.getOrPut("$code|${locale.language}") {
        java.util.Locale.Builder().setRegion(code).build().getDisplayCountry(locale).ifBlank { code }
    }

// The curated genre tags are a fixed set, so their display is localized while the English tag
// stays the matching key. Unknown tags fall back to their raw value.
@Composable
internal fun rememberTagLabels(): Map<String, String> {
    val news = stringResource(R.string.tag_news)
    val sport = stringResource(R.string.tag_sport)
    val pop = stringResource(R.string.tag_pop)
    val rock = stringResource(R.string.tag_rock)
    val jazz = stringResource(R.string.tag_jazz)
    val classical = stringResource(R.string.tag_classical)
    val dance = stringResource(R.string.tag_dance)
    val soul = stringResource(R.string.tag_soul)
    val country = stringResource(R.string.tag_country)
    val electronic = stringResource(R.string.tag_electronic)
    return remember(news, sport, pop, rock, jazz, classical, dance, soul, country, electronic) {
        mapOf(
            "News" to news,
            "Sport" to sport,
            "Pop" to pop,
            "Rock" to rock,
            "Jazz" to jazz,
            "Classical" to classical,
            "Dance" to dance,
            "Soul" to soul,
            "Country" to country,
            "Electronic" to electronic,
        )
    }
}

// One consistent tonal surface for every mood tile, per M3 guidance: primary/secondary/
// tertiary are meant for small high-emphasis elements (buttons, FABs, selected states), not
// large fills, and rotating colours across a grid of peer items — none more important than
// another — is a decorative pattern, not a functional use of colour. The icon and label are
// what tell the six moods apart, matching how every other card in this app (For You, search
// rows, favourites tiles) uses one steady surface rather than per-item colour.
internal data class CuratedMood(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val detailDescriptionRes: Int = descriptionRes,
    val icon: ImageVector,
)

internal val CURATED_MOODS = listOf(
    CuratedMood(
        id = "relax",
        titleRes = R.string.mood_relax,
        descriptionRes = R.string.mood_relax_desc,
        detailDescriptionRes = R.string.mood_relax_detail_desc,
        icon = Icons.Rounded.BeachAccess,
    ),
    CuratedMood(
        id = "focus",
        titleRes = R.string.mood_focus,
        descriptionRes = R.string.mood_focus_desc,
        icon = Icons.Rounded.Psychology,
    ),
    CuratedMood(
        id = "morning",
        titleRes = R.string.mood_morning,
        descriptionRes = R.string.mood_morning_desc,
        icon = Icons.Rounded.WbSunny,
    ),
    CuratedMood(
        id = "driving",
        titleRes = R.string.mood_driving,
        descriptionRes = R.string.mood_driving_desc,
        icon = Icons.Rounded.Landscape,
    ),
    CuratedMood(
        id = "late_night",
        titleRes = R.string.mood_late_night,
        descriptionRes = R.string.mood_late_night_desc,
        icon = Icons.Rounded.NightsStay,
    ),
    CuratedMood(
        id = "workout",
        titleRes = R.string.mood_workout,
        descriptionRes = R.string.mood_workout_desc,
        icon = Icons.Rounded.FitnessCenter,
    ),
)

internal data class RegistryStationKey(
    val provider: String,
    val providerId: String,
)

internal fun RegistryStation.savedKey(): RegistryStationKey? =
    RegistryStationKey(provider, providerId).takeIf {
        it.provider.isNotBlank() && it.providerId.isNotBlank()
    }

internal fun Station.savedKey(): RegistryStationKey? =
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

enum class HomeViewMode {
    Cards,
    List,
}

internal const val TAB_HOME = 0
internal const val TAB_FAVORITES = 1

// Bottom sheets in this app only ever hide or expand, never partially — shared so it isn't
// reallocated on every recomposition of each sheet's host composable.
@OptIn(ExperimentalMaterial3Api::class)
internal val SHEET_ENABLED_VALUES = setOf(SheetValue.Hidden, SheetValue.Expanded)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    settingsContent: @Composable (onDismiss: () -> Unit) -> Unit,
    stationEditContent: @Composable (stationId: Long?, onDismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.mainUiState.collectAsStateWithLifecycle()
    val playbackUiState = uiState.playback.playback
    val stations = uiState.home.stations
    val currentStation = playbackUiState.station
    val playbackQueue = playbackUiState.queue
    val nextPlaybackStation = currentStation?.let { playbackQueue.stationAtOffset(it, 1) }
    val hasStationNavigation = nextPlaybackStation != null
    val isPlaying = playbackUiState.isPlaying
    val isBuffering = playbackUiState.isBuffering
    val currentTrackTitle = playbackUiState.trackTitle
    val currentTrackArtist = playbackUiState.trackArtist
    val miniPlayerDisplay = computeTrackDisplay(
        stationName = currentStation?.name.orEmpty(),
        trackTitle = currentTrackTitle,
        trackArtist = currentTrackArtist,
        liveRadio = stringResource(R.string.live_radio),
    )
    val currentBitrateKbps = playbackUiState.bitrateKbps
    val sleepTimer = uiState.playback.sleepTimer
    val playbackError = playbackUiState.error
    val recentlyAddedStationId = uiState.playback.recentlyAddedStationId
    val isOnline = uiState.home.isOnline

    val haptic = LocalHapticFeedback.current
    val showNowPlaying = uiState.playback.showNowPlaying
    val registrySearchResults = uiState.search.results.registryStations
    val favoriteSearchResults = uiState.search.results.favoriteStations
    val recentSearches = uiState.search.results.recentQueries
    val allTags = uiState.search.filters.allTags
    val selectedCountries = uiState.search.filters.selectedCountries
    val selectedTags = uiState.search.filters.selectedTags
    val availableCountries = uiState.search.filters.availableCountries
    val featuredStations = uiState.home.discovery.featuredStations
    val forYouStations = uiState.home.discovery.forYouStations
    val recentlyPlayedStations = uiState.home.discovery.recentlyPlayedStations
    val defaultStations = uiState.home.discovery.defaultStations
    val curatedMoodStations = uiState.home.discovery.curatedMoodStations
    val homeViewMode = uiState.home.preferences.viewMode
    val favoritesSort = uiState.home.preferences.favoritesSort
    val showStreamBitrate = uiState.home.preferences.showStreamBitrate
    val showHome = uiState.home.preferences.showHome
    val appLocale = LocalConfiguration.current.locales[0]
    val tagLabels = rememberTagLabels()

    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberContainedSearchBarState()
    val searchDestination = if (showHome) uiState.home.selectedTab else TAB_FAVORITES
    val searchScrollBehavior = key(searchDestination) {
        SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    }
    val isSearchExpanded by remember { derivedStateOf { searchBarState.currentValue == SearchBarValue.Expanded } }
    val searchQueryText by remember { derivedStateOf { textFieldState.text.toString() } }
    var showCountrySheet by remember { mutableStateOf(false) }
    var showGenreSheet by remember { mutableStateOf(false) }
    var contextStation by remember { mutableStateOf<Station?>(null) }
    var stationToDelete by remember { mutableStateOf<Station?>(null) }
    val countrySheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = SHEET_ENABLED_VALUES,
    )
    val genreSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = SHEET_ENABLED_VALUES,
    )
    var countryFilterQuery by remember { mutableStateOf("") }
    var genreFilterQuery by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val favoriteRemovedMessage = stringResource(R.string.favorite_removed_message)
    val undoLabel = stringResource(R.string.undo)
    val dismissCountrySheet = {
        scope.launch {
            countrySheetState.hide()
            showCountrySheet = false
            countryFilterQuery = ""
        }
        Unit
    }
    val dismissGenreSheet = {
        scope.launch {
            genreSheetState.hide()
            showGenreSheet = false
            genreFilterQuery = ""
        }
        Unit
    }
    fun openFilterSheet(open: () -> Unit) {
        // Release the search field before presenting the sheet so the IME does not resize
        // the sheet during its entrance animation.
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        open()
    }

    val savedStreamUrls = remember(stations) { stations.map { it.streamUrl }.toSet() }
    val savedRegistryKeys = remember(stations) { stations.mapNotNull { it.savedKey() }.toSet() }

    val backStack = rememberNavBackStack(
        if (showHome && uiState.home.selectedTab == TAB_HOME) AerialRoute.Home else AerialRoute.Favorites,
    )
    val navigator = remember(backStack) { AerialNavigator(backStack) }
    val currentRoute = backStack.lastOrNull() as? AerialRoute ?: AerialRoute.Favorites
    val rootRoute = backStack.firstOrNull() ?: AerialRoute.Favorites
    val effectiveSelectedTab = if (showHome && rootRoute == AerialRoute.Home) TAB_HOME else TAB_FAVORITES
    // Hoisted so each tab keeps its scroll position across tab switches.
    val homeListState = rememberLazyGridState()
    val favoritesGridState = rememberLazyGridState()

    var miniPlayerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val stationContentBottomPadding = if (currentStation != null) with(density) { miniPlayerHeightPx.toDp() } else 0.dp
    MainScreenEffects(
        viewModel = viewModel,
        context = context,
        showNowPlaying = showNowPlaying,
        isSearchExpanded = isSearchExpanded,
        showCountrySheet = showCountrySheet,
        showGenreSheet = showGenreSheet,
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        scope = scope,
        searchQueryText = searchQueryText,
        recentlyAddedStationId = recentlyAddedStationId,
        showHome = showHome,
        selectedTab = uiState.home.selectedTab,
        currentRoute = currentRoute,
        navigator = navigator,
    )
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
                }
            },
        )
    }

    val renderDestination: @Composable (Int, CuratedMood?) -> Unit = { destination, mood ->
        MainDestinationContent(
            uiState = uiState,
            selectedMood = mood,
            selectedMoodStations = mood?.let { curatedMoodStations[it.id] }.orEmpty(),
            savedStreamUrls = savedStreamUrls,
            savedRegistryKeys = savedRegistryKeys,
            bottomPadding = stationContentBottomPadding,
            selectedTab = destination,
            appLocale = appLocale,
            homeListState = homeListState,
            favoritesGridState = favoritesGridState,
            onScrollToTop = {
                searchScrollBehavior.scrollState.scrollOffset = 0f
                searchScrollBehavior.scrollState.contentOffset = 0f
            },
            onMoodSelected = { navigator.navigate(AerialRoute.Mood(it.id)) },
            onSetForYouCountry = viewModel::setForYouCountry,
            onOpenCountrySearch = ::openCountrySearch,
            onOpenRegistrySearch = ::openRegistrySearch,
            onPlayRegistryStation = viewModel::playFromRegistry,
            onPlayRegistryQueue = viewModel::playFromRegistry,
            onAddRegistryStation = viewModel::addFromRegistry,
            onRemoveRegistryStation = viewModel::removeFromRegistry,
            onPlayFavorite = { viewModel.play(it, stations) },
            onRemoveFavorite = { station ->
                viewModel.toggleFavorite(station)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = favoriteRemovedMessage.format(station.name),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreFavorite(station)
                    }
                }
            },
            onHomeViewModeChange = viewModel::setHomeViewMode,
            onSortSelected = viewModel::setFavoritesSort,
            onStationLongPress = { contextStation = it },
        )
    }

    val moodScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isMainRoute = currentRoute == AerialRoute.Home ||
        currentRoute == AerialRoute.Favorites ||
        currentRoute is AerialRoute.Mood
    val isSearchRoute = currentRoute == AerialRoute.Home || currentRoute == AerialRoute.Favorites

    val renderMainRoute: @Composable (Int, CuratedMood?) -> Unit = { destination, mood ->
        MainRouteContent(
            destination = destination,
            mood = mood,
            snackbarHostState = snackbarHostState,
            searchBarState = searchBarState,
            searchInputField = searchInputField,
            isSearchExpanded = isSearchExpanded,
            onOpenSettings = { navigator.navigate(AerialRoute.Settings) },
            onBack = { navigator.goBack() },
            searchScrollBehavior = searchScrollBehavior,
            moodScrollBehavior = moodScrollBehavior,
            renderDestination = renderDestination,
            currentStation = currentStation,
            miniPlayerDisplay = miniPlayerDisplay,
            playbackError = playbackError,
            isBuffering = isBuffering,
            isPlaying = isPlaying,
            hasStationNavigation = hasStationNavigation,
            onHeightChanged = { miniPlayerHeightPx = it },
            onStop = viewModel::stopAndClear,
            onTogglePlayback = viewModel::togglePlayback,
            onPlayNext = { nextPlaybackStation?.let { viewModel.play(it, playbackQueue) } },
            onExpand = { viewModel.setShowNowPlaying(true) },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true },
    ) {
        AdaptiveNavigationShell(
            selectedDestination = effectiveSelectedTab,
            showNavigation = showHome && isSearchRoute,
            onDestinationSelected = { destination ->
                viewModel.setSelectedHomeTab(destination)
                val route = if (destination == TAB_HOME && showHome) {
                    AerialRoute.Home
                } else {
                    AerialRoute.Favorites
                }
                navigator.navigateTopLevel(route)
            },
        ) {
            MainNavigationHost(
                backStack = backStack,
                navigator = navigator,
                renderMainRoute = renderMainRoute,
                settingsContent = settingsContent,
                stationEditContent = stationEditContent,
            )
        }

        AnimatedVisibility(
            visible = showNowPlaying && isMainRoute,
            enter = slideInVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize(),
        ) {
            val station = currentStation
            if (station != null) {
                // Swipe order frozen for the lifetime of the pane: under the Last/Most played
                // sorts, playing a station immediately re-sorts the live list, which would make
                // consecutive swipes ping-pong between the same two stations.
                val fallbackSwipeStations = remember { viewModel.stations.value }
                val swipeStations = playbackUiState.queue
                    .takeIf { queue -> queue.size > 1 && queue.any(station::matches) }
                    ?: fallbackSwipeStations
                NowPlayingScreen(
                    station = station,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    currentTrackTitle = currentTrackTitle,
                    currentTrackArtist = currentTrackArtist,
                    currentBitrateKbps = currentBitrateKbps,
                    showStreamBitrate = showStreamBitrate,
                    sleepTimer = sleepTimer,
                    swipeStations = swipeStations,
                    onPlayStation = { viewModel.play(it, swipeStations) },
                    onPreviousStation = {
                        swipeStations.stationAtOffset(station, -1)?.let { viewModel.play(it, swipeStations) }
                    },
                    onNextStation = {
                        swipeStations.stationAtOffset(station, 1)?.let { viewModel.play(it, swipeStations) }
                    },
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

        if (isSearchRoute) MainSearchOverlay(
            searchBarState = searchBarState,
            inputField = searchInputField,
            textFieldState = textFieldState,
            query = searchQueryText,
            uiState = uiState,
            savedStreamUrls = savedStreamUrls,
            savedRegistryKeys = savedRegistryKeys,
            onCountryFilter = { openFilterSheet { showCountrySheet = true } },
            onGenreFilter = { openFilterSheet { showGenreSheet = true } },
            onClearFilters = viewModel::clearAllFilters,
            onSearch = viewModel::searchRegistry,
            onSaveRecentSearch = viewModel::saveRecentSearch,
            onRemoveRecentSearch = viewModel::removeRecentSearch,
            onPlayFavorite = viewModel::play,
            onPlayRegistry = viewModel::playFromRegistry,
            onTogglePlayback = viewModel::togglePlayback,
            onAddRegistry = viewModel::addFromRegistry,
            onRemoveRegistry = viewModel::removeFromRegistry,
            onCollapse = { scope.launch { searchBarState.animateToCollapsed() } },
            onAddManually = { navigator.navigate(AerialRoute.AddStation) },
        )

        if (isSearchRoute) MainModalHost(
            showCountrySheet = showCountrySheet,
            showGenreSheet = showGenreSheet,
            countrySheetState = countrySheetState,
            genreSheetState = genreSheetState,
            countryQuery = countryFilterQuery,
            genreQuery = genreFilterQuery,
            availableCountries = availableCountries,
            selectedCountries = selectedCountries,
            allTags = allTags,
            selectedTags = selectedTags,
            tagLabels = tagLabels,
            appLocale = appLocale,
            contextStation = contextStation,
            stationToDelete = stationToDelete,
            onDismissCountry = dismissCountrySheet,
            onDismissGenre = dismissGenreSheet,
            onCountryQueryChange = { countryFilterQuery = it },
            onGenreQueryChange = { genreFilterQuery = it },
            onToggleCountry = viewModel::toggleCountryFilter,
            onClearCountry = viewModel::clearCountryFilter,
            onToggleTag = viewModel::toggleTagFilter,
            onClearTag = viewModel::clearTagFilter,
            onDismissContext = { contextStation = null },
            onEditStation = {
                contextStation = null
                navigator.navigate(AerialRoute.EditStation(it.id))
            },
            onRequestDelete = {
                stationToDelete = it
                contextStation = null
            },
            onDismissDelete = { stationToDelete = null },
            onConfirmDelete = {
                viewModel.deleteStation(it)
                stationToDelete = null
            },
        )
    }

}
