package com.shapeshed.aerial.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NetworkMonitorTest {
    private val connectivityManager = mock<ConnectivityManager>()
    private val context = mock<Context>().apply {
        whenever(getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
    }

    @Test
    fun reportsOnlineWhenTheActiveNetworkHasInternet() {
        val network = networkWith(internet = true)
        whenever(connectivityManager.activeNetwork).thenReturn(network)

        assertEquals(true, NetworkMonitor(context).isOnline.value)
    }

    @Test
    fun reportsOfflineWhenThereIsNoActiveNetwork() {
        whenever(connectivityManager.activeNetwork).thenReturn(null)

        assertEquals(false, NetworkMonitor(context).isOnline.value)
    }

    @Test
    fun reportsOfflineWhenCapabilitiesAreMissing() {
        val network = mock<Network>()
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(null)

        assertEquals(false, NetworkMonitor(context).isOnline.value)
    }

    @Test
    fun reportsOfflineWhenTheNetworkLacksInternetCapability() {
        val network = networkWith(internet = false)
        whenever(connectivityManager.activeNetwork).thenReturn(network)

        assertEquals(false, NetworkMonitor(context).isOnline.value)
    }

    @Test
    fun defaultNetworkCallbacksToggleOnlineState() {
        whenever(connectivityManager.activeNetwork).thenReturn(null)
        val monitor = NetworkMonitor(context)
        val callback = registeredCallback()
        val network = mock<Network>()

        callback.onAvailable(network)
        assertEquals(true, monitor.isOnline.value)

        callback.onLost(network)
        assertEquals(false, monitor.isOnline.value)
    }

    @Test
    fun capabilityChangesFollowInternetCapability() {
        whenever(connectivityManager.activeNetwork).thenReturn(null)
        val monitor = NetworkMonitor(context)
        val callback = registeredCallback()
        val network = mock<Network>()

        callback.onCapabilitiesChanged(network, capabilities(internet = true))
        assertEquals(true, monitor.isOnline.value)

        callback.onCapabilitiesChanged(network, capabilities(internet = false))
        assertEquals(false, monitor.isOnline.value)
    }

    private fun networkWith(internet: Boolean): Network {
        val network = mock<Network>()
        val networkCapabilities = capabilities(internet)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        return network
    }

    private fun capabilities(internet: Boolean): NetworkCapabilities {
        val capabilities = mock<NetworkCapabilities>()
        whenever(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(internet)
        return capabilities
    }

    private fun registeredCallback(): ConnectivityManager.NetworkCallback {
        val captor = argumentCaptor<ConnectivityManager.NetworkCallback>()
        verify(connectivityManager).registerDefaultNetworkCallback(captor.capture())
        return captor.firstValue
    }
}
