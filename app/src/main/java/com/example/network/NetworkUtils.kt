package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.model.DiscoveredPeer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {
    private const val TAG = "NetworkUtils"
    const val DISCOVERY_PORT = 8894
    const val TCP_PORT = 8888
    const val VIDEO_PORT = 8890
    const val AUDIO_PORT = 8892

    /**
     * Finds the local IPv4 address of this Android device on Wi-Fi or active local interface.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // Prefer wlan0 or non-loopback IPv4
            var fallbackIp: String? = null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (intf.name.startsWith("wlan") || intf.name.startsWith("eth")) {
                            return host
                        }
                        if (fallbackIp == null) {
                            fallbackIp = host
                        }
                    }
                }
            }
            if (fallbackIp != null) return fallbackIp
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining local IP", e)
        }
        return "127.0.0.1"
    }

    /**
     * Broadcasts UDP beacon on local subnet to announce presence.
     */
    suspend fun broadcastPresence(deviceName: String, localIp: String) = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            val message = "GHOSTLINK_BEACON|$deviceName|$localIp"
            val data = message.toByteArray(Charsets.UTF_8)

            // Try generic broadcast addresses
            val broadcastTargets = listOf(
                "255.255.255.255",
                getDirectedBroadcastAddress(localIp)
            )

            for (target in broadcastTargets) {
                try {
                    val address = InetAddress.getByName(target)
                    val packet = DatagramPacket(data, data.size, address, DISCOVERY_PORT)
                    socket.send(packet)
                } catch (e: Exception) {
                    // Ignore per-target send failure
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast discovery beacon: ${e.message}")
        } finally {
            socket?.close()
        }
    }

    private fun getDirectedBroadcastAddress(localIp: String): String {
        val parts = localIp.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}.255"
        } else {
            "255.255.255.255"
        }
    }

    /**
     * Listens for discovery beacons from other peers on the local Wi-Fi.
     */
    suspend fun listenForBeacons(
        localIp: String,
        onPeerDiscovered: (DiscoveredPeer) -> Unit
    ) = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(DISCOVERY_PORT)
            socket.broadcast = true
            val buffer = ByteArray(1024)

            while (true) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val parts = text.split("|")
                if (parts.size >= 3 && parts[0] == "GHOSTLINK_BEACON") {
                    val peerName = parts[1]
                    val peerIp = parts[2]
                    // Do not discover ourselves
                    if (peerIp != localIp && peerIp != "127.0.0.1") {
                        onPeerDiscovered(DiscoveredPeer(ip = peerIp, name = peerName))
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Discovery listener closed: ${e.message}")
        } finally {
            socket?.close()
        }
    }
}
