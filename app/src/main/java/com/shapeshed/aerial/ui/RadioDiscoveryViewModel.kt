package com.shapeshed.aerial.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class RadioDiscoveryViewModel : ViewModel() {

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

    init {
        viewModelScope.launch {
            _query.collectLatest { q ->
                if (q.trim().length >= 3) {
                    delay(300L)
                    search()
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
        }
    }

    fun retry() = search()

    fun search() {
        val q = _query.value.trim().takeIf { it.length >= 3 } ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _results.value = RadioBrowserApi.search(q)
                _searchedOnce.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: RadioBrowserServerException) {
                _error.value = if (e.code in 500..599) {
                    DiscoveryError("Station search is temporarily unavailable. Try again in a moment.", ErrorKind.SERVICE)
                } else {
                    DiscoveryError("Something went wrong with station search. Try again.", ErrorKind.GENERIC)
                }
                _results.value = emptyList()
            } catch (e: UnknownHostException) {
                _error.value = DiscoveryError("No internet connection. Check your Wi-Fi or mobile data.", ErrorKind.CONNECTIVITY)
                _results.value = emptyList()
            } catch (e: SocketTimeoutException) {
                _error.value = DiscoveryError("Station search is taking too long. Try again.", ErrorKind.SERVICE)
                _results.value = emptyList()
            } catch (e: Exception) {
                _error.value = DiscoveryError("Station search isn't available right now. Try again.", ErrorKind.GENERIC)
                _results.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
