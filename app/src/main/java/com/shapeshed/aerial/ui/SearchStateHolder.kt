package com.shapeshed.aerial.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.RegistryStation
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException

private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
private val SEARCH_COUNTRIES_KEY = stringPreferencesKey("search_countries")
private val SEARCH_TAGS_KEY = stringPreferencesKey("search_tags")
private const val MAX_RECENT_SEARCHES = 5

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal class SearchStateHolder(
    private val scope: CoroutineScope,
    private val repository: StationRepository,
    private val registryRepository: RegistryRepository,
    private val dataStore: DataStore<Preferences>,
) {
    private val _registryResults = MutableStateFlow<List<RegistryStation>>(emptyList())
    val registryResults: StateFlow<List<RegistryStation>> = _registryResults.asStateFlow()

    private val _favoriteResults = MutableStateFlow<List<Station>>(emptyList())
    val favoriteResults: StateFlow<List<Station>> = _favoriteResults.asStateFlow()

    private val _selectedCountries = MutableStateFlow<Set<String>>(emptySet())
    val selectedCountries: StateFlow<Set<String>> = _selectedCountries.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    val recentSearches: StateFlow<List<String>> = dataStore.data
        .map { preferences -> preferences.recentSearches() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private data class SearchRequest(
        val query: String,
        val countries: Set<String>,
        val tags: Set<String>,
    )

    private val searchRequests = MutableStateFlow(SearchRequest("", emptySet(), emptySet()))

    init {
        searchRequests
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .mapLatest { request ->
                if (request.query.isBlank()) {
                    _favoriteResults.value = emptyList()
                } else {
                    _favoriteResults.value = repository.searchFavorites(request.query)
                }
                _registryResults.value = registryRepository.search(
                    request.query,
                    request.countries,
                    request.tags,
                )
            }
            .launchIn(scope)
    }

    fun restoreFilters(preferences: Preferences) {
        _selectedCountries.value = preferences[SEARCH_COUNTRIES_KEY].toFilterSet()
        _selectedTags.value = preferences[SEARCH_TAGS_KEY].toFilterSet()
    }

    fun search(query: String) {
        publishSearchRequest(query)
    }

    fun toggleCountry(country: String) {
        _selectedCountries.value = _selectedCountries.value.toggle(country)
        filtersChanged()
    }

    fun setCountry(country: String) {
        _selectedCountries.value = setOf(country)
        filtersChanged()
    }

    fun toggleTag(tag: String) {
        _selectedTags.value = _selectedTags.value.toggle(tag)
        filtersChanged()
    }

    fun clearCountries() {
        _selectedCountries.value = emptySet()
        filtersChanged()
    }

    fun clearTags() {
        _selectedTags.value = emptySet()
        filtersChanged()
    }

    fun clearFilters() {
        _selectedCountries.value = emptySet()
        _selectedTags.value = emptySet()
        filtersChanged()
    }

    fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        scope.launch {
            dataStore.edit { preferences ->
                val searches = preferences.recentSearches().toMutableList()
                searches.remove(trimmed)
                searches.add(0, trimmed)
                preferences[RECENT_SEARCHES_KEY] = JSONArray(searches.take(MAX_RECENT_SEARCHES)).toString()
            }
        }
    }

    fun removeRecentSearch(query: String) {
        scope.launch {
            dataStore.edit { preferences ->
                val searches = preferences.recentSearches().toMutableList()
                searches.remove(query)
                preferences[RECENT_SEARCHES_KEY] = JSONArray(searches).toString()
            }
        }
    }

    private fun filtersChanged() {
        persistFilters()
        publishSearchRequest()
    }

    private fun persistFilters() {
        val countries = _selectedCountries.value
        val tags = _selectedTags.value
        scope.launch {
            dataStore.edit { preferences ->
                preferences[SEARCH_COUNTRIES_KEY] = countries.joinToString(",")
                preferences[SEARCH_TAGS_KEY] = tags.joinToString(",")
            }
        }
    }

    private fun publishSearchRequest(query: String? = null) {
        val current = searchRequests.value
        searchRequests.value = SearchRequest(
            query = query ?: current.query,
            countries = _selectedCountries.value,
            tags = _selectedTags.value,
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value

private fun String?.toFilterSet(): Set<String> =
    this?.split(',')?.filter(String::isNotBlank)?.toSet().orEmpty()

private fun Preferences.recentSearches(): List<String> {
    val json = this[RECENT_SEARCHES_KEY] ?: return emptyList()
    return try {
        val array = JSONArray(json)
        List(array.length(), array::getString)
    } catch (_: JSONException) {
        emptyList()
    }
}
