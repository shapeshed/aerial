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

private val CURATED_MOODS = listOf(
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

enum class HomeViewMode {
    Cards,
    List,
}

private const val TAB_HOME = 0
private const val TAB_FAVORITES = 1

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
    LaunchedEffect(Unit) { viewModel.connect(context) }
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
    val currentRoute = backStack.lastOrNull() ?: AerialRoute.Favorites
    val rootRoute = backStack.firstOrNull() ?: AerialRoute.Favorites
    val effectiveSelectedTab = if (showHome && rootRoute == AerialRoute.Home) TAB_HOME else TAB_FAVORITES
    LaunchedEffect(showHome, uiState.home.selectedTab, currentRoute) {
        val desiredRoute = if (showHome && uiState.home.selectedTab == TAB_HOME) {
            AerialRoute.Home
        } else {
            AerialRoute.Favorites
        }
        if ((currentRoute == AerialRoute.Home || currentRoute == AerialRoute.Favorites) &&
            currentRoute != desiredRoute
        ) {
            navigator.navigateTopLevel(desiredRoute)
        }
    }
    // Hoisted so each tab keeps its scroll position across tab switches.
    val homeListState = rememberLazyGridState()
    val favoritesGridState = rememberLazyGridState()

    var miniPlayerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val stationContentBottomPadding = if (currentStation != null) with(density) { miniPlayerHeightPx.toDp() } else 0.dp
    BackHandler(enabled = showNowPlaying) { viewModel.setShowNowPlaying(false) }
    BackHandler(enabled = isSearchExpanded && !showCountrySheet && !showGenreSheet) {
        textFieldState.edit { replace(0, length, "") }
        scope.launch { searchBarState.animateToCollapsed() }
    }
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
        MainAppContent(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (mood == null) {
                    AppBarWithSearch(
                        state = searchBarState,
                        inputField = searchInputField,
                        colors = SearchBarDefaults.appBarWithSearchColors(
                            searchBarColors = SearchBarDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            scrolledSearchBarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            appBarContainerColor = MaterialTheme.colorScheme.surface,
                            scrolledAppBarContainerColor = MaterialTheme.colorScheme.surface,
                            appBarNavigationIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            appBarActionIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        actions = {
                            if (!isSearchExpanded) {
                                IconButton(
                                    onClick = { navigator.navigate(AerialRoute.Settings) },
                                    shapes = IconButtonShapes(
                                        IconButtonDefaults.smallRoundShape,
                                        IconButtonDefaults.smallPressedShape,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Rounded.Settings,
                                        contentDescription = stringResource(R.string.settings),
                                    )
                                }
                            }
                        },
                        scrollBehavior = searchScrollBehavior,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LargeFlexibleTopAppBar(
                        title = { Text(stringResource(mood.titleRes)) },
                        subtitle = { Text(stringResource(mood.detailDescriptionRes)) },
                        navigationIcon = {
                            IconButton(onClick = { navigator.goBack() }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
                            }
                        },
                        scrollBehavior = moodScrollBehavior,
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(
                    if (mood == null) searchScrollBehavior.nestedScrollConnection
                    else moodScrollBehavior.nestedScrollConnection,
                ),
        ) {
            renderDestination(destination, mood)

            MiniPlayer(
                station = currentStation,
                stationName = miniPlayerDisplay.title,
                icyInfo = playbackError
                    ?: if (isBuffering) stringResource(R.string.buffering) else miniPlayerDisplay.artist,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                onHeightChanged = { miniPlayerHeightPx = it },
                onStop = viewModel::stopAndClear,
                onTogglePlayback = viewModel::togglePlayback,
                showNextStation = hasStationNavigation,
                onNextStation = { nextPlaybackStation?.let { viewModel.play(it, playbackQueue) } },
                onExpand = { viewModel.setShowNowPlaying(true) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .semantics { traversalIndex = 1f }
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            )
        }
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
            NavDisplay(
                backStack = backStack,
                onBack = { navigator.goBack() },
                modifier = Modifier.fillMaxSize(),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<AerialRoute.Home> { renderMainRoute(TAB_HOME, null) }
                    entry<AerialRoute.Favorites> { renderMainRoute(TAB_FAVORITES, null) }
                    entry<AerialRoute.Mood> { route ->
                        renderMainRoute(
                            TAB_HOME,
                            CURATED_MOODS.firstOrNull { it.id == route.moodId },
                        )
                    }
                    entry<AerialRoute.Settings> { settingsContent { navigator.goBack() } }
                    entry<AerialRoute.AddStation> { stationEditContent(null) { navigator.goBack() } }
                    entry<AerialRoute.EditStation> { route ->
                        stationEditContent(route.stationId) { navigator.goBack() }
                    }
                },
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

@Composable
private fun MainDestinationContent(
    uiState: MainUiState,
    selectedMood: CuratedMood?,
    selectedMoodStations: List<RegistryStation>,
    savedStreamUrls: Set<String>,
    savedRegistryKeys: Set<RegistryStationKey>,
    bottomPadding: Dp,
    selectedTab: Int,
    appLocale: java.util.Locale,
    homeListState: LazyGridState,
    favoritesGridState: LazyGridState,
    onScrollToTop: () -> Unit,
    onMoodSelected: (CuratedMood) -> Unit,
    onSetForYouCountry: (String) -> Unit,
    onOpenCountrySearch: (String) -> Unit,
    onOpenRegistrySearch: () -> Unit,
    onPlayRegistryStation: (RegistryStation) -> Unit,
    onPlayRegistryQueue: (RegistryStation, List<RegistryStation>) -> Unit,
    onAddRegistryStation: (RegistryStation) -> Unit,
    onRemoveRegistryStation: (RegistryStation) -> Unit,
    onPlayFavorite: (Station) -> Unit,
    onRemoveFavorite: (Station) -> Unit,
    onHomeViewModeChange: (HomeViewMode) -> Unit,
    onSortSelected: (FavoritesSort) -> Unit,
    onStationLongPress: (Station) -> Unit,
) {
    val playback = uiState.playback.playback
    val home = uiState.home
    if (!home.isOnline) {
        NoNetworkState()
    } else if (selectedMood != null) {
        MoodDetailScreen(
            stations = selectedMoodStations,
            currentStation = playback.station,
            isPlaying = playback.isPlaying,
            isBuffering = playback.isBuffering,
            savedStreamUrls = savedStreamUrls,
            savedRegistryKeys = savedRegistryKeys,
            bottomPadding = bottomPadding,
            onPlay = { selectedMoodStations.firstOrNull()?.let { onPlayRegistryQueue(it, selectedMoodStations) } },
            onSave = { selectedMoodStations.forEach(onAddRegistryStation) },
            onAddStation = onAddRegistryStation,
            onRemoveStation = onRemoveRegistryStation,
            onPlayStation = { onPlayRegistryQueue(it, selectedMoodStations) },
        )
    } else if (selectedTab == TAB_HOME) {
        val countryCode = appLocale.country.takeIf { it.isNotBlank() } ?: "GB"
        LaunchedEffect(countryCode) { onSetForYouCountry(countryCode) }
        val hasCountrySelection = home.discovery.forYouStations.isNotEmpty()
        val forYouStations = home.discovery.forYouStations.ifEmpty { home.discovery.featuredStations }
        HomeTabContent(
            forYouStations = forYouStations,
            forYouCountry = countryName(countryCode, appLocale).takeIf { hasCountrySelection },
            recentlyPlayedStations = home.discovery.recentlyPlayedStations,
            listState = homeListState,
            bottomPadding = bottomPadding,
            onMoodTap = onMoodSelected,
            onRecentlyPlayedStationTap = onPlayRegistryStation,
            onFeaturedStationTap = onPlayRegistryStation,
            onForYouViewAll = {
                if (hasCountrySelection) onOpenCountrySearch(countryCode) else onOpenRegistrySearch()
            },
        )
    } else {
        FavoritesTabContent(
            stations = home.stations,
            currentStation = playback.station,
            isPlaying = playback.isPlaying,
            isBuffering = playback.isBuffering,
            homeViewMode = home.preferences.viewMode,
            favoritesSort = home.preferences.favoritesSort,
            gridState = favoritesGridState,
            onScrollToTop = onScrollToTop,
            bottomPadding = bottomPadding,
            onPlay = onPlayFavorite,
            onRemoveFavorite = onRemoveFavorite,
            onHomeViewModeChange = onHomeViewModeChange,
            onSortSelected = onSortSelected,
            onStationLongPress = onStationLongPress,
        )
    }
}

@Composable
private fun MainSearchOverlay(
    searchBarState: androidx.compose.material3.SearchBarState,
    inputField: @Composable () -> Unit,
    textFieldState: androidx.compose.foundation.text.input.TextFieldState,
    query: String,
    uiState: MainUiState,
    savedStreamUrls: Set<String>,
    savedRegistryKeys: Set<RegistryStationKey>,
    onCountryFilter: () -> Unit,
    onGenreFilter: () -> Unit,
    onClearFilters: () -> Unit,
    onSearch: (String) -> Unit,
    onSaveRecentSearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onPlayFavorite: (Station) -> Unit,
    onPlayRegistry: (RegistryStation) -> Unit,
    onTogglePlayback: () -> Unit,
    onAddRegistry: (RegistryStation) -> Unit,
    onRemoveRegistry: (RegistryStation) -> Unit,
    onCollapse: () -> Unit,
    onAddManually: () -> Unit,
) {
    val resultsGridState = rememberLazyGridState()
    val filterHeader: @Composable () -> Unit = {
        SearchFilterRow(
            selectedCountries = uiState.search.filters.selectedCountries,
            selectedTags = uiState.search.filters.selectedTags,
            onCountryClick = onCountryFilter,
            onGenreClick = onGenreFilter,
            onClearAll = onClearFilters,
            hasFilters = uiState.search.filters.selectedCountries.isNotEmpty() ||
                uiState.search.filters.selectedTags.isNotEmpty(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    ExpandedFullScreenContainedSearchBar(
        state = searchBarState,
        inputField = inputField,
    ) {
        if (!uiState.home.isOnline) {
            NoNetworkState()
            return@ExpandedFullScreenContainedSearchBar
        }
        val search = uiState.search
        val playback = uiState.playback.playback
        val hasFilters = search.filters.selectedCountries.isNotEmpty() || search.filters.selectedTags.isNotEmpty()
        if (query.isBlank() && !hasFilters) {
            if (search.results.recentQueries.isNotEmpty()) {
                RecentSearches(
                    searches = search.results.recentQueries,
                    onSelect = { selectedQuery ->
                        textFieldState.edit { replace(0, length, selectedQuery) }
                        onSearch(selectedQuery)
                    },
                    onRemove = onRemoveRecentSearch,
                    state = resultsGridState,
                    header = filterHeader,
                )
            } else {
                DefaultSearchResults(
                    stations = uiState.home.discovery.defaultStations,
                    savedStreamUrls = savedStreamUrls,
                    savedRegistryKeys = savedRegistryKeys,
                    currentStation = playback.station,
                    isPlaying = playback.isPlaying,
                    isBuffering = playback.isBuffering,
                    onPlay = {
                        onPlayRegistry(it)
                        textFieldState.edit { replace(0, length, "") }
                        onCollapse()
                    },
                    onPreviewPlay = onPlayRegistry,
                    onTogglePlayback = onTogglePlayback,
                    onAdd = onAddRegistry,
                    onRemove = onRemoveRegistry,
                    state = resultsGridState,
                    header = filterHeader,
                )
            }
        } else {
            RegistrySearchResults(
                favoriteResults = search.results.favoriteStations,
                results = search.results.registryStations,
                savedStreamUrls = savedStreamUrls,
                savedRegistryKeys = savedRegistryKeys,
                currentStation = playback.station,
                isPlaying = playback.isPlaying,
                isBuffering = playback.isBuffering,
                onFavoritePlay = {
                    onSaveRecentSearch(query)
                    onPlayFavorite(it)
                    textFieldState.edit { replace(0, length, "") }
                    onCollapse()
                },
                onFavoritePreviewPlay = {
                    onSaveRecentSearch(query)
                    onPlayFavorite(it)
                },
                onPlay = {
                    onSaveRecentSearch(query)
                    onPlayRegistry(it)
                    textFieldState.edit { replace(0, length, "") }
                    onCollapse()
                },
                onPreviewPlay = {
                    onSaveRecentSearch(query)
                    onPlayRegistry(it)
                },
                onTogglePlayback = onTogglePlayback,
                onAdd = onAddRegistry,
                onRemove = onRemoveRegistry,
                bottomPadding = 0.dp,
                onAddManually = {
                    onCollapse()
                    onAddManually()
                },
                state = resultsGridState,
                header = filterHeader,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainModalHost(
    showCountrySheet: Boolean,
    showGenreSheet: Boolean,
    countrySheetState: androidx.compose.material3.SheetState,
    genreSheetState: androidx.compose.material3.SheetState,
    countryQuery: String,
    genreQuery: String,
    availableCountries: List<String>,
    selectedCountries: Set<String>,
    allTags: List<String>,
    selectedTags: Set<String>,
    tagLabels: Map<String, String>,
    appLocale: java.util.Locale,
    contextStation: Station?,
    stationToDelete: Station?,
    onDismissCountry: () -> Unit,
    onDismissGenre: () -> Unit,
    onCountryQueryChange: (String) -> Unit,
    onGenreQueryChange: (String) -> Unit,
    onToggleCountry: (String) -> Unit,
    onClearCountry: () -> Unit,
    onToggleTag: (String) -> Unit,
    onClearTag: () -> Unit,
    onDismissContext: () -> Unit,
    onEditStation: (Station) -> Unit,
    onRequestDelete: (Station) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (Station) -> Unit,
) {
    if (showCountrySheet) {
        ModalBottomSheet(onDismissRequest = onDismissCountry, sheetState = countrySheetState) {
            FilterPickerSheetContent(
                title = stringResource(R.string.filter_country),
                searchLabel = stringResource(R.string.search_countries),
                query = countryQuery,
                onQueryChange = onCountryQueryChange,
                items = availableCountries,
                selectedItems = selectedCountries,
                displayName = { countryName(it, appLocale) },
                onToggle = onToggleCountry,
                onSelectionComplete = onDismissCountry,
                onClear = onClearCountry,
            )
        }
    }
    if (showGenreSheet) {
        ModalBottomSheet(onDismissRequest = onDismissGenre, sheetState = genreSheetState) {
            FilterPickerSheetContent(
                title = stringResource(R.string.filter_genre),
                searchLabel = stringResource(R.string.search_genres),
                query = genreQuery,
                onQueryChange = onGenreQueryChange,
                items = allTags,
                selectedItems = selectedTags,
                displayName = { tagLabels[it] ?: it },
                onToggle = onToggleTag,
                onSelectionComplete = onDismissGenre,
                onClear = onClearTag,
            )
        }
    }
    contextStation?.let { station ->
        ModalBottomSheet(
            onDismissRequest = onDismissContext,
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = SHEET_ENABLED_VALUES,
            ),
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            StationContextSheet(
                station = station,
                onEdit = { onEditStation(station) },
                onDelete = { onRequestDelete(station) },
            )
        }
    }
    stationToDelete?.let { station ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(stringResource(R.string.remove_station_title)) },
            text = { Text(stringResource(R.string.remove_station_message, station.name)) },
            confirmButton = {
                TextButton(onClick = { onConfirmDelete(station) }) {
                    Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

// Pre-search browse list (A-Z subsection of the registry) shown on a cold start when there
// are no recent searches, so the search view isn't empty before the user types.
@Composable
private fun DefaultSearchResults(
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
private fun RecentSearches(
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
private fun RegistrySearchResults(
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
            StationLogoSurface(logoModel = imageRequest, size = 50.dp) {
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
            StationLogoSurface(
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
                    items(
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
private fun MoodDetailScreen(
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
                StationLogoSurface(
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

@Composable
private fun favoritesSortLabel(sort: FavoritesSort): String = stringResource(
    when (sort) {
        FavoritesSort.AZ -> R.string.sort_az
        FavoritesSort.LAST_PLAYED -> R.string.sort_last_played
        FavoritesSort.MOST_PLAYED -> R.string.sort_most_played
    },
)

internal fun favoritesGridMinimumWidth(maxWidth: Dp): Dp = when {
    maxWidth >= 840.dp -> 160.dp
    maxWidth >= 600.dp -> 144.dp
    else -> 112.dp
}

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minimumCardWidth = favoritesGridMinimumWidth(maxWidth)
        LazyVerticalGrid(
            columns = if (homeViewMode == HomeViewMode.Cards) {
                GridCells.Adaptive(minSize = minimumCardWidth)
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
                            // normal tile tone so its rounded-square boundary remains visible.
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

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun StationTile(
    station: Station,
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
    val cardColor = if (isActive) MaterialTheme.colorScheme.surfaceContainerHigh
    else MaterialTheme.colorScheme.surfaceContainer
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = cardColor,
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
            StationLogoSurface(
                logoModel = logoModelFor(station.logoPath),
                size = Dp.Unspecified,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                fallbackBackground = cardColor,
                allowContrastPlate = false,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
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

// Grapheme-safe "first letter" for a station-name fallback avatar. take(1) slices by UTF-16
// code unit, not code point — a name starting with a supplementary-plane character (some
// emoji, rare CJK extensions) would cut a lone surrogate half and render as a broken glyph.
// codePointAt/charCount takes the full code point regardless of whether it's one or two chars.
private fun String.avatarInitial(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return ""
    val charCount = Character.charCount(trimmed.codePointAt(0))
    return trimmed.substring(0, charCount).uppercase()
}

// A logo path is either a remote URL or a local file path — the latter needs wrapping in a
// File so Coil resolves it, rather than trying (and failing) to treat it as a bare URI string.
// Recently-played entries in particular can carry a local path here: they're backed by
// RegistryStation, but MainViewModel substitutes a locally-saved station's own logoPath when
// the registry's copy has none.
private fun logoModelFor(path: String): Any? = when {
    path.startsWith("http") -> path
    path.isNotEmpty() -> File(path)
    else -> null
}

@Composable
private fun ForYouStationCard(
    station: com.shapeshed.aerial.data.RegistryStation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Match the flat tonal containment used by the favourites station tiles.
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.width(140.dp).height(116.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp).fillMaxSize(),
        ) {
            StationLogoSurface(
                logoModel = logoModelFor(station.logoUrl),
                size = 64.dp,
            ) {
                Text(
                    text = station.name.avatarInitial(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Square station logo surface shared by grids, lists, search results, and player artwork.
// Circular artwork is masked inside the square surface so transparent regions reveal the same
// surface as the surrounding item.
@Composable
fun StationLogoSurface(
    logoModel: Any?,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape? = null,
    fallbackBackground: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    allowContrastPlate: Boolean = true,
    fallback: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    var logoFailed by remember(logoModel) { mutableStateOf(false) }
    var logoIsLight by remember(logoModel) { mutableStateOf(false) }
    var logoPrefersLightPlate by remember(logoModel) { mutableStateOf(false) }
    var logoHasTransparentMargin by remember(logoModel) { mutableStateOf(false) }
    var logoHasCircularArtwork by remember(logoModel) { mutableStateOf(false) }
    var loadedLogo by remember(logoModel) { mutableStateOf<coil3.Image?>(null) }
    LaunchedEffect(logoModel, loadedLogo) {
        val image = loadedLogo ?: return@LaunchedEffect
        val appearance = sharedLogoAppearanceAnalyzer.analyze(logoModel.toString(), image)
        logoIsLight = appearance.isLight
        logoPrefersLightPlate = appearance.prefersLightPlate
        logoHasTransparentMargin = appearance.hasTransparentMargin
        logoHasCircularArtwork = appearance.hasCircularArtwork
    }
    val showLogo = logoModel != null && !logoFailed
    val containerShape = shape ?: MaterialTheme.shapes.small
    val artworkShape = if (logoHasCircularArtwork) CircleShape else containerShape
    val useContrastPlate = allowContrastPlate && logoHasTransparentMargin && artworkNeedsContrastPlate(
        isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
        isArtworkLight = logoIsLight,
        prefersLightPlate = logoPrefersLightPlate,
        hasTransparentMargin = true,
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(if (size == Dp.Unspecified) Modifier.fillMaxSize() else Modifier.size(size))
            .clip(containerShape)
            .background(
                // The outer container is the item's square Material surface. Circular artwork
                // is masked below so its surrounding surface remains visible.
                fallbackBackground,
            ),
    ) {
        if (showLogo) {
            AsyncImage(
                model = logoModel,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { logoFailed = true },
                onSuccess = { state -> loadedLogo = state.result.image },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(artworkShape)
                    .background(
                        if (useContrastPlate) {
                            // Use the shared contrast-aware plate only when the artwork would
                            // otherwise disappear against the current theme surface.
                            artworkPlateColor(
                                isArtworkLight = logoIsLight,
                                prefersLightPlate = logoPrefersLightPlate,
                                hasTransparentMargin = true,
                            )
                        } else {
                            // Artwork uses the same surface as its surrounding item. Transparent
                            // regions reveal it and opaque artwork no longer creates a tonal square.
                            fallbackBackground
                        },
                        artworkShape,
                    ),
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
    surfaceColor: androidx.compose.ui.graphics.Color? = null,
) {
    val logoModel = logoModelFor(station.logoPath)
    StationLogoSurface(
        logoModel = logoModel,
        size = size,
        modifier = modifier,
        fallbackBackground = surfaceColor ?: if (isActive) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Icon(
            imageVector = Icons.Rounded.Radio,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
