package com.shapeshed.aerial.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.shapeshed.aerial.PlayerService
import com.shapeshed.aerial.data.RadioBrowserApi
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val GRID_VIEW_KEY = booleanPreferencesKey("grid_view")

class MainViewModel(
    application: Application,
    private val repository: StationRepository,
    private val dataStore: DataStore<Preferences>,
) : AndroidViewModel(application) {

    val isGridView: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[GRID_VIEW_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    val stations: StateFlow<List<Station>> = combine(
        repository.getAll(),
        _showFavoritesOnly,
    ) { list, favOnly ->
        if (favOnly) list.filter { it.isFavorite } else list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var statsServer: String? = null

    private val _currentStationId = MutableStateFlow<Long?>(null)

    val currentStation: StateFlow<Station?> = combine(
        repository.getAll(),
        _currentStationId,
    ) { list, id -> list.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _bitrateKbps = MutableStateFlow<Int?>(null)
    val bitrateKbps: StateFlow<Int?> = _bitrateKbps.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow<String?>(null)
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    val grayscaleLogos: StateFlow<Boolean> = dataStore.data
        .map { it[GRAYSCALE_LOGOS_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    fun connect(context: Context) {
        val token = SessionToken(context, ComponentName(context, PlayerService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
        }, ContextCompat.getMainExecutor(context))
    }

    fun setGridView(gridView: Boolean) {
        viewModelScope.launch { dataStore.edit { it[GRID_VIEW_KEY] = gridView } }
    }

    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun setFavoritesFilter(favOnly: Boolean) {
        _showFavoritesOnly.value = favOnly
    }

    fun toggleFavorite(station: Station) {
        val nowFavorite = !station.isFavorite
        viewModelScope.launch {
            repository.update(station.copy(isFavorite = nowFavorite))
            if (nowFavorite && station.radioBrowserUuid.isNotEmpty()) {
                launch(Dispatchers.IO) {
                    runCatching {
                        val srv = statsServer ?: RadioBrowserApi.discoverServer().also { statsServer = it }
                        RadioBrowserApi.vote(srv, station.radioBrowserUuid)
                    }
                }
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
        override fun onPlaybackStateChanged(state: Int) {
            _isBuffering.value = state == Player.STATE_BUFFERING
        }
        override fun onPlayerError(error: PlaybackException) {
            _isBuffering.value = false
            _isPlaying.value = false
            _playbackError.value = error.userMessage()
        }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val artist = mediaMetadata.artist?.toString()?.trim()
            if (!artist.isNullOrEmpty() && artist != "Live Radio") {
                _currentTrackTitle.value = artist
            }
        }
        @OptIn(UnstableApi::class)
        override fun onTracksChanged(tracks: Tracks) {
            val bitrate = tracks.groups
                .firstOrNull { it.isSelected && it.type == C.TRACK_TYPE_AUDIO }
                ?.let { group ->
                    (0 until group.length)
                        .firstOrNull { group.isTrackSelected(it) }
                        ?.let { group.getTrackFormat(it).bitrate }
                }
                ?.takeIf { it > 0 }
            _bitrateKbps.value = bitrate?.div(1000)
        }
    }

    fun play(station: Station) {
        _currentStationId.value = station.id
        _currentTrackTitle.value = null
        _bitrateKbps.value = null
        _playbackError.value = null
        if (station.radioBrowserUuid.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    val srv = statsServer ?: RadioBrowserApi.discoverServer().also { statsServer = it }
                    RadioBrowserApi.registerClick(srv, station.radioBrowserUuid)
                }
            }
        }
        val artworkUri = station.logoPath
            .takeIf { it.startsWith("http") }
            ?.let { Uri.parse(it) }
        val mediaItem = MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder().apply {
                    val localArtworkData = station.logoPath
                        .takeIf { it.isNotEmpty() && !it.startsWith("http") }
                        ?.let { compressedLogoData(File(it)) }
                    if (localArtworkData != null) {
                        setArtworkData(
                            localArtworkData,
                            MediaMetadata.PICTURE_TYPE_FRONT_COVER,
                        )
                    } else {
                        artworkUri?.let { setArtworkUri(it) }
                    }
                }
                    .setTitle(station.name)
                    .setArtist("Live Radio")
                    .setSubtitle("Live Radio")
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

    fun addStation(name: String, streamUrl: String, logoPath: String = "", radioBrowserUuid: String = "") {
        viewModelScope.launch {
            val localLogoPath = if (logoPath.startsWith("http")) {
                withContext(Dispatchers.IO) { downloadLogo(logoPath) } ?: logoPath
            } else {
                logoPath
            }
            repository.insert(Station(
                name = name.trim(),
                streamUrl = streamUrl.trim(),
                logoPath = localLogoPath.trim(),
                radioBrowserUuid = radioBrowserUuid.trim(),
            ))
        }
    }

    private suspend fun downloadLogo(url: String): String? {
        return try {
            val dir = File(getApplication<Application>().filesDir, "logos").also { it.mkdirs() }
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val dest = logoFileForUrl(url, dir, conn.contentType)
            conn.inputStream.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            ensureMediaArtworkForLogo(getApplication(), dest)
            dest.absolutePath
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
            repository.delete(station)
        }
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
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
    private val dataStore: DataStore<Preferences>,
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(application, repository, dataStore) as T
    }
}
