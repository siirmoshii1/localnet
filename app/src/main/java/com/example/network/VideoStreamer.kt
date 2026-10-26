package com.example.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles low-latency local P2P video streaming over a dedicated TCP frame socket.
 * Delivers crisp 15-20 FPS compressed JPEG video directly on local Wi-Fi.
 */
class VideoStreamer(
    private val videoPort: Int = NetworkUtils.VIDEO_PORT,
    private val onRemoteFrameReceived: (Bitmap) -> Unit
) {
    private val TAG = "VideoStreamer"
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    private var senderJob: Job? = null
    private var senderSocket: Socket? = null
    private var senderDos: DataOutputStream? = null

    private val isSending = AtomicBoolean(false)
    private val isFrameInFlight = AtomicBoolean(false)

    /**
     * Starts listening for incoming video frames from peer on [videoPort].
     */
    fun startReceiving(scope: CoroutineScope) {
        if (serverJob?.isActive == true) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(videoPort).apply {
                    reuseAddress = true
                }
                Log.i(TAG, "Video server listening on TCP $videoPort")

                while (isActive && serverSocket?.isClosed == false) {
                    val client = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        null
                    } ?: break

                    launch(Dispatchers.IO) {
                        handleIncomingVideoStream(client)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Video server error: ${e.message}")
            }
        }
    }

    private suspend fun handleIncomingVideoStream(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.tcpNoDelay = true
            socket.soTimeout = 10000
            val dis = DataInputStream(socket.getInputStream())

            while (isActive && !socket.isClosed) {
                val length = dis.readInt()
                if (length <= 0 || length > 1024 * 1024 * 5) {
                    // Invalid frame size
                    break
                }
                val buffer = ByteArray(length)
                dis.readFully(buffer)

                val bitmap = BitmapFactory.decodeByteArray(buffer, 0, length)
                if (bitmap != null) {
                    onRemoteFrameReceived(bitmap)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Incoming video client stream ended: ${e.message}")
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    /**
     * Connects to target IP and prepares to stream outgoing camera frames.
     */
    fun startSending(scope: CoroutineScope, targetIp: String) {
        if (senderJob?.isActive == true) return
        isSending.set(true)

        senderJob = scope.launch(Dispatchers.IO) {
            var attempts = 0
            while (isActive && isSending.get()) {
                try {
                    val socket = Socket()
                    socket.tcpNoDelay = true
                    socket.connect(InetSocketAddress(targetIp, videoPort), 3000)
                    senderSocket = socket
                    senderDos = DataOutputStream(socket.getOutputStream())
                    Log.i(TAG, "Video sender connected to $targetIp:$videoPort")

                    // Hold loop open until disconnected
                    while (isActive && isSending.get() && !socket.isClosed) {
                        Thread.sleep(100)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Waiting to connect video stream to $targetIp... (${e.message})")
                    attempts++
                    Thread.sleep(1000)
                } finally {
                    closeSender()
                }
            }
        }
    }

    /**
     * Transmits a single JPEG frame. If a previous frame is currently transmitting,
     * this frame is dropped to prevent queue buildup and ensure minimum latency.
     */
    fun sendFrame(jpegBytes: ByteArray) {
        if (!isSending.get() || senderDos == null) return
        if (isFrameInFlight.compareAndSet(false, true)) {
            try {
                val dos = senderDos ?: return
                dos.writeInt(jpegBytes.size)
                dos.write(jpegBytes)
                dos.flush()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send frame: ${e.message}")
                closeSender()
            } finally {
                isFrameInFlight.set(false)
            }
        }
    }

    private fun closeSender() {
        try { senderDos?.close() } catch (ignored: Exception) {}
        try { senderSocket?.close() } catch (ignored: Exception) {}
        senderDos = null
        senderSocket = null
    }

    fun stop() {
        isSending.set(false)
        closeSender()
        senderJob?.cancel()
        senderJob = null

        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverJob?.cancel()
        serverJob = null
    }
}
