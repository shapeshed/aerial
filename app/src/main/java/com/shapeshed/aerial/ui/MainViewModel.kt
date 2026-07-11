package com.shapeshed.aerial.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
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
import com.shapeshed.aerial.FAVORITES_GRID_COLUMNS_DEFAULT
import com.shapeshed.aerial.FAVORITES_GRID_COLUMNS_KEY
import com.shapeshed.aerial.FAVORITES_GRID_COLUMNS_RANGE
import com.shapeshed.aerial.SHOW_STREAM_BITRATE_KEY
import com.shapeshed.aerial.data.ACTION_SLEEP_TIMER_CANCEL
import com.shapeshed.aerial.data.ACTION_SLEEP_TIMER_SET
import com.shapeshed.aerial.data.NowPlayingInfo
import com.shapeshed.aerial.data.NowPlayingStore
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.SLEEP_TIMER_DURATION_MS
import com.shapeshed.aerial.data.SleepTimerState
import com.shapeshed.aerial.data.SleepTimerStore
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.data.bauerStreamUrl
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
private val SEARCH_COUNTRIES_KEY = stringPreferencesKey("search_countries")
private val SEARCH_TAGS_KEY = stringPreferencesKey("search_tags")
private val LAST_PLAYED_STATION_KEY = stringPreferencesKey("last_played_station")
private val HOME_CARDS_VIEW_KEY = booleanPreferencesKey("home_cards_view")
private val LAST_HOME_TAB_KEY = intPreferencesKey("last_home_tab")
private val FAVORITES_SORT_KEY = stringPreferencesKey("favorites_sort")

enum class FavoritesSort {
    AZ,
    LAST_PLAYED,
    MOST_PLAYED,
}
private const val MAX_RECENT_SEARCHES = 5

private data class LastPlayedStationSnapshot(
    val station: Station,
)

class MainViewModel(
    application: Application,
    private val repository: StationRepository,
    private val registryRepository: RegistryRepository,
    private val dataStore: DataStore<Preferences>,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : AndroidViewModel(application) {

    val isOnline = (application as AerialApp).networkMonitor.isOnline

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

    init {
        viewModelScope.launch {
            repository.getAll().first()
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
            _selectedCountries.value = prefs[SEARCH_COUNTRIES_KEY]
                ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            _selectedTags.value = prefs[SEARCH_TAGS_KEY]
                ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            _selectedHomeTab.value = prefs[LAST_HOME_TAB_KEY] ?: 0
            _favoritesSort.value = prefs[FAVORITES_SORT_KEY]
                ?.let { saved -> FavoritesSort.entries.firstOrNull { it.name == saved } }
                ?: FavoritesSort.AZ
        }
    }

    private val _allStations: StateFlow<List<Station>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Sort order for the Favourites tab. Updated synchronously so the list reorders
    // immediately; the DataStore write catches up in the background.
    private val _favoritesSort = MutableStateFlow(FavoritesSort.AZ)
    val favoritesSort: StateFlow<FavoritesSort> = _favoritesSort.asStateFlow()

    fun setFavoritesSort(sort: FavoritesSort) {
        _favoritesSort.value = sort
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[FAVORITES_SORT_KEY] = sort.name }
        }
    }

    val stations: StateFlow<List<Station>> = combine(_allStations, _favoritesSort) { list, sort ->
        when (sort) {
            FavoritesSort.AZ -> list.sortedWith(compareBy { stationSortKey(it.name) })
            FavoritesSort.LAST_PLAYED -> list.sortedWith(
                compareByDescending<Station> { it.lastPlayedAt }.thenBy { stationSortKey(it.name) },
            )
            FavoritesSort.MOST_PLAYED -> list.sortedWith(
                compareByDescending<Station> { it.playCount }.thenBy { stationSortKey(it.name) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val appIconArtwork: ByteArray? by lazy { appIconBitmap(getApplication()) }

    private val _currentStationId = MutableStateFlow<Long?>(null)
    private val _ephemeralStation = MutableStateFlow<Station?>(null)
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
                        _ephemeralStation.value = removed.copy(id = 0, isFavorite = false)
                        _currentStationId.value = null
                    }
                }
                previous = list
            }
        }
    }

    val currentStation: StateFlow<Station?> = combine(
        _allStations,
        _currentStationId,
        _ephemeralStation,
    ) { list, id, ephemeral ->
        ephemeral ?: id?.let { i -> list.firstOrNull { s -> s.id == i } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow<String?>(null)
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle.asStateFlow()

    private val _currentTrackArtist = MutableStateFlow<String?>(null)
    val currentTrackArtist: StateFlow<String?> = _currentTrackArtist.asStateFlow()

    private val _currentTrackArtworkUrl = MutableStateFlow<String?>(null)
    val currentTrackArtworkUrl: StateFlow<String?> = _currentTrackArtworkUrl.asStateFlow()

    private val _currentTrackArtworkData = MutableStateFlow<ByteArray?>(null)
    val currentTrackArtworkData: StateFlow<ByteArray?> = _currentTrackArtworkData.asStateFlow()

    private val _currentBitrateKbps = MutableStateFlow<Int?>(null)
    val currentBitrateKbps: StateFlow<Int?> = _currentBitrateKbps.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    val nowPlayingInfo: StateFlow<NowPlayingInfo?> = NowPlayingStore.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Single derived "what's playing" summary. Recomputes whenever any source flow emits
    // (enricher info, ICY media metadata, or the current station change) so the UI never has
    // to reconcile the sources itself.
    val nowPlayingDisplay: StateFlow<NowPlayingDisplay> = combine(
        currentStation,
        nowPlayingInfo,
        _currentTrackTitle,
        _currentTrackArtist,
    ) { station, info, icyTitle, icyArtist ->
        val activeInfo = info?.takeIf { it.stationId == station?.id }
        computeNowPlayingDisplay(station?.name.orEmpty(), activeInfo, icyTitle, icyArtist, liveRadio())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingDisplay("", ""))

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

    val favoritesGridColumns: StateFlow<Int> = dataStore.data
        .map { prefs ->
            (prefs[FAVORITES_GRID_COLUMNS_KEY] ?: FAVORITES_GRID_COLUMNS_DEFAULT)
                .coerceIn(FAVORITES_GRID_COLUMNS_RANGE)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FAVORITES_GRID_COLUMNS_DEFAULT)

    fun setHomeViewMode(mode: HomeViewMode) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[HOME_CARDS_VIEW_KEY] = mode == HomeViewMode.Cards
            }
        }
    }

    private val _registrySearchResults = MutableStateFlow<List<RegistryStation>>(emptyList())
    val registrySearchResults: StateFlow<List<RegistryStation>> = _registrySearchResults.asStateFlow()

    private val _favoriteSearchResults = MutableStateFlow<List<Station>>(emptyList())
    val favoriteSearchResults: StateFlow<List<Station>> = _favoriteSearchResults.asStateFlow()

    private val _selectedCountries = MutableStateFlow<Set<String>>(emptySet())
    val selectedCountries: StateFlow<Set<String>> = _selectedCountries.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()



    private val _availableCountries = MutableStateFlow<List<String>>(emptyList())
    val availableCountries: StateFlow<List<String>> = _availableCountries.asStateFlow()

    private var _lastSearchQuery = ""
    private var searchJob: Job? = null

    fun searchRegistry(query: String) {
        _lastSearchQuery = query
        runSearch(query)
    }

    fun toggleCountryFilter(country: String) {
        _selectedCountries.value = _selectedCountries.value.let {
            if (it.contains(country)) it - country else it + country
        }
        persistFilters()
        runSearch(_lastSearchQuery)
    }

    fun setCountryFilter(country: String) {
        _selectedCountries.value = setOf(country)
        persistFilters()
        runSearch(_lastSearchQuery)
    }

    fun toggleTagFilter(tag: String) {
        _selectedTags.value = _selectedTags.value.let {
            if (it.contains(tag)) it - tag else it + tag
        }
        persistFilters()
        runSearch(_lastSearchQuery)
    }

    fun clearCountryFilter() {
        _selectedCountries.value = emptySet()
        persistFilters()
        runSearch(_lastSearchQuery)
    }

    fun clearTagFilter() {
        _selectedTags.value = emptySet()
        persistFilters()
        runSearch(_lastSearchQuery)
    }

    private fun persistFilters() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[SEARCH_COUNTRIES_KEY] = _selectedCountries.value.joinToString(",")
                prefs[SEARCH_TAGS_KEY] = _selectedTags.value.joinToString(",")
            }
        }
    }

    private fun runSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _favoriteSearchResults.value = if (query.isBlank()) {
                emptyList()
            } else {
                repository.searchFavorites(query)
            }
            _registrySearchResults.value = registryRepository.search(
                query = query,
                countryCodes = _selectedCountries.value,
                tags = _selectedTags.value,
            )
        }
    }

    fun clearAllFilters() {
        _selectedCountries.value = emptySet()
        _selectedTags.value = emptySet()
        persistFilters()
        runSearch(_lastSearchQuery)
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

    fun playFromRegistry(registryStation: RegistryStation) {
        val station = Station(
            name = registryStation.name,
            streamUrl = registryStation.streamUrl,
            logoPath = registryStation.logoUrl,
            provider = registryStation.provider,
            providerId = registryStation.providerId,
            tags = registryStation.tags,
            description = registryStation.description,
            country = registryStation.country,
            countryCode = registryStation.countryCode,
        )
        play(station)
    }

    fun addFromRegistry(registryStation: RegistryStation) {
        viewModelScope.launch {
            val localLogoPath = if (registryStation.logoUrl.isNotBlank()) {
                withContext(Dispatchers.IO) { downloadLogo(registryStation.logoUrl) } ?: registryStation.logoUrl
            } else {
                ""
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
            val station = withContext(Dispatchers.IO) {
                repository.findMatching(registryStation)
            } ?: return@launch
            deleteStationRecord(station)
        }
    }

    private val _recentlyAddedStationId = MutableStateFlow<Long?>(null)
    val recentlyAddedStationId: StateFlow<Long?> = _recentlyAddedStationId.asStateFlow()

    val recentSearches: StateFlow<List<String>> = dataStore.data
        .map { prefs ->
            prefs[RECENT_SEARCHES_KEY]?.let { json ->
                try {
                    val arr = JSONArray(json)
                    (0 until arr.length()).map { arr.getString(it) }
                } catch (_: Exception) { emptyList() }
            } ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            dataStore.edit { prefs ->
                val current = prefs.recentSearches()
                current.remove(trimmed)
                current.add(0, trimmed)
                prefs[RECENT_SEARCHES_KEY] = JSONArray(current.take(MAX_RECENT_SEARCHES)).toString()
            }
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                val current = prefs.recentSearches()
                current.remove(query)
                prefs[RECENT_SEARCHES_KEY] = JSONArray(current).toString()
            }
        }
    }

    private fun Preferences.recentSearches(): MutableList<String> =
        get(RECENT_SEARCHES_KEY)?.let { json ->
            try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }.toMutableList()
            } catch (_: Exception) { mutableListOf() }
        } ?: mutableListOf()

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
                    _currentStationId.value = station.id
                    if (_ephemeralStation.value?.streamUrl == station.streamUrl) {
                        _ephemeralStation.value = null
                    }
                    _isPlaying.value = controller?.isPlaying ?: false
                    updateCurrentBitrate(controller?.currentTracks ?: Tracks.EMPTY)
                    controller?.mediaMetadata?.artist?.toString()?.trim()
                        ?.takeIf { it.isNotEmpty() && it != liveRadio() }
                        ?.let { _currentTrackTitle.value = it }
                } else if (_currentStationId.value == null) {
                    _isPlaying.value = controller?.isPlaying ?: false
                    updateCurrentBitrate(controller?.currentTracks ?: Tracks.EMPTY)
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
                val id = repository.saveAsFavorite(station)
                _currentStationId.value = id
                _allStations.first { list -> list.any { it.id == id } }
                _ephemeralStation.value = null
                return@launch
            }
            if (!station.isFavorite) {
                // Saved but unflagged (e.g. imported): favouriting just sets the flag.
                repository.update(station.copy(isFavorite = true))
                return@launch
            }
            // Unfavouriting removes the saved row — home and search both treat row
            // existence as "favourited". If it's the active station, hand it off to the
            // ephemeral slot first so playback and the Now Playing UI carry on, and keep
            // its logo files so re-favouriting restores the same artwork.
            val isCurrent = _currentStationId.value == station.id
            if (isCurrent) {
                _ephemeralStation.value = station.copy(id = 0, isFavorite = false)
                _currentStationId.value = null
            } else {
                clearLastPlayedStationIfMatching(station)
            }
            repository.delete(station)
            if (!isCurrent) {
                withContext(Dispatchers.IO) { deleteLogoFiles(station.logoPath) }
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (!suppressLastPlayedPersist) {
                currentStation.value?.let { station ->
                    persistLastPlayedStation(station)
                }
            }
        }
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                // Only show buffering when the user intends to play; suppress the transient
                // STATE_BUFFERING that fires during prepare() when restoring a paused station.
                // Read both off the player itself (settled as of this batch) rather than off
                // the individual onPlaybackStateChanged callback, whose isolated playWhenReady
                // read can still be stale mid-batch — e.g. the very first play() of a session,
                // where STATE_BUFFERING can be delivered before playWhenReady=true has
                // propagated, silently dropping the buffering spinner for that first play.
                _isBuffering.value = player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            _isBuffering.value = false
            _isPlaying.value = false
            _playbackError.value = error.userMessage()
        }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val title = mediaMetadata.title?.toString()?.trim()
            val artist = mediaMetadata.artist?.toString()?.trim()
            val artwork = mediaMetadata.artworkUri?.toString()?.trim()
            val artworkData = mediaMetadata.artworkData
            when {
                !title.isNullOrEmpty() && title != liveRadio() -> _currentTrackTitle.value = title
                !artist.isNullOrEmpty() && artist != liveRadio() -> _currentTrackTitle.value = artist
            }
            _currentTrackArtist.value = artist?.takeIf { it.isNotEmpty() && it != liveRadio() }
            _currentTrackArtworkData.value = artworkData
            _currentTrackArtworkUrl.value = if (artworkData == null && !artwork.isNullOrEmpty()) {
                artwork
            } else {
                null
            }
        }
        override fun onTracksChanged(tracks: Tracks) {
            updateCurrentBitrate(tracks)
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun updateCurrentBitrate(tracks: Tracks) {
        _currentBitrateKbps.value = tracks.getGroups()
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
    }

    fun play(station: Station) {
        if (station.id == 0L) {
            _ephemeralStation.value = station
            _currentStationId.value = null
        } else {
            _ephemeralStation.value = null
            _currentStationId.value = station.id
            viewModelScope.launch {
                repository.recordPlay(station.id, System.currentTimeMillis())
            }
        }
        _currentTrackTitle.value = null
        _currentTrackArtist.value = null
        _currentTrackArtworkUrl.value = null
        _currentTrackArtworkData.value = null
        _currentBitrateKbps.value = null
        _playbackError.value = null
        persistLastPlayedStation(station)
        val mediaItem = MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(bauerStreamUrl(station))
            .setMediaMetadata(stationMediaMetadata(station))
            .build()
        controller?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    fun togglePlayback() {
        controller?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                _playbackError.value = null
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
        controller?.apply {
            stop()
            clearMediaItems()
        }
        _currentStationId.value = null
        _ephemeralStation.value = null
        _currentTrackTitle.value = null
        _currentTrackArtist.value = null
        _currentTrackArtworkUrl.value = null
        _currentTrackArtworkData.value = null
        _currentBitrateKbps.value = null
        _playbackError.value = null
        _isPlaying.value = false
        _isBuffering.value = false
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

    private suspend fun downloadLogo(url: String): String? {
        return try {
            val dir = File(getApplication<Application>().filesDir, "logos").also { it.mkdirs() }
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                // A dead/moved logo URL can respond with a redirect or an HTML error page
                // instead of an image (e.g. a 301 whose body is just an nginx redirect
                // notice) — without this check that body gets saved to disk as if it were
                // the logo. On rejection the caller falls back to the raw URL, which Coil's
                // own HTTP client can still resolve correctly at render time (it isn't
                // restricted to same-protocol redirects the way HttpURLConnection is).
                val contentType = conn.contentType?.substringBefore(';')?.trim()
                if (conn.responseCode != HttpURLConnection.HTTP_OK ||
                    (contentType != null && !contentType.startsWith("image/"))
                ) {
                    return null
                }
                val dest = logoFileForUrl(url, dir, contentType)
                conn.inputStream.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                ensureMediaArtworkForLogo(getApplication(), dest)
                dest.absolutePath
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun deleteStation(station: Station) {
        viewModelScope.launch {
            deleteStationRecord(station)
        }
    }

    private suspend fun deleteStationRecord(station: Station) {
        if (_currentStationId.value == station.id) {
            controller?.stop()
            _currentStationId.value = null
        }
        if (_ephemeralStation.value?.streamUrl == station.streamUrl) {
            _ephemeralStation.value = null
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
            _currentStationId.value = savedStation.id
            _ephemeralStation.value = null
        } else {
            _ephemeralStation.value = snapshot.station.toEphemeral()
        }

        pendingRestoreStation.complete(savedStation ?: snapshot.station.toEphemeral())
    }

    private fun persistLastPlayedStation(station: Station) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[LAST_PLAYED_STATION_KEY] = station.toLastPlayedJson().toString()
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

    private fun loadStationPaused(station: Station) {
        val mediaItem = MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(bauerStreamUrl(station))
            .setMediaMetadata(stationMediaMetadata(station))
            .build()

        controller?.apply {
            setMediaItem(mediaItem)
            prepare()
            pause()
        }
    }

    private fun stationMediaMetadata(station: Station): MediaMetadata {
        val artworkUri = station.logoPath
            .takeIf { it.startsWith("http") }
            ?.let { Uri.parse(it) }
        val localArtworkData = station.logoPath
            .takeIf { it.isNotEmpty() && !it.startsWith("http") }
            ?.let { compressedLogoData(File(it)) }

        return MediaMetadata.Builder().apply {
            when {
                localArtworkData != null -> setArtworkData(
                    localArtworkData,
                    MediaMetadata.PICTURE_TYPE_FRONT_COVER,
                )
                artworkUri != null -> setArtworkUri(artworkUri)
                else -> appIconArtwork?.let {
                    setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            }
        }
            .setTitle(station.name)
            .setArtist(liveRadio())
            .setSubtitle(liveRadio())
            .setExtras(Bundle().apply {
                putString("provider", station.provider)
                putString("providerId", station.providerId)
                putString("streamUrl", station.streamUrl)
            })
            .build()
    }
}

/** Two-line "what's playing" summary for the mini player / notifications. */
data class NowPlayingDisplay(val title: String, val subtitle: String)

/**
 * Derives the display summary from the available metadata sources, in priority order:
 * track (song) → programme → ICY title → nothing. Pure and side-effect free so it can be
 * unit tested and reused; the ViewModel drives it from the event-fed flows and injects the
 * localized [liveRadio] label.
 */
fun computeNowPlayingDisplay(
    stationName: String,
    info: NowPlayingInfo?,
    icyTitle: String?,
    icyArtist: String? = null,
    liveRadio: String = "Live Radio",
): NowPlayingDisplay {
    val track = info?.track
    return when {
        track != null -> artistTitleDisplay(track.artist, track.title, stationName, liveRadio)
        info?.programmeTitle != null -> NowPlayingDisplay(
            title = info.programmeTitle,
            subtitle = info.programmeSubtitle?.takeIf { it.isNotBlank() } ?: stationName,
        )
        // No enricher active but the stream carries ICY/ID3 track text. ICY commonly parses to
        // "Artist - Title"; show both, the same as an enriched song.
        info == null && !icyTitle.isNullOrBlank() -> artistTitleDisplay(icyArtist.orEmpty(), icyTitle, stationName, liveRadio)
        else -> NowPlayingDisplay(stationName, liveRadio)
    }
}

// Artist on the top line, title below; falls back to the station name when one side is missing,
// ignoring an "artist" that is really just the station name (ICY with no separator).
private fun artistTitleDisplay(rawArtist: String, rawTitle: String, stationName: String, liveRadio: String): NowPlayingDisplay {
    val artist = rawArtist.takeIf { it.isNotBlank() && it != stationName }
    // Ignore a "title" that is really just the station name: with no track metadata the media
    // item's title is the station name, which would otherwise show as "Station - Station".
    val title = rawTitle.takeIf { it.isNotBlank() && it != stationName }
    return when {
        artist != null && title != null -> NowPlayingDisplay(artist, title)
        artist != null -> NowPlayingDisplay(artist, stationName)
        title != null -> NowPlayingDisplay(title, stationName)
        else -> NowPlayingDisplay(stationName, liveRadio)
    }
}

private fun Station.toEphemeral(): Station = copy(id = 0)

private fun Station.toLastPlayedJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("name", name)
        .put("streamUrl", streamUrl)
        .put("logoPath", logoPath)
        .put("isFavorite", isFavorite)
        .put("provider", provider)
        .put("providerId", providerId)
        .put("tags", tags)
        .put("description", description)
        .put("country", country)
        .put("countryCode", countryCode)

private fun lastPlayedStationSnapshot(json: String): LastPlayedStationSnapshot {
    val obj = JSONObject(json)
    return LastPlayedStationSnapshot(
        station = Station(
            id = obj.optLong("id"),
            name = obj.optString("name"),
            streamUrl = obj.optString("streamUrl"),
            logoPath = obj.optString("logoPath"),
            isFavorite = obj.optBoolean("isFavorite"),
            provider = obj.optString("provider"),
            providerId = obj.optString("providerId"),
            tags = obj.optString("tags"),
            description = obj.optString("description"),
            country = obj.optString("country"),
            countryCode = obj.optString("countryCode"),
        ),
    )
}

private fun deleteLogoFiles(logoPath: String) {
    if (logoPath.isBlank() || logoPath.startsWith("http")) return
    val file = java.io.File(logoPath)
    file.delete()
    java.io.File(file.parentFile, "${file.nameWithoutExtension}_media.png").delete()
}

// Maps English number words and digit strings to zero-padded numbers so that
// "BBC Radio One", "BBC Radio Two" … sort in numeric order rather than alphabetically.
private val NUMBER_WORDS = mapOf(
    "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
    "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
    "ten" to 10, "eleven" to 11, "twelve" to 12,
)

private fun stationSortKey(name: String): String =
    name.split(Regex("\\s+")).joinToString(" ") { token ->
        NUMBER_WORDS[token.lowercase()]?.let { "%03d".format(it) }
            ?: token.toIntOrNull()?.let { "%03d".format(it) }
            ?: token.lowercase()
    }

private fun PlaybackException.userMessage(): String {
    return when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_TIMEOUT,
        -> "Connection failed"
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> "Stream format unsupported"
        else -> "Playback failed"
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: StationRepository,
    private val registryRepository: RegistryRepository,
    private val dataStore: DataStore<Preferences>,
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(application, repository, registryRepository, dataStore, extras.createSavedStateHandle()) as T
    }
}
