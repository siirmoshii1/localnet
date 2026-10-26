package com.example.network

import android.util.Log
import com.example.model.P2PPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles lightweight TCP Server and Client networking for peer-to-peer text chat,
 * call signaling (invite, accept, decline, end), and heartbeat pings.
 */
class TcpMessenger(
    private val localPort: Int = NetworkUtils.TCP_PORT,
    private val onPacketReceived: (P2PPacket, String) -> Unit
) {
    private val TAG = "TcpMessenger"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val activeClients = ConcurrentHashMap<String, Socket>()

    fun startServer(scope: CoroutineScope) {
        if (serverJob != null && serverJob?.isActive == true) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(localPort).apply {
                    reuseAddress = true
                }
                Log.i(TAG, "TCP Server started on port $localPort")

                while (isActive && serverSocket?.isClosed == false) {
                    val clientSocket = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        null
                    } ?: break

                    val remoteIp = clientSocket.inetAddress?.hostAddress ?: "unknown"
                    launch(Dispatchers.IO) {
                        handleClient(clientSocket, remoteIp)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket error: ${e.message}")
            }
        }
    }

    private suspend fun handleClient(socket: Socket, remoteIp: String) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 0 // Wait for incoming data
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isNotBlank()) {
                    val packet = P2PPacket.deserialize(line)
                    if (packet != null) {
                        onPacketReceived(packet, remoteIp)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Client connection ended: ${socket.inetAddress} (${e.message})")
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    /**
     * Sends a packet to the target IP address via direct TCP socket.
     */
    suspend fun sendPacket(targetIp: String, packet: P2PPacket): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(targetIp, localPort), 4000)
            socket.soTimeout = 4000

            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
            writer.write(packet.serialize())
            writer.newLine()
            writer.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send packet to $targetIp: ${e.message}")
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (ignored: Exception) {}
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverJob?.cancel()
        serverJob = null
    }
}
