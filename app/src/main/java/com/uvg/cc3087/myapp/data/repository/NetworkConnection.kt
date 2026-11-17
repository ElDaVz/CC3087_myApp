package com.uvg.cc3087.myapp.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One application-scoped callback. Reachability still requires a successful backend request. */
class NetworkConnection(context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private fun available() = manager.getNetworkCapabilities(manager.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    private val mutableConnected = MutableStateFlow(available())
    val connected = mutableConnected.asStateFlow()
    init {
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) { mutableConnected.value = false }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                mutableConnected.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        })
    }
}
