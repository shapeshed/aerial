package com.shapeshed.aerial.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.PlayerService
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
                    _availableCountries.value = registryRepository.availableCountryCodes()
                    _allTags.value = registryRepository.availableTags()
                }
        }
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            _selectedCountries.value = prefs[SEARCH_COUNTRIES_KEY]
                ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            _selectedTags.value = prefs[SEARCH_TAGS_KEY]
                ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        }
    }

    private val _allStations: StateFlow<List<Station>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stations: StateFlow<List<Station>> = _allStations
        .map { list -> list.sortedWith(compareBy { stationSortKey(it.name) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val appIconArtwork: ByteArray? by lazy { appIconBitmap(getApplication()) }

    private val _currentStationId = MutableStateFlow<Long?>(null)
    private val _ephemeralStation = MutableStateFlow<Station?>(null)
    // Carries the last-played station to loadStationPaused() once the MediaController connects.
    // CompletableDeferred ensures the handoff is safe regardless of which side wins the race.
    private val pendingRestoreStation = CompletableDeferred<Station?>()

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

    private val _currentTrackArtworkUrl = MutableStateFlow<String?>(null)
    val currentTrackArtworkUrl: StateFlow<String?> = _currentTrackArtworkUrl.asStateFlow()

    private val _currentTrackArtworkData = MutableStateFlow<ByteArray?>(null)
    val currentTrackArtworkData: StateFlow<ByteArray?> = _currentTrackArtworkData.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    val nowPlayingInfo: StateFlow<NowPlayingInfo?> = NowPlayingStore.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    private val _registrySearchResults = MutableStateFlow<List<RegistryStation>>(emptyList())
    val registrySearchResults: StateFlow<List<RegistryStation>> = _registrySearchResults.asStateFlow()

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

    fun playRandomFromCategory(tag: String) {
        viewModelScope.launch {
            val station = withContext(Dispatchers.IO) {
                registryRepository.randomByCategory(tag.lowercase())
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
                ),
            )
            _recentlyAddedStationId.value = stationId
        }
    }

    fun removeFromRegistry(registryStation: RegistryStation) {
        val station = _allStations.value.find { it.streamUrl == registryStation.streamUrl } ?: return
        deleteStation(station)
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
                    controller?.mediaMetadata?.artist?.toString()?.trim()
                        ?.takeIf { it.isNotEmpty() && it != "Live Radio" }
                        ?.let { _currentTrackTitle.value = it }
                } else if (_currentStationId.value == null) {
                    _isPlaying.value = controller?.isPlaying ?: false
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
            val nowFavorite = !station.isFavorite
            repository.update(station.copy(isFavorite = nowFavorite))
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            currentStation.value?.let { station ->
                persistLastPlayedStation(station)
            }
        }
        override fun onPlaybackStateChanged(state: Int) {
            // Only show buffering when the user intends to play; suppress the transient
            // STATE_BUFFERING that fires during prepare() when restoring a paused station.
            _isBuffering.value = state == Player.STATE_BUFFERING && controller?.playWhenReady == true
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
                !title.isNullOrEmpty() && title != "Live Radio" -> _currentTrackTitle.value = title
                !artist.isNullOrEmpty() && artist != "Live Radio" -> _currentTrackTitle.value = artist
            }
            _currentTrackArtworkData.value = artworkData
            _currentTrackArtworkUrl.value = if (artworkData == null && !artwork.isNullOrEmpty()) {
                artwork
            } else {
                null
            }
        }
    }

    fun play(station: Station) {
        if (station.id == 0L) {
            _ephemeralStation.value = station
            _currentStationId.value = null
        } else {
            _ephemeralStation.value = null
            _currentStationId.value = station.id
        }
        _currentTrackTitle.value = null
        _currentTrackArtworkUrl.value = null
        _currentTrackArtworkData.value = null
        _playbackError.value = null
        persistLastPlayedStation(station)
        val artworkUri = station.logoPath
            .takeIf { it.startsWith("http") }
            ?.let { Uri.parse(it) }
        val mediaItem = MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(bauerStreamUrl(station))
            .setMediaMetadata(
                MediaMetadata.Builder().apply {
                    val localArtworkData = station.logoPath
                        .takeIf { it.isNotEmpty() && !it.startsWith("http") }
                        ?.let { compressedLogoData(File(it)) }
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
                    .setArtist("Live Radio")
                    .setSubtitle("Live Radio")
                    .setExtras(Bundle().apply {
                        putString("provider", station.provider)
                        putString("providerId", station.providerId)
                        putString("streamUrl", station.streamUrl)
                    })
                    .build()
            )
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
                val dest = logoFileForUrl(url, dir, conn.contentType)
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
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist("Live Radio")
                    .setSubtitle("Live Radio")
                    .build(),
            )
            .build()

        controller?.apply {
            setMediaItem(mediaItem)
            prepare()
            pause()
        }
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
