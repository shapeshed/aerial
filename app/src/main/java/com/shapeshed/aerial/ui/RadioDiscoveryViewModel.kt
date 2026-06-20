package com.shapeshed.aerial.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.data.RadioBrowserApi
import com.shapeshed.aerial.data.RadioBrowserServerException
import com.shapeshed.aerial.data.RadioBrowserStation
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ErrorKind { CONNECTIVITY, SERVICE, GENERIC }

data class DiscoveryError(val message: String, val kind: ErrorKind)

class RadioDiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val networkMonitor = (application as AerialApp).networkMonitor

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<RadioBrowserStation>>(emptyList())
    val results: StateFlow<List<RadioBrowserStation>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<DiscoveryError?>(null)
    val error: StateFlow<DiscoveryError?> = _error.asStateFlow()

    private var searchJob: Job? = null

    private val _searchedOnce = MutableStateFlow(false)
    val searchedOnce: StateFlow<Boolean> = _searchedOnce.asStateFlow()

    private val _isOfflineFallback = MutableStateFlow(false)
    val isOfflineFallback: StateFlow<Boolean> = _isOfflineFallback.asStateFlow()

    init {
        // Trigger search as the user types.
        viewModelScope.launch {
            _query.collectLatest { q ->
                if (q.trim().length >= 3) {
                    delay(300L)
                    search()
                }
            }
        }

        // React to network changes. If we come back online and the user has a query,
        // retry automatically. If we go offline mid-search, cancel and show fallback.
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online) {
                    // Came back online — retry if there's a pending query and no live results.
                    val q = _query.value.trim()
                    if (q.length >= 3 && _isOfflineFallback.value) {
                        search()
                    }
                } else {
                    // Lost network — cancel any in-flight search and fall back immediately.
                    val q = _query.value.trim()
                    if (q.length >= 3 && (_isLoading.value || _results.value.isEmpty())) {
                        searchJob?.cancel()
                        val fallback = RadioBrowserApi.getFallback(q)
                        if (fallback.isNotEmpty()) {
                            _results.value = fallback
                            _searchedOnce.value = true
                            _isOfflineFallback.value = true
                            _error.value = null
                        } else {
                            _error.value = DiscoveryError(
                                "No internet connection. Check your Wi-Fi or mobile data.",
                                ErrorKind.CONNECTIVITY,
                            )
                            _results.value = emptyList()
                        }
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun onQueryChange(q: String) {
        _query.value = q
        if (q.trim().length < 3) {
            searchJob?.cancel()
            _isLoading.value = false
            _results.value = emptyList()
            _error.value = null
            _searchedOnce.value = false
            _isOfflineFallback.value = false
        }
    }

    fun retry() = search()

    fun search() {
        val q = _query.value.trim().takeIf { it.length >= 3 } ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Clear stale results immediately — stale data must never show through
            // while the new search is in flight.
            _isLoading.value = true
            _error.value = null
            _results.value = emptyList()
            _isOfflineFallback.value = false

            // No network: skip the API entirely and go straight to bundled fallback.
            if (!networkMonitor.isOnline.value) {
                val fallback = RadioBrowserApi.getFallback(q)
                if (fallback.isNotEmpty()) {
                    _results.value = fallback
                    _searchedOnce.value = true
                    _isOfflineFallback.value = true
                } else {
                    _error.value = DiscoveryError(
                        "No internet connection. Check your Wi-Fi or mobile data.",
                        ErrorKind.CONNECTIVITY,
                    )
                }
                _isLoading.value = false
                return@launch
            }

            // Race the live API against a 1 s deadline. If we have no live result by
            // then, show the bundled fallback so the user sees something immediately.
            // When (and if) the live search eventually completes, it silently replaces
            // the fallback with fresh results.
            var fallbackShown = false
            val deadline = launch {
                delay(1_000)
                val fallback = RadioBrowserApi.getFallback(q)
                if (fallback.isNotEmpty()) {
                    fallbackShown = true
                    _results.value = fallback
                    _searchedOnce.value = true
                    _isOfflineFallback.value = true
                    _isLoading.value = false
                }
            }

            try {
                val live = RadioBrowserApi.search(q)
                deadline.cancel()
                _results.value = live
                _searchedOnce.value = true
                _isOfflineFallback.value = false
                _error.value = null
            } catch (e: CancellationException) {
                deadline.cancel()
                throw e
            } catch (e: Exception) {
                deadline.cancel()
                // If the deadline already showed fallback results, keep them silently.
                if (!fallbackShown || _results.value.isEmpty()) {
                    val fallback = RadioBrowserApi.getFallback(q)
                    if (fallback.isNotEmpty()) {
                        _results.value = fallback
                        _searchedOnce.value = true
                        _isOfflineFallback.value = true
                    } else {
                        _isOfflineFallback.value = false
                        _results.value = emptyList()
                        _error.value = mapException(e)
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun mapException(e: Exception): DiscoveryError = when (e) {
        is RadioBrowserServerException -> if (e.code in 500..599) {
            DiscoveryError("Station search is temporarily unavailable. Try again in a moment.", ErrorKind.SERVICE)
        } else {
            DiscoveryError("Something went wrong with station search. Try again.", ErrorKind.GENERIC)
        }
        is UnknownHostException -> DiscoveryError(
            "No internet connection. Check your Wi-Fi or mobile data.",
            ErrorKind.CONNECTIVITY,
        )
        is SocketTimeoutException -> DiscoveryError(
            "Station search is taking too long. Try again.",
            ErrorKind.SERVICE,
        )
        else -> DiscoveryError("Station search isn't available right now. Try again.", ErrorKind.GENERIC)
    }
}
