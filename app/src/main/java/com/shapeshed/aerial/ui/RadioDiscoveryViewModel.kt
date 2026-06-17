package com.shapeshed.aerial.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shapeshed.aerial.data.RadioBrowserApi
import com.shapeshed.aerial.data.RadioBrowserStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RadioDiscoveryViewModel : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<RadioBrowserStation>>(emptyList())
    val results: StateFlow<List<RadioBrowserStation>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var server: String? = null

    private val _searchedOnce = MutableStateFlow(false)
    val searchedOnce: StateFlow<Boolean> = _searchedOnce.asStateFlow()

    fun onQueryChange(q: String) {
        _query.value = q
        _searchedOnce.value = false
    }

    fun search() {
        val q = _query.value.trim().takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val srv = server ?: RadioBrowserApi.discoverServer().also { server = it }
                _results.value = RadioBrowserApi.search(srv, q)
                _searchedOnce.value = true
            } catch (e: Exception) {
                server = null
                _error.value = "Search failed — check your connection."
                _results.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
