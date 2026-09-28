package com.shapeshed.aerial.ui

import android.util.Log
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.StationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

private const val TAG = "DiscoveryStateHolder"
private const val RECENTLY_PLAYED_LIMIT = 10

/**
 * The registry-backed content the home screen shows: tags, featured stations, For You, browse
 * defaults, curated moods, available countries, and recently played.
 *
 * Split out of `MainViewModel` because it is one coherent concern with no playback, favourites,
 * search, or settings behaviour in it — every field here is a registry query or a play-history
 * read, and none of them mutate a station. Collection starts in [start] rather than in an
 * `init` block so `MainViewModel` can construct every `StateFlow` it owns before any collector
 * begins; on a real Android main looper a `launch` can run before the next property is assigned.
 */
internal class DiscoveryStateHolder(
    private val scope: CoroutineScope,
    private val repository: StationRepository,
    private val registryRepository: RegistryRepository,
) {
    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

    private val _featuredStations = MutableStateFlow<List<RegistryStation>>(emptyList())
    val featuredStations: StateFlow<List<RegistryStation>> = _featuredStations.asStateFlow()

    // For You is loaded for the device locale's country: a curated selection where one
    // exists, otherwise a random sample of that country's stations with artwork. Keyed by
    // country (distinctUntilChanged below) so the random pick stays stable for the session.
    private val forYouCountryState = MutableStateFlow("GB")
    private val _forYouStations = MutableStateFlow<List<RegistryStation>>(emptyList())
    val forYouStations: StateFlow<List<RegistryStation>> = _forYouStations.asStateFlow()

    private val _defaultStations = MutableStateFlow<List<RegistryStation>>(emptyList())
    val defaultStations: StateFlow<List<RegistryStation>> = _defaultStations.asStateFlow()

    private val _curatedMoodStations = MutableStateFlow<Map<String, List<RegistryStation>>>(emptyMap())
    val curatedMoodStations: StateFlow<Map<String, List<RegistryStation>>> = _curatedMoodStations.asStateFlow()

    // Populated by the same registry refresh as everything else here, but consumed by the search
    // filter sheet rather than by the home screen. It lives with the query that fills it.
    private val _availableCountries = MutableStateFlow<List<String>>(emptyList())
    val availableCountries: StateFlow<List<String>> = _availableCountries.asStateFlow()

    // Recently played stations for the home screen, same source as Android Auto's Recently
    // Played folder: the play_history table resolved against the registry (entries whose
    // station is no longer in the registry are skipped). Room re-emits on every recorded
    // play, so the row reorders live. The first resolution gates isInitialized (and so the
    // splash screen): the home list's scroll position is restored against the first
    // composition, so this section must already be present in it rather than streaming in
    // afterwards and shifting the restored position.
    private val _recentlyPlayedStations = MutableStateFlow<List<RegistryStation>>(emptyList())
    val recentlyPlayedStations: StateFlow<List<RegistryStation>> = _recentlyPlayedStations.asStateFlow()
    private val recentlyPlayedFirstLoad = CompletableDeferred<Unit>()

    fun setForYouCountry(countryCode: String) {
        if (countryCode.isNotBlank()) forYouCountryState.value = countryCode
    }

    /**
     * Resolves once the first recently-played emission has landed, or once the history query has
     * failed. Never completes on cancellation, so the caller's timeout is what bounds startup.
     */
    suspend fun awaitFirstRecentlyPlayed() {
        recentlyPlayedFirstLoad.await()
    }

    fun start() {
        scope.launch {
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
                        } else {
                            registryStation
                        }
                    }
                }
                // A failing history query must not leave the splash screen waiting on a signal
                // that will never arrive; the startup gate settles on its timeout either way.
                .catch { error ->
                    Log.w(TAG, "Recently played history unavailable", error)
                    recentlyPlayedFirstLoad.complete(Unit)
                }
                .collect {
                    _recentlyPlayedStations.value = it
                    // complete() is a no-op once signalled, so re-emissions are harmless.
                    recentlyPlayedFirstLoad.complete(Unit)
                }
        }
        scope.launch {
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
        scope.launch {
            combine(
                registryRepository.countAsFlow().filter { it > 0 }.distinctUntilChanged(),
                forYouCountryState,
            ) { _, country -> country }
                .distinctUntilChanged()
                .collect { country ->
                    _forYouStations.value = registryRepository.forYouStations(country)
                }
        }
    }
}
