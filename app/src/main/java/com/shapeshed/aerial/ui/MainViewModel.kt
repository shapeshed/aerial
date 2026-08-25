package com.shapeshed.aerial.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.PlayerService
import com.shapeshed.aerial.R
import com.shapeshed.aerial.stationNameFromMediaMetadata
import com.shapeshed.aerial.SHOW_STREAM_BITRATE_KEY
import com.shapeshed.aerial.SHOW_HOME_KEY
import com.shapeshed.aerial.data.ACTION_SLEEP_TIMER_CANCEL
import com.shapeshed.aerial.data.ACTION_SLEEP_TIMER_SET
import com.shapeshed.aerial.data.FAVORITES_SORT_KEY
import com.shapeshed.aerial.data.FavoritesSort
import com.shapeshed.aerial.data.LAST_PLAYED_STATION_KEY
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.SLEEP_TIMER_DURATION_MS
import com.shapeshed.aerial.data.SleepTimerState
import com.shapeshed.aerial.data.SleepTimerStore
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.resolveQueueStart
import com.shapeshed.aerial.data.sortStations
import com.shapeshed.aerial.data.lastPlayedStationSnapshot
import com.shapeshed.aerial.data.toLastPlayedJson
import com.shapeshed.aerial.data.parseTrackMetadata
import com.shapeshed.aerial.toEphemeralStation
import com.shapeshed.aerial.toSystemPlayableMediaItem
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HOME_CARDS_VIEW_KEY = booleanPreferencesKey("home_cards_view")
private val LAST_HOME_TAB_KEY = intPreferencesKey("last_home_tab")
private const val RECENTLY_PLAYED_LIMIT = 10

/** Keeps a favourite usable when its app-private cached logo disappeared during reinstall. */
internal fun recoverLogoPath(storedPath: String, remoteLogoUrl: String, fileExists: Boolean): String =
    when {
        // A file that still exists may be artwork explicitly selected by the user. It is the
        // authoritative value; the registry URL is only a recovery source for missing cached
        // files (for example after restoring a backup or reinstalling the app).
        fileExists -> storedPath
        remoteLogoUrl.isNotBlank() -> remoteLogoUrl
        else -> storedPath
    }

/** Chooses artwork for a recently-played registry row when a saved logo file may be stale. */
internal fun recentlyPlayedLogoPath(storedPath: String, remoteLogoUrl: String): String =
    recoverLogoPath(
        storedPath = storedPath,
        remoteLogoUrl = remoteLogoUrl,
        fileExists = storedPath.isNotBlank() && !storedPath.startsWith("http") && File(storedPath).isFile,
    )

/** Reorders an existing Media3 playlist without clearing or preparing the active item. */
internal fun reorderPlayerPlaylist(player: Player, current: List<Station>, desired: List<Station>) {
    val working = current.toMutableList()
    desired.forEachIndexed { targetIndex, station ->
        val currentIndex = working.indexOfFirst { it.matches(station) }
        if (currentIndex >= 0 && currentIndex != targetIndex) {
            player.moveMediaItem(currentIndex, targetIndex)
            working.add(targetIndex, working.removeAt(currentIndex))
        }
    }
}

class MainViewModel(
    application: Application,
    private val repository: StationRepository,
    private val registryRepository: RegistryRepository,
    private val dataStore: DataStore<Preferences>,
    // Default is test/preview-only — the viewModelFactory { initializer { } } in MainActivity
    // always passes an explicit SavedStateHandle via CreationExtras.createSavedStateHandle().
    @Suppress("VisibleForTests")
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val artworkLoader: ArtworkLoader = CoilArtworkLoader(application),
) : AndroidViewModel(application) {

    val isOnline = (application as AerialApp).networkMonitor.isOnline
    private val searchStateHolder = SearchStateHolder(
        scope = viewModelScope,
        repository = repository,
        registryRepository = registryRepository,
        dataStore = dataStore,
    )

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

    private val _featuredStations = MutableStateFlow<List<RegistryStation>>(emptyList())
    val featuredStations: StateFlow<List<RegistryStation>> = _featuredStations.asStateFlow()

    // For You is loaded for the device locale's country: a curated selection where one
    // exists, otherwise a random sample of that country's stations with artwork. Keyed by
    // country (distinctUntilChanged below) so the random pick stays stable for the session.
    private val _forYouCountry = MutableStateFlow("GB")
    private val _forYouStations = MutableStateFlow<List<RegistryStation>>(emptyList())
    val forYouStations: StateFlow<List<RegistryStation>> = _forYouStations.asStateFlow()

    fun setForYouCountry(countryCode: String) {
        if (countryCode.isNotBlank()) _forYouCountry.value = countryCode
    }

    private val _defaultStations = MutableStateFlow<List<RegistryStation>>(emptyList())
    val defaultStations: StateFlow<List<RegistryStation>> = _defaultStations.asStateFlow()

    private val _curatedMoodStations = MutableStateFlow<Map<String, List<RegistryStation>>>(emptyMap())
    val curatedMoodStations: StateFlow<Map<String, List<RegistryStation>>> = _curatedMoodStations.asStateFlow()

    // Recently played stations for the home screen, same source as Android Auto's Recently
    // Played folder: the play_history table resolved against the registry (entries whose
    // station is no longer in the registry are skipped). Room re-emits on every recorded
    // play, so the row reorders live. The first resolution gates isInitialized (and so the
    // splash screen) below: the home list's scroll position is restored against the first
    // composition, so this section must already be present in it rather than streaming in
    // afterwards and shifting the restored position.
    private val _recentlyPlayedStations = MutableStateFlow<List<RegistryStation>>(emptyList())
    val recentlyPlayedStations: StateFlow<List<RegistryStation>> = _recentlyPlayedStations.asStateFlow()
    private val recentlyPlayedFirstLoad = CompletableDeferred<Unit>()

    private fun initialize() {
        viewModelScope.launch {
            migrateImportedArtwork()
        }
        viewModelScope.launch {
            repository.recentlyPlayedAsFlow(RECENTLY_PLAYED_LIMIT)
                .map { entries ->
                    entries.mapNotNull { entry ->
                        val registryStation = registryRepository.getByProviderId(entry.provider, entry.providerId)
                            ?: return@mapNotNull null
                        // The registry's own copy may have no logo, or one the user has
                        // replaced locally (e.g. a custom-uploaded SVG) — prefer the user's
                        // saved station's artwork when this station is saved locally.
                        val localLogoPath = repository.findMatching(registryStation)?.logoPath.orEmpty()
                        val artworkPath = recentlyPlayedLogoPath(localLogoPath, registryStation.logoUrl)
                        if (artworkPath != registryStation.logoUrl) {
                            registryStation.copy(logoUrl = artworkPath)
                        } else registryStation
                    }
                }
                .collect {
                    _recentlyPlayedStations.value = it
                    recentlyPlayedFirstLoad.complete(Unit)
                }
        }
        viewModelScope.launch {
            repository.getAll().first()
            recentlyPlayedFirstLoad.await()
            _isInitialized.value = true
        }
        viewModelScope.launch {
            restoreLastPlayedStation()
        }
        viewModelScope.launch {
            registryRepository.countAsFlow()
                .filter { it > 0 }
                .distinctUntilChanged()
                .collect {
                    _featuredStations.value = registryRepository.featuredStations()
                    _defaultStations.value = registryRepository.defaultStations()
                    _curatedMoodStations.value = registryRepository.curatedMoodStations()
                    _availableCountries.value = registryRepository.availableCountryCodes()
                    _allTags.value = registryRepository.availableTags()
                }
        }
        viewModelScope.launch {
            combine(
                registryRepository.countAsFlow().filter { it > 0 }.distinctUntilChanged(),
                _forYouCountry,
            ) { _, country -> country }
                .distinctUntilChanged()
                .collect { country ->
                    _forYouStations.value = registryRepository.forYouStations(country)
                }
        }
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            searchStateHolder.restoreFilters(prefs)
            _selectedHomeTab.value = prefs[LAST_HOME_TAB_KEY] ?: 0
            _favoritesSort.value = prefs[FAVORITES_SORT_KEY]
                ?.let { saved -> FavoritesSort.entries.firstOrNull { it.name == saved } }
                ?: FavoritesSort.AZ
        }
    }

    /** Replace only missing imported artwork with its registry URL; preserve user-selected files. */
    private suspend fun migrateImportedArtwork() {
        repository.getAll().first().forEach { station ->
            val registry = when {
                station.provider.isNotBlank() && station.providerId.isNotBlank() ->
                    registryRepository.getByProviderId(station.provider, station.providerId)
                else -> registryRepository.getByStreamUrl(station.streamUrl)
            }
            val registryLogo = registry?.logoUrl.orEmpty()
            val localPath = station.logoPath
            val recoveredPath = recoverLogoPath(
                storedPath = localPath,
                remoteLogoUrl = registryLogo,
                fileExists = localPath.isNotBlank() &&
                    !localPath.startsWith("http") &&
                    File(localPath).isFile,
            )
            if (recoveredPath != localPath) {
                repository.update(station.copy(logoPath = recoveredPath))
            }
        }
    }

    private val _allStations: StateFlow<List<Station>> = repository.getAll()
        .map { stations -> stations.map { recoverStationArtwork(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private suspend fun recoverStationArtwork(station: Station): Station {
        val localPath = station.logoPath
        val registry = when {
            station.provider.isNotBlank() && station.providerId.isNotBlank() ->
                registryRepository.getByProviderId(station.provider, station.providerId)
            else -> registryRepository.getByStreamUrl(station.streamUrl)
        }
        val recoveredPath = recoverLogoPath(
            localPath,
            registry?.logoUrl.orEmpty(),
            fileExists = localPath.isNotBlank() && !localPath.startsWith("http") && File(localPath).isFile,
        )
        return if (recoveredPath == localPath) station else station.copy(logoPath = recoveredPath)
    }

    // Sort order for the Favourites tab. Updated synchronously so the list reorders
    // immediately; the DataStore write catches up in the background.
    private val _favoritesSort = MutableStateFlow(FavoritesSort.AZ)
    val favoritesSort: StateFlow<FavoritesSort> = _favoritesSort.asStateFlow()

    private val _activeFavoritesOrder = MutableStateFlow<List<Long>?>(null)

    private fun favoritesOrder(queue: List<Station>): List<Long>? {
        if (queue.size < 2 || queue.any { it.id == 0L }) return null
        val favorites = _allStations.value.filter(Station::isFavorite)
        return queue.takeIf { ordered ->
            ordered.size == favorites.size && favorites.all { favorite -> ordered.any { it.matches(favorite) } }
        }?.map(Station::id)
    }

    private fun sortFavorites(stations: List<Station>, sort: FavoritesSort): List<Station> =
        sortStations(stations.filter(Station::isFavorite), sort)

    fun setFavoritesSort(sort: FavoritesSort) {
        _favoritesSort.value = sort
        refreshActiveFavoritesQueue(sort)
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[FAVORITES_SORT_KEY] = sort.name }
        }
    }

    /** Keeps next/previous aligned with the order currently shown in the favourites tab. */
    private fun refreshActiveFavoritesQueue(sort: FavoritesSort) {
        val activeQueue = _playbackUiState.value.queue
        if (activeQueue.size < 2) return

        val favorites = _allStations.value.filter(Station::isFavorite)
        val isFavoritesQueue = favorites.size == activeQueue.size &&
            favorites.all { favorite -> activeQueue.any { it.matches(favorite) } }
        if (!isFavoritesQueue) return

        val reorderedQueue = sortStations(favorites, sort)
        _activeFavoritesOrder.value = reorderedQueue.map(Station::id)
        _playbackUiState.value = _playbackUiState.value.copy(queue = reorderedQueue)
        controller?.let { player -> reorderPlayerPlaylist(player, activeQueue, reorderedQueue) }
        val currentStation = _playbackUiState.value.station ?: return
        persistLastPlayedStation(currentStation, reorderedQueue)
    }

    val stations: StateFlow<List<Station>> = combine(_allStations, _favoritesSort, _activeFavoritesOrder) { list, sort, activeOrder ->
        val sorted = sortFavorites(list, sort)
        activeOrder?.let { order ->
            sorted.sortedBy { station -> order.indexOf(station.id).takeIf { it >= 0 } ?: Int.MAX_VALUE }
        } ?: sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentStationId = MutableStateFlow<Long?>(null)
    private val _ephemeralStation = MutableStateFlow<Station?>(null)
    private val _playbackUiState = MutableStateFlow(PlaybackUiState())
    val playbackUiState: StateFlow<PlaybackUiState> = _playbackUiState.asStateFlow()
    private data class PendingPlaybackMetadata(
        val station: Station,
        val title: String?,
        val artist: String?,
    )
    private var pendingPlaybackMetadata: PendingPlaybackMetadata? = null
    // Carries the last-played station to loadStationPaused() once the MediaController connects.
    // CompletableDeferred ensures the handoff is safe regardless of which side wins the race.
    private val pendingRestoreStation = CompletableDeferred<Station?>()

    init {
        // If the current station's row is deleted outside this ViewModel (e.g. unfavourited
        // from the media notification), hand it off to the ephemeral slot so playback and the
        // Now Playing UI survive. In-app delete paths clear _currentStationId before deleting,
        // so they never trigger this. The previous-list guard stops the initial empty emission
        // from being mistaken for a deletion.
        viewModelScope.launch {
            var previous: List<Station> = emptyList()
            _allStations.collect { list ->
                val id = _currentStationId.value
                if (id != null && list.none { it.id == id }) {
                    previous.firstOrNull { it.id == id }?.let { removed ->
                        setCurrentStation(removed.copy(id = 0, isFavorite = false))
                    }
            } else if (id != null) {
                    list.firstOrNull { it.id == id }?.let(::refreshCurrentStation)
                }
                refreshActiveFavoritesQueueForStationUpdate(list)
                previous = list
            }
        }
    }

    /**
     * A play updates [Station.lastPlayedAt] or [Station.playCount] asynchronously in Room. When
     * a play-dependent Favorites sort is active, keep both the visible list and the next/previous
     * queue aligned with that newer row order instead of letting the queue snapshot freeze it.
     */
    private fun refreshActiveFavoritesQueueForStationUpdate(stations: List<Station>) {
        val sort = _favoritesSort.value
        if (sort != FavoritesSort.LAST_PLAYED && sort != FavoritesSort.MOST_PLAYED) return
        val activeQueue = _playbackUiState.value.queue
        if (activeQueue.size < 2) return
        val favorites = stations.filter(Station::isFavorite)
        val isFavoritesQueue = favorites.size == activeQueue.size &&
            favorites.all { favorite -> activeQueue.any { it.matches(favorite) } }
        if (!isFavoritesQueue) return

        val reorderedQueue = sortStations(favorites, sort)
        if (reorderedQueue.map(Station::id) == activeQueue.map(Station::id)) return
        _activeFavoritesOrder.value = reorderedQueue.map(Station::id)
        _playbackUiState.value = _playbackUiState.value.copy(queue = reorderedQueue)
        controller?.let { player -> reorderPlayerPlaylist(player, activeQueue, reorderedQueue) }
        _playbackUiState.value.station?.let { persistLastPlayedStation(it, reorderedQueue) }
    }

    // Single derived "what's playing" summary. Recomputes whenever stream metadata or the
    // current station changes so the UI never has to reconcile the sources itself.
    val nowPlayingDisplay: StateFlow<NowPlayingDisplay> = playbackUiState
        .map { playback ->
            computeNowPlayingDisplay(
                playback.station?.name.orEmpty(),
                playback.trackTitle,
                playback.trackArtist,
                liveRadio(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingDisplay("", ""))

    // Localized "Live Radio" — the placeholder shown in the notification / mini player when a
    // station has no track metadata, and the sentinel used to detect that placeholder below.
    private fun liveRadio(): String = getApplication<Application>().getString(R.string.live_radio)

    val sleepTimer: StateFlow<SleepTimerState?> = SleepTimerStore.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setSleepTimer(durationMs: Long) {
        val ctrl = controller ?: return
        ctrl.sendCustomCommand(
            SessionCommand(ACTION_SLEEP_TIMER_SET, Bundle.EMPTY),
            Bundle().apply { putLong(SLEEP_TIMER_DURATION_MS, durationMs) },
        )
    }

    fun cancelSleepTimer() {
        val ctrl = controller ?: return
        ctrl.sendCustomCommand(SessionCommand(ACTION_SLEEP_TIMER_CANCEL, Bundle.EMPTY), Bundle.EMPTY)
    }

    val showNowPlaying: StateFlow<Boolean> = savedStateHandle.getStateFlow("showNowPlaying", false)
    fun setShowNowPlaying(value: Boolean) {
        savedStateHandle["showNowPlaying"] = value
    }

    // Last selected bottom-navigation tab, restored on relaunch. Updated synchronously so
    // tab switches render immediately; the DataStore write catches up in the background.
    private val _selectedHomeTab = MutableStateFlow(0)
    val selectedHomeTab: StateFlow<Int> = _selectedHomeTab.asStateFlow()

    fun setSelectedHomeTab(tab: Int) {
        _selectedHomeTab.value = tab
        if (tab != 1) _activeFavoritesOrder.value = null
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[LAST_HOME_TAB_KEY] = tab }
        }
    }

    val homeViewMode: StateFlow<HomeViewMode> = dataStore.data
        .map { prefs -> if (prefs[HOME_CARDS_VIEW_KEY] == false) HomeViewMode.List else HomeViewMode.Cards }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeViewMode.Cards)

    val showStreamBitrate: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[SHOW_STREAM_BITRATE_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val showHome: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[SHOW_HOME_KEY] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)


    fun setHomeViewMode(mode: HomeViewMode) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[HOME_CARDS_VIEW_KEY] = mode == HomeViewMode.Cards
            }
        }
    }

    val registrySearchResults: StateFlow<List<RegistryStation>> = searchStateHolder.registryResults
    val favoriteSearchResults: StateFlow<List<Station>> = searchStateHolder.favoriteResults
    val selectedCountries: StateFlow<Set<String>> = searchStateHolder.selectedCountries
    val selectedTags: StateFlow<Set<String>> = searchStateHolder.selectedTags

    private val _availableCountries = MutableStateFlow<List<String>>(emptyList())
    val availableCountries: StateFlow<List<String>> = _availableCountries.asStateFlow()

    fun searchRegistry(query: String) {
        searchStateHolder.search(query)
    }

    fun toggleCountryFilter(country: String) {
        searchStateHolder.toggleCountry(country)
    }

    fun setCountryFilter(country: String) {
        searchStateHolder.setCountry(country)
    }

    fun toggleTagFilter(tag: String) {
        searchStateHolder.toggleTag(tag)
    }

    fun clearCountryFilter() {
        searchStateHolder.clearCountries()
    }

    fun clearTagFilter() {
        searchStateHolder.clearTags()
    }

    fun clearAllFilters() {
        searchStateHolder.clearFilters()
    }

    fun playRandomFromMood(tags: List<String>) {
        viewModelScope.launch {
            val station = withContext(Dispatchers.IO) {
                tags.shuffled().firstNotNullOfOrNull { tag ->
                    registryRepository.randomByCategory(tag.lowercase())
                }
            } ?: return@launch
            playFromRegistry(station)
        }
    }

    fun playFromRegistry(registryStation: RegistryStation, queue: List<RegistryStation> = emptyList()) {
        play(registryStation.toEphemeralStation(), queue.map { it.toEphemeralStation() })
    }

    fun addFromRegistry(registryStation: RegistryStation) {
        viewModelScope.launch {
            val localLogoPath = if (registryStation.logoUrl.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    val dir = File(getApplication<Application>().filesDir, "logos").also { it.mkdirs() }
                    artworkLoader.download(registryStation.logoUrl, dir)
                } ?: registryStation.logoUrl
            } else {
                ""
            }
            if (!localLogoPath.startsWith("http")) {
                withContext(Dispatchers.IO) {
                    ensureMediaArtworkForLogo(getApplication(), File(localLogoPath))
                }
            }
            val stationId = repository.insertOrGetExisting(
                Station(
                    name = registryStation.name,
                    streamUrl = registryStation.streamUrl,
                    logoPath = localLogoPath,
                    isFavorite = true,
                    provider = registryStation.provider,
                    providerId = registryStation.providerId,
                    tags = registryStation.tags,
                    description = registryStation.description,
                    country = registryStation.country,
                    countryCode = registryStation.countryCode,
                ),
            )
            _recentlyAddedStationId.value = stationId
        }
    }

    fun removeFromRegistry(registryStation: RegistryStation) {
        viewModelScope.launch {
            val station = repository.findMatching(registryStation) ?: return@launch
            deleteStationRecord(station)
        }
    }

    private val _recentlyAddedStationId = MutableStateFlow<Long?>(null)
    val recentlyAddedStationId: StateFlow<Long?> = _recentlyAddedStationId.asStateFlow()

    val recentSearches: StateFlow<List<String>> = searchStateHolder.recentSearches

    private val playbackScreenUiState = combine(
        playbackUiState,
        nowPlayingDisplay,
        sleepTimer,
        showNowPlaying,
        recentlyAddedStationId,
    ) { playback, display, timer, showNowPlaying, recentlyAddedStationId ->
        PlaybackScreenUiState(playback, display, timer, showNowPlaying, recentlyAddedStationId)
    }

    private val homeDiscoveryUiState = combine(
        featuredStations,
        forYouStations,
        recentlyPlayedStations,
        defaultStations,
        curatedMoodStations,
    ) { featured, forYou, recentlyPlayed, defaults, moods ->
        HomeDiscoveryUiState(featured, forYou, recentlyPlayed, defaults, moods)
    }

    private val homePreferencesUiState = combine(
        homeViewMode,
        favoritesSort,
        showStreamBitrate,
        showHome,
    ) { viewMode, sort, showBitrate, showHome ->
        HomePreferencesUiState(viewMode, sort, showBitrate, showHome)
    }

    private val homeUiState = combine(
        stations,
        homeDiscoveryUiState,
        homePreferencesUiState,
        selectedHomeTab,
        isOnline,
    ) { stations, discovery, preferences, selectedTab, isOnline ->
        HomeUiState(stations, discovery, preferences, selectedTab, isOnline)
    }

    private val searchResultsUiState = combine(
        registrySearchResults,
        favoriteSearchResults,
        recentSearches,
    ) { registry, favorites, recent -> SearchResultsUiState(registry, favorites, recent) }

    private val searchFiltersUiState = combine(
        allTags,
        selectedCountries,
        selectedTags,
        availableCountries,
    ) { tags, countries, selectedTags, availableCountries ->
        SearchFiltersUiState(tags, countries, selectedTags, availableCountries)
    }

    private val searchUiState = combine(searchResultsUiState, searchFiltersUiState) { results, filters ->
        SearchUiState(results, filters)
    }

    val mainUiState: StateFlow<MainUiState> = combine(
        playbackScreenUiState,
        homeUiState,
        searchUiState,
    ) { playback, home, search -> MainUiState(playback, home, search) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun saveRecentSearch(query: String) {
        searchStateHolder.saveRecentSearch(query)
    }

    fun removeRecentSearch(query: String) {
        searchStateHolder.removeRecentSearch(query)
    }

    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    fun connect(context: Context) {
        if (controllerFuture != null) return
        val appContext = context.applicationContext
        val token = SessionToken(appContext, ComponentName(appContext, PlayerService::class.java))
        controllerFuture = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
            controller?.currentMediaItem?.mediaId?.toLongOrNull()?.let { id ->
                val station = _allStations.value.firstOrNull { it.id == id }
                if (station != null) {
                    syncPlaybackState(controller, station)
                    controller?.mediaMetadata?.let { metadata ->
                        handlePlaybackMetadata(
                            mediaItem = controller?.currentMediaItem,
                            title = metadata.title?.toString(),
                            artist = metadata.artist?.toString(),
                        )
                    }
                } else if (_currentStationId.value == null) {
                    syncPlaybackState(controller)
                }
            }
            if (controller?.currentMediaItem == null) {
                viewModelScope.launch {
                    pendingRestoreStation.await()?.let { loadStationPaused(it) }
                }
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            if (station.id == 0L) {
                // Ephemeral station (played without saving) — persist it as a favourite.
                // Set currentStationId first, then wait for _allStations to contain the new
                // row before clearing ephemeral, so currentStation never drops to null.
                val id = repository.saveAsFavorite(ensureLocalLogo(station))
                _currentStationId.value = id
                _allStations.first { list -> list.any { it.id == id } }
                setCurrentStation(repository.getById(id) ?: station.copy(id = id, isFavorite = true))
                return@launch
            }
            if (!station.isFavorite) {
                // Saved but unflagged (e.g. imported): favouriting sets the flag and, if
                // the logo is still a remote URL, downloads it so it survives backups.
                repository.update(ensureLocalLogo(station).copy(isFavorite = true))
                return@launch
            }
            // Unfavouriting removes the saved row — home and search both treat row
            // existence as "favourited". If it's the active station, hand it off to the
            // ephemeral slot first so playback and the Now Playing UI carry on, and keep
            // its logo files so re-favouriting restores the same artwork.
            val isCurrent = _currentStationId.value == station.id
            if (isCurrent) {
                setCurrentStation(station.copy(id = 0, isFavorite = false))
            } else {
                clearLastPlayedStationIfMatching(station)
            }
            repository.delete(station)
            if (!isCurrent) {
                withContext(Dispatchers.IO) { deleteLogoFiles(station.logoPath) }
            }
        }
    }

    fun restoreFavorite(station: Station) {
        viewModelScope.launch {
            val id = repository.saveAsFavorite(station.copy(isFavorite = true))
            if (_ephemeralStation.value?.streamUrl == station.streamUrl) {
                _currentStationId.value = id
                _ephemeralStation.value = null
            }
        }
    }

    private val playerListener = object : Player.Listener {
        // Playback can start or change from outside this ViewModel — Android Auto, voice
        // search, a queue skip — so the phone's current-station state must follow the
        // session's media item, not just this ViewModel's own play() calls. When play() did
        // initiate the change the state already matches and the guards below make this a
        // no-op.
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                )
            ) {
                // onEvents runs after the individual callbacks in the batch. Reading the Player
                // here gives the UI one coherent station/playback snapshot even while lifecycle-
                // aware Compose collection is stopped during screen sleep.
                syncPlaybackState(player)
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            _playbackUiState.value = _playbackUiState.value.copy(
                isPlaying = false,
                isBuffering = false,
            )
            _playbackUiState.value = _playbackUiState.value.copy(
                error = getApplication<Application>().getString(playbackErrorMessageRes(error.errorCode)),
            )
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            handlePlaybackMetadata(
                mediaItem = controller?.currentMediaItem,
                title = mediaMetadata.title?.toString(),
                artist = mediaMetadata.artist?.toString(),
            )
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateCurrentBitrate(tracks)
        }
    }

    @androidx.annotation.VisibleForTesting
    internal fun handlePlaybackEvents(
        mediaItem: MediaItem?,
        isPlaying: Boolean,
        playbackState: Int = Player.STATE_READY,
        playWhenReady: Boolean = isPlaying,
        queue: List<Station> = emptyList(),
    ) {
        syncPlaybackState(mediaItem, isPlaying, playbackState, playWhenReady, queue = queue)
    }

    @androidx.annotation.VisibleForTesting
    internal fun handlePlaybackMetadata(title: String?, artist: String?) {
        applyPlaybackMetadata(title, artist)
    }

    @androidx.annotation.VisibleForTesting
    internal fun handlePlaybackMetadata(mediaItem: MediaItem?, title: String?, artist: String?) {
        val station = resolveStation(mediaItem)
        val currentStation = _playbackUiState.value.station
        if (station != null && (currentStation == null || !currentStation.matches(station))) {
            pendingPlaybackMetadata = PendingPlaybackMetadata(station, title, artist)
        } else {
            pendingPlaybackMetadata = null
            applyPlaybackMetadata(title, artist)
        }
    }

    private fun applyPlaybackMetadata(title: String?, artist: String?) {
        val parsed = parseTrackMetadata(title, artist)
        val normalizedTitle = parsed.title?.takeIf { it != liveRadio() && !isStationName(it) }
        val normalizedArtist = parsed.artist?.takeIf { it != liveRadio() && !isStationName(it) }
        _playbackUiState.value = _playbackUiState.value.copy(
            trackTitle = normalizedTitle ?: normalizedArtist,
            trackArtist = normalizedArtist,
        )
    }

    private fun isStationName(value: String): Boolean {
        val playback = _playbackUiState.value
        return playback.station?.name?.equals(value, ignoreCase = true) == true ||
            playback.queue.any { it.name.equals(value, ignoreCase = true) } ||
            _allStations.value.any { it.name.equals(value, ignoreCase = true) }
    }

    private fun syncPlaybackState(player: Player?, resolvedStation: Station? = null) {
        if (player == null) return
        syncPlaybackState(
            mediaItem = player.currentMediaItem,
            isPlaying = player.isPlaying,
            playbackState = player.playbackState,
            playWhenReady = player.playWhenReady,
            resolvedStation = resolvedStation,
            queue = (0 until player.mediaItemCount)
                .mapNotNull { index -> resolveStation(player.getMediaItemAt(index)) },
        )
        updateCurrentBitrate(player.currentTracks)
    }

    private fun syncPlaybackState(
        mediaItem: MediaItem?,
        isPlaying: Boolean,
        playbackState: Int,
        playWhenReady: Boolean,
        resolvedStation: Station? = null,
        queue: List<Station> = emptyList(),
    ) {
        val station = resolvedStation ?: resolveStation(mediaItem)
        if (queue.isNotEmpty()) {
            _activeFavoritesOrder.value = favoritesOrder(queue)
        }
        if (station != null) {
            val changed = stationChanged(station)
            updateStationIdentity(station)
            clearPerStationStateIfChanged(changed)
            pendingPlaybackMetadata
                ?.takeIf { it.station.matches(station) }
                ?.let { pending ->
                    applyPlaybackMetadata(pending.title, pending.artist)
                    pendingPlaybackMetadata = null
                }
            if (!suppressLastPlayedPersist) {
                persistLastPlayedStation(station, queue)
            }
        }
        _playbackUiState.value = _playbackUiState.value.copy(
            station = station ?: _playbackUiState.value.station,
            isPlaying = isPlaying,
            isBuffering = playbackState == Player.STATE_BUFFERING && playWhenReady,
            queue = queue.ifEmpty { _playbackUiState.value.queue },
        )
    }

    private fun resolveStation(mediaItem: MediaItem?): Station? {
        val item = mediaItem ?: return null
        val extras = item.mediaMetadata.extras
        val streamUrl = extras?.getString("streamUrl").orEmpty()
        val provider = extras?.getString("provider").orEmpty()
        val providerId = extras?.getString("providerId").orEmpty()
        // Resolve to a saved row the same way PlayerService.stationForMediaItem does:
        // numeric mediaId first, then provider identity, then stream URL.
        val saved = item.mediaId.toLongOrNull()
            ?.let { id -> _allStations.value.firstOrNull { it.id == id } }
            ?: _allStations.value.firstOrNull {
                provider.isNotBlank() && providerId.isNotBlank() &&
                    it.provider == provider && it.providerId == providerId
            }
            ?: _allStations.value.firstOrNull { streamUrl.isNotBlank() && it.streamUrl == streamUrl }
        return saved ?: streamUrl.takeIf { it.isNotBlank() }?.let {
            Station(
                name = stationNameFromMediaMetadata(
                    item.mediaMetadata.extras?.getString("stationName"),
                    item.mediaMetadata.title,
                ),
                streamUrl = streamUrl,
                logoPath = extras?.getString("logoPath").orEmpty(),
                provider = provider,
                providerId = providerId,
            )
        }
    }

    private fun setCurrentStation(station: Station?) {
        val changed = stationChanged(station)
        updateStationIdentity(station)
        _playbackUiState.value = _playbackUiState.value.copy(station = station)
        clearPerStationStateIfChanged(changed)
    }

    private fun updateStationIdentity(station: Station?) {
        if (station == null) {
            _currentStationId.value = null
            _ephemeralStation.value = null
        } else if (station.id == 0L) {
            _currentStationId.value = null
            _ephemeralStation.value = station
        } else {
            _currentStationId.value = station.id
            _ephemeralStation.value = null
        }
    }

    private fun stationChanged(station: Station?): Boolean {
        val previous = _playbackUiState.value.station
        return when {
            previous == null -> station != null
            station == null -> true
            else -> !previous.matches(station)
        }
    }

    private fun clearPerStationStateIfChanged(changed: Boolean) {
        if (!changed) return
        // Per-track state belongs to the previous station; onMediaMetadataChanged
        // repopulates it for the new one.
        _playbackUiState.value = _playbackUiState.value.copy(
            trackTitle = null,
            trackArtist = null,
            bitrateKbps = null,
            error = null,
        )
    }

    private fun refreshCurrentStation(station: Station) {
        if (_playbackUiState.value.station?.id == station.id) {
            _playbackUiState.value = _playbackUiState.value.copy(station = station)
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun updateCurrentBitrate(tracks: Tracks) {
        val bitrateKbps = tracks.getGroups()
            .asSequence()
            .filter { group -> group.type == C.TRACK_TYPE_AUDIO && group.isSelected }
            .flatMap { group ->
                (0 until group.length).asSequence()
                    .filter { index -> group.isTrackSelected(index) }
                    .map { index -> group.getTrackFormat(index) }
            }
            .mapNotNull { format -> format.bitrate.takeIf { it != Format.NO_VALUE && it > 0 } }
            .firstOrNull()
            ?.let { bitrate -> (bitrate / 1_000).coerceAtLeast(1) }
        _playbackUiState.value = _playbackUiState.value.copy(bitrateKbps = bitrateKbps)
    }

    // queue is the ordered list station is part of in whatever screen triggered playback
    // (favourites in the user's sort order, or an active mood's stations) — the same list the
    // Now Playing pager swipes through. Feeding it to the session as a real multi-item queue
    // (rather than a single MediaItem) is what makes hardware/Bluetooth media-button
    // next/previous work: Media3's default session callback already handles those by seeking
    // within the player's timeline, it just needs a timeline with neighbours to seek to.
    fun play(station: Station, queue: List<Station> = emptyList()) {
        // Use the recovered station instance for every playback surface. This preserves a
        // user-edited logo while supplying the registry fallback for imported rows whose old
        // local artwork path no longer exists (including the mini-player).
        val recoveredStations = _allStations.value
        val playbackStation = recoveredStations.firstOrNull { it.matches(station) } ?: station
        val playbackQueue = queue.map { queued ->
            recoveredStations.firstOrNull { it.matches(queued) } ?: queued
        }
        _activeFavoritesOrder.value = favoritesOrder(playbackQueue)
        setCurrentStation(playbackStation)
        _playbackUiState.value = _playbackUiState.value.copy(
            queue = playbackQueue.takeIf { resolveQueueStart(it, playbackStation) != null }
                ?: listOf(playbackStation),
        )
        persistLastPlayedStation(playbackStation, playbackQueue)
        val startIndex = resolveQueueStart(playbackQueue, playbackStation)
        controller?.let { mediaController ->
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    if (startIndex != null) {
                        val mediaItems = coroutineScope {
                            playbackQueue.map { station ->
                                async { station.toSystemPlayableMediaItem(getApplication()) }
                            }.awaitAll()
                        }
                        withContext(Dispatchers.Main.immediate) {
                            mediaController.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                        }
                    } else {
                        val mediaItem = playbackStation.toSystemPlayableMediaItem(getApplication())
                        withContext(Dispatchers.Main.immediate) {
                            mediaController.setMediaItem(mediaItem)
                        }
                    }
                }
                mediaController.prepare()
                mediaController.play()
            }
        }
    }

    fun togglePlayback() {
        controller?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                _playbackUiState.value = _playbackUiState.value.copy(error = null)
                it.play()
            }
        }
    }

    // True while stopAndClear() is unwinding playback, so the isPlaying listener below doesn't
    // re-persist the station we're in the middle of forgetting.
    private var suppressLastPlayedPersist = false

    // Swiping the mini player away: stop playback, clear the current/ephemeral station, and
    // forget it so app restart doesn't resume it. Clearing the controller's media items (rather
    // than just stop()) also drops the media notification and its Quick Settings / lock screen
    // media card, since Media3 only shows those while a current media item exists. A fresh
    // play() afterwards goes through the same path as starting a station with no player active.
    fun stopAndClear() {
        suppressLastPlayedPersist = true
        _activeFavoritesOrder.value = null
        controller?.apply {
            stop()
            clearMediaItems()
        }
        setCurrentStation(null)
        _playbackUiState.value = PlaybackUiState()
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs.remove(LAST_PLAYED_STATION_KEY) }
            suppressLastPlayedPersist = false
        }
    }

    fun clearRecentlyAddedStation(stationId: Long) {
        if (_recentlyAddedStationId.value == stationId) {
            _recentlyAddedStationId.value = null
        }
    }

    // Favouriting must leave the logo cached under filesDir/logos so it survives backup/
    // restore — a bare remote URL isn't embedded in the backup zip (see SettingsViewModel).
    private suspend fun ensureLocalLogo(station: Station): Station {
        if (!station.logoPath.startsWith("http")) return station
        val localPath = withContext(Dispatchers.IO) {
            val dir = File(getApplication<Application>().filesDir, "logos").also { it.mkdirs() }
            artworkLoader.download(station.logoPath, dir)
        } ?: return station
        return station.copy(logoPath = localPath)
    }

    fun deleteStation(station: Station) {
        viewModelScope.launch {
            deleteStationRecord(station)
        }
    }

    private suspend fun deleteStationRecord(station: Station) {
        if (_currentStationId.value == station.id) {
            controller?.stop()
            setCurrentStation(null)
        }
        if (_ephemeralStation.value?.streamUrl == station.streamUrl) {
            setCurrentStation(null)
        }
        clearLastPlayedStationIfMatching(station)
        repository.delete(station)
        withContext(Dispatchers.IO) { deleteLogoFiles(station.logoPath) }
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    private suspend fun restoreLastPlayedStation() {
        val snapshot = dataStore.data.first()[LAST_PLAYED_STATION_KEY]?.let(::lastPlayedStationSnapshot)
        if (snapshot == null) {
            pendingRestoreStation.complete(null)
            return
        }
        val savedStation = when {
            snapshot.station.id > 0 -> repository.getById(snapshot.station.id)
            else -> null
        } ?: repository.getByStreamUrl(snapshot.station.streamUrl)

        if (savedStation != null) {
            setCurrentStation(savedStation)
        } else {
            setCurrentStation(snapshot.station.toEphemeral())
        }
        _playbackUiState.value = _playbackUiState.value.copy(queue = snapshot.queue)

        pendingRestoreStation.complete(savedStation ?: snapshot.station.toEphemeral())
    }

    private fun persistLastPlayedStation(station: Station, queue: List<Station> = emptyList()) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                val existingQueue = prefs[LAST_PLAYED_STATION_KEY]
                    ?.let(::lastPlayedStationSnapshot)
                    ?.queue
                    .orEmpty()
                prefs[LAST_PLAYED_STATION_KEY] = station
                    .toLastPlayedJson(queue.ifEmpty { existingQueue })
                    .toString()
            }
        }
    }

    private fun clearLastPlayedStationIfMatching(station: Station) {
        viewModelScope.launch {
            val snapshot = dataStore.data.first()[LAST_PLAYED_STATION_KEY]?.let(::lastPlayedStationSnapshot) ?: return@launch
            if (snapshot.station.id == station.id || snapshot.station.streamUrl == station.streamUrl) {
                dataStore.edit { prefs -> prefs.remove(LAST_PLAYED_STATION_KEY) }
            }
        }
    }

    private suspend fun loadStationPaused(station: Station) {
        // Prefer the exact timeline captured by the Media3 session. Legacy snapshots did not
        // include a queue, so only those fall back to rebuilding from the database and sort.
        val snapshot = dataStore.data.first()[LAST_PLAYED_STATION_KEY]
            ?.let(::lastPlayedStationSnapshot)
        val storedQueue = snapshot?.queue?.takeIf { it.size > 1 }
            ?: sortStations(repository.getAll().first(), _favoritesSort.value)
        // Backups can contain paths to the old app-private logo directory. Resolve every
        // restored entry through the registry before constructing Media3 items; otherwise the
        // missing local path produces the fallback app icon in system controls.
        val queue = storedQueue.map { recoverStationArtwork(it) }
        val restoredStation = recoverStationArtwork(station)
        val startIndex = resolveQueueStart(queue, restoredStation)

        controller?.let { mediaController ->
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    if (startIndex != null) {
                        val mediaItems = coroutineScope {
                            queue.map { stationEntry ->
                                async { stationEntry.toSystemPlayableMediaItem(getApplication()) }
                            }.awaitAll()
                        }
                        withContext(Dispatchers.Main.immediate) {
                            mediaController.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                        }
                    } else {
                        val mediaItem = restoredStation.toSystemPlayableMediaItem(getApplication())
                        withContext(Dispatchers.Main.immediate) {
                            mediaController.setMediaItem(mediaItem)
                        }
                    }
                }
                mediaController.prepare()
                mediaController.pause()
            }
        }
    }

    init {
        // Start collectors only after every StateFlow they touch has been initialized.
        // This matters on a real Android main looper, where launch can run immediately.
        initialize()
    }
}

private fun Station.toEphemeral(): Station = copy(id = 0)

private fun deleteLogoFiles(logoPath: String) {
    if (logoPath.isBlank() || logoPath.startsWith("http")) return
    val file = java.io.File(logoPath)
    file.delete()
    java.io.File(file.parentFile, "${file.nameWithoutExtension}_media.png").delete()
}

internal fun playbackErrorMessageRes(errorCode: Int): Int =
    when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_TIMEOUT,
        -> R.string.playback_connection_failed
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> R.string.playback_format_unsupported
        else -> R.string.playback_failed
    }
