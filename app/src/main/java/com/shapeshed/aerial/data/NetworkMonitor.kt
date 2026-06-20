package com.shapeshed.aerial.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrent())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        // A new default network is available — traffic is routed.
        override fun onAvailable(network: Network) {
            _isOnline.value = true
        }

        // The default network is gone with no replacement — truly offline.
        // With registerDefaultNetworkCallback this is NOT called when one network
        // is simply replaced by another (e.g. cellular → WiFi); in that case
        // onAvailable fires for the new default first.
        override fun onLost(network: Network) {
            _isOnline.value = false
        }

        // Default network capabilities changed (e.g. captive portal resolved).
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    init {
        cm.registerDefaultNetworkCallback(callback)
    }

    private fun checkCurrent(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

}
