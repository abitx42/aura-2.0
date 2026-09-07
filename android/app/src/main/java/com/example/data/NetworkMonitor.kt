package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Reactive network connectivity monitor.
 * Emits NetworkStatus changes as a Flow.
 */
sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Lost : NetworkStatus()
}

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available)
            }
            override fun onLost(network: Network) {
                trySend(NetworkStatus.Lost)
            }
            override fun onCapabilitiesChanged(
                network: Network,
                caps: NetworkCapabilities
            ) {
                val hasInternet = caps.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) && caps.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                trySend(
                    if (hasInternet) NetworkStatus.Available
                    else NetworkStatus.Lost
                )
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit initial state
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        val isConnected = caps?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) == true && caps.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
        trySend(if (isConnected) NetworkStatus.Available else NetworkStatus.Lost)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
