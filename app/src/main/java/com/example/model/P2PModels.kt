package com.example.model

import android.graphics.Bitmap
import java.util.UUID

/**
 * Data models for peer-to-peer communication.
 */

data class DeviceContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ip: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun serialize(): String {
        return "$id|$name|$ip|$note|$createdAt"
    }

    companion object {
        fun deserialize(raw: String): DeviceContact? {
            return try {
                val parts = raw.split("|")
                if (parts.size >= 3) {
                    DeviceContact(
                        id = parts[0],
                        name = parts[1],
                        ip = parts[2],
                        note = if (parts.size > 3) parts[3] else "",
                        createdAt = if (parts.size > 4) parts[4].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class ChatMessage(
    val id: String,
    val senderName: String,
    val senderIp: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMine: Boolean,
    val isSystemNotification: Boolean = false
)

enum class PacketType {
    CHAT,
    CALL_INVITE,
    CALL_ACCEPT,
    CALL_DECLINE,
    CALL_END,
    PING,
    PONG
}

data class P2PPacket(
    val type: PacketType,
    val senderName: String,
    val senderIp: String,
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun serialize(): String {
        return "${type.name}|$senderName|$senderIp|$timestamp|${content.replace("\n", "\\n")}"
    }

    companion object {
        fun deserialize(raw: String): P2PPacket? {
            return try {
                val parts = raw.split("|", limit = 5)
                if (parts.size < 4) return null
                val type = PacketType.valueOf(parts[0])
                val senderName = parts[1]
                val senderIp = parts[2]
                val timestamp = parts[3].toLongOrNull() ?: System.currentTimeMillis()
                val content = if (parts.size > 4) parts[4].replace("\\n", "\n") else ""
                P2PPacket(type, senderName, senderIp, content, timestamp)
            } catch (e: Exception) {
                null
            }
        }
    }
}

sealed interface CallState {
    object Idle : CallState
    data class OutgoingCalling(val targetIp: String, val targetName: String) : CallState
    data class IncomingCall(val callerIp: String, val callerName: String) : CallState
    data class InCall(
        val peerIp: String,
        val peerName: String,
        val startTime: Long = System.currentTimeMillis(),
        val isMuted: Boolean = false,
        val isVideoEnabled: Boolean = true,
        val isFrontCamera: Boolean = true,
        val remoteFrame: Bitmap? = null
    ) : CallState
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class DiscoveredPeer(
    val ip: String,
    val name: String,
    val lastSeen: Long = System.currentTimeMillis()
)

enum class AppScreen(val label: String) {
    CONTACTS("DEVICES"),
    CONFIG("CONFIG"),
    CHAT("CHAT"),
    CALL("CALL")
}
