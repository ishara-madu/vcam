package com.pixeleye.vcam.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    private const val TAG = "VCamNetworkUtils"

    fun getLocalIpAddress(context: Context? = null): String {
        // 1. Try ConnectivityManager LinkProperties (most reliable on Android 10+)
        if (context != null) {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                if (cm != null) {
                    val activeNetwork: Network? = cm.activeNetwork
                    val linkProperties: LinkProperties? = cm.getLinkProperties(activeNetwork)
                    if (linkProperties != null) {
                        for (linkAddress in linkProperties.linkAddresses) {
                            val addr = linkAddress.address
                            if (addr is Inet4Address && !addr.isLoopbackAddress) {
                                val host = addr.hostAddress
                                if (!host.isNullOrEmpty() && host != "127.0.0.1") {
                                    return host
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ConnectivityManager lookup error: ${e.message}")
            }
        }

        // 2. Iterate network interfaces (prioritizing wlan, eth, ap)
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()

            // Check WiFi / Ethernet interfaces first
            val preferredInterfaces = interfaces.filter { iface ->
                iface.isUp && !iface.isLoopback && (
                    iface.name.startsWith("wlan", ignoreCase = true) ||
                    iface.name.startsWith("eth", ignoreCase = true) ||
                    iface.name.startsWith("ap", ignoreCase = true) ||
                    iface.name.startsWith("rndis", ignoreCase = true)
                )
            }

            for (iface in preferredInterfaces + interfaces.filter { it.isUp && !it.isLoopback }) {
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress
                        if (!host.isNullOrEmpty() && host != "127.0.0.1") {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "NetworkInterface lookup error: ${e.message}")
        }

        return "127.0.0.1"
    }
}
