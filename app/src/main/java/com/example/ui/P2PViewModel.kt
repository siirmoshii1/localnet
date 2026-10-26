package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ContactRepository
import com.example.model.AppScreen
import com.example.model.CallState
import com.example.model.ChatMessage
import com.example.model.ConnectionStatus
import com.example.model.DeviceContact
import com.example.model.DiscoveredPeer
import com.example.model.P2PPacket
import com.example.model.PacketType
import com.example.network.AudioStreamer
import com.example.network.NetworkUtils
import com.example.network.TcpMessenger
import com.example.network.VideoStreamer
import com.example.network.YuvToJpegConverter
import com.example.ui.theme.AccentColorPreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class P2PViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "P2PViewModel"

    private val contactRepository = ContactRepository(application)
    val contacts: StateFlow<List<DeviceContact>> = contactRepository.contacts

    private val _localIp = MutableStateFlow(NetworkUtils.getLocalIpAddress())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _deviceName = MutableStateFlow("Node-${Build.MODEL.take(8)}")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _targetIp = MutableStateFlow("")
    val targetIp: StateFlow<String> = _targetIp.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready - Enter Target IP or select a Device Contact")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.CONTACTS)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _accentPreset = MutableStateFlow(AccentColorPreset.MONOCHROME)
    val accentPreset: StateFlow<AccentColorPreset> = _accentPreset.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _remoteVideoFrame = MutableStateFlow<Bitmap?>(null)
    val remoteVideoFrame: StateFlow<Bitmap?> = _remoteVideoFrame.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0L)
    val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()

    private var callTimerJob: Job? = null

    // Networking engines
    private val tcpMessenger = TcpMessenger(
        localPort = NetworkUtils.TCP_PORT,
        onPacketReceived = { packet, remoteIp -> handleIncomingPacket(packet, remoteIp) }
    )

    private val audioStreamer = AudioStreamer(application)
    private val videoStreamer = VideoStreamer(
        videoPort = NetworkUtils.VIDEO_PORT,
        onRemoteFrameReceived = { bitmap -> _remoteVideoFrame.value = bitmap }
    )

    init {
        // Start TCP Server
        tcpMessenger.startServer(viewModelScope)

        // Start Video Server
        videoStreamer.startReceiving(viewModelScope)

        // Start Audio Server
        audioStreamer.startReceiving(viewModelScope)

        // Start UDP Peer Discovery Listener
        viewModelScope.launch {
            NetworkUtils.listenForBeacons(_localIp.value) { peer ->
                val current = _discoveredPeers.value.toMutableList()
                val idx = current.indexOfFirst { it.ip == peer.ip }
                if (idx >= 0) {
                    current[idx] = peer
                } else {
                    current.add(0, peer)
                }
                _discoveredPeers.value = current
            }
        }

        // Broadcast presence initially and periodically
        broadcastPresence()
        startPeriodicPresence()

        // Welcome system message
        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                senderName = "SYSTEM",
                senderIp = "127.0.0.1",
                content = "GhostLink initialized on ${_localIp.value}:${NetworkUtils.TCP_PORT}. No cloud or internet connection used.",
                isMine = false,
                isSystemNotification = true
            )
        )
    }

    fun resolveName(ip: String, defaultName: String): String {
        val contact = contactRepository.findByIp(ip)
        return contact?.name ?: defaultName
    }

    fun getContactForIp(ip: String): DeviceContact? {
        return contactRepository.findByIp(ip)
    }

    fun saveContact(name: String, ip: String, note: String = "") {
        contactRepository.saveContact(name, ip, note)
        _statusMessage.value = "Saved contact $name ($ip)"
    }

    fun deleteContact(contactId: String) {
        contactRepository.deleteContact(contactId)
    }

    fun saveDiscoveredPeerAsContact(peer: DiscoveredPeer) {
        contactRepository.saveContact(peer.name, peer.ip, "Discovered on Wi-Fi")
        _statusMessage.value = "Saved ${peer.name} to contacts"
    }

    fun selectContactForChat(contact: DeviceContact) {
        _targetIp.value = contact.ip
        _currentScreen.value = AppScreen.CHAT
    }

    fun selectContactForCall(contact: DeviceContact) {
        _targetIp.value = contact.ip
        startCall()
    }

    fun refreshLocalIp() {
        val ip = NetworkUtils.getLocalIpAddress()
        _localIp.value = ip
        broadcastPresence()
    }

    fun setTargetIp(ip: String) {
        _targetIp.value = ip.trim()
    }

    fun setDeviceName(name: String) {
        if (name.isNotBlank()) {
            _deviceName.value = name.trim()
            broadcastPresence()
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setAccentPreset(preset: AccentColorPreset) {
        _accentPreset.value = preset
    }

    fun broadcastPresence() {
        viewModelScope.launch {
            NetworkUtils.broadcastPresence(_deviceName.value, _localIp.value)
        }
    }

    private fun startPeriodicPresence() {
        viewModelScope.launch {
            while (isActive) {
                delay(12000)
                NetworkUtils.broadcastPresence(_deviceName.value, _localIp.value)
            }
        }
    }

    fun testConnection() {
        val target = _targetIp.value
        if (target.isBlank()) {
            _statusMessage.value = "Please enter target IP address"
            return
        }
        _connectionStatus.value = ConnectionStatus.CONNECTING
        val displayName = resolveName(target, target)
        _statusMessage.value = "Pinging $displayName:${NetworkUtils.TCP_PORT}..."

        viewModelScope.launch {
            val pingPacket = P2PPacket(
                type = PacketType.PING,
                senderName = _deviceName.value,
                senderIp = _localIp.value,
                content = "PING"
            )
            val result = tcpMessenger.sendPacket(target, pingPacket)
            if (result.isSuccess) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _statusMessage.value = "Connected to $displayName"
            } else {
                _connectionStatus.value = ConnectionStatus.ERROR
                _statusMessage.value = "Failed to connect to $displayName: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun sendChatMessage(text: String) {
        val target = _targetIp.value
        if (target.isBlank()) {
            _statusMessage.value = "Cannot send: No target IP configured"
            return
        }
        if (text.isBlank()) return

        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderName = "Me",
            senderIp = _localIp.value,
            content = text.trim(),
            isMine = true
        )
        addMessage(message)

        viewModelScope.launch {
            val packet = P2PPacket(
                type = PacketType.CHAT,
                senderName = _deviceName.value,
                senderIp = _localIp.value,
                content = text.trim()
            )
            val result = tcpMessenger.sendPacket(target, packet)
            if (result.isFailure) {
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        senderName = "SYSTEM",
                        senderIp = "127.0.0.1",
                        content = "Warning: Failed to deliver message to $target",
                        isMine = false,
                        isSystemNotification = true
                    )
                )
            }
        }
    }

    // Call management
    fun startCall() {
        val target = _targetIp.value
        if (target.isBlank()) {
            _statusMessage.value = "Enter or select target device before calling"
            return
        }

        val targetName = resolveName(target, "Peer ($target)")
        _callState.value = CallState.OutgoingCalling(targetIp = target, targetName = targetName)
        _currentScreen.value = AppScreen.CALL

        viewModelScope.launch {
            val packet = P2PPacket(
                type = PacketType.CALL_INVITE,
                senderName = _deviceName.value,
                senderIp = _localIp.value,
                content = "CALL_OFFER"
            )
            val res = tcpMessenger.sendPacket(target, packet)
            if (res.isFailure) {
                _callState.value = CallState.Idle
                _statusMessage.value = "Could not reach $targetName to place call"
            }
        }
    }

    fun acceptIncomingCall() {
        val incoming = _callState.value as? CallState.IncomingCall ?: return
        val peerIp = incoming.callerIp
        _targetIp.value = peerIp

        viewModelScope.launch {
            val packet = P2PPacket(
                type = PacketType.CALL_ACCEPT,
                senderName = _deviceName.value,
                senderIp = _localIp.value,
                content = "CALL_ACCEPTED"
            )
            tcpMessenger.sendPacket(peerIp, packet)
            beginCallStreams(peerIp, incoming.callerName)
        }
    }

    fun declineIncomingCall() {
        val incoming = _callState.value as? CallState.IncomingCall ?: return
        viewModelScope.launch {
            val packet = P2PPacket(
                type = PacketType.CALL_DECLINE,
                senderName = _deviceName.value,
                senderIp = _localIp.value,
                content = "CALL_DECLINED"
            )
            tcpMessenger.sendPacket(incoming.callerIp, packet)
            _callState.value = CallState.Idle
        }
    }

    fun endCall() {
        val peerIp = when (val state = _callState.value) {
            is CallState.InCall -> state.peerIp
            is CallState.OutgoingCalling -> state.targetIp
            is CallState.IncomingCall -> state.callerIp
            else -> _targetIp.value
        }

        if (peerIp.isNotBlank()) {
            viewModelScope.launch {
                val packet = P2PPacket(
                    type = PacketType.CALL_END,
                    senderName = _deviceName.value,
                    senderIp = _localIp.value,
                    content = "CALL_ENDED"
                )
                tcpMessenger.sendPacket(peerIp, packet)
            }
        }
        stopCallStreams()
        _callState.value = CallState.Idle
    }

    private fun beginCallStreams(peerIp: String, peerName: String) {
        val resolvedName = resolveName(peerIp, peerName)
        _callState.value = CallState.InCall(
            peerIp = peerIp,
            peerName = resolvedName,
            startTime = System.currentTimeMillis()
        )
        _currentScreen.value = AppScreen.CALL
        _remoteVideoFrame.value = null

        // Start Audio and Video streaming pipelines
        audioStreamer.startSending(viewModelScope, peerIp)
        videoStreamer.startSending(viewModelScope, peerIp)

        // Start call duration timer
        callTimerJob?.cancel()
        _callDurationSeconds.value = 0L
        callTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    private fun stopCallStreams() {
        audioStreamer.stop()
        audioStreamer.startReceiving(viewModelScope)
        videoStreamer.stop()
        videoStreamer.startReceiving(viewModelScope)
        callTimerJob?.cancel()
        callTimerJob = null
        _callDurationSeconds.value = 0L
        _remoteVideoFrame.value = null
    }

    fun toggleMute() {
        val inCall = _callState.value as? CallState.InCall ?: return
        val newMute = !inCall.isMuted
        audioStreamer.setMuted(newMute)
        _callState.value = inCall.copy(isMuted = newMute)
    }

    fun toggleVideo() {
        val inCall = _callState.value as? CallState.InCall ?: return
        _callState.value = inCall.copy(isVideoEnabled = !inCall.isVideoEnabled)
    }

    fun toggleCameraLens() {
        val inCall = _callState.value as? CallState.InCall ?: return
        _callState.value = inCall.copy(isFrontCamera = !inCall.isFrontCamera)
    }

    fun processCameraFrame(imageProxy: ImageProxy) {
        val inCall = _callState.value as? CallState.InCall
        if (inCall == null || !inCall.isVideoEnabled) {
            imageProxy.close()
            return
        }

        try {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val jpeg = YuvToJpegConverter.imageProxyToJpeg(imageProxy, quality = 45, rotationDegrees = rotation)
            if (jpeg != null) {
                videoStreamer.sendFrame(jpeg)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error processing camera frame: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun handleIncomingPacket(packet: P2PPacket, remoteIp: String) {
        val senderIp = packet.senderIp.ifBlank { remoteIp }
        val senderResolvedName = resolveName(senderIp, packet.senderName)

        when (packet.type) {
            PacketType.CHAT -> {
                val msg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderName = senderResolvedName,
                    senderIp = senderIp,
                    content = packet.content,
                    isMine = false
                )
                addMessage(msg)
                // Auto-fill target IP if currently empty
                if (_targetIp.value.isBlank()) {
                    _targetIp.value = senderIp
                }
            }
            PacketType.CALL_INVITE -> {
                _callState.value = CallState.IncomingCall(
                    callerIp = senderIp,
                    callerName = senderResolvedName
                )
            }
            PacketType.CALL_ACCEPT -> {
                beginCallStreams(senderIp, senderResolvedName)
            }
            PacketType.CALL_DECLINE -> {
                _callState.value = CallState.Idle
                _statusMessage.value = "Call declined by $senderResolvedName"
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        senderName = "SYSTEM",
                        senderIp = "127.0.0.1",
                        content = "Call was declined by $senderResolvedName.",
                        isMine = false,
                        isSystemNotification = true
                    )
                )
            }
            PacketType.CALL_END -> {
                stopCallStreams()
                _callState.value = CallState.Idle
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        senderName = "SYSTEM",
                        senderIp = "127.0.0.1",
                        content = "Call ended by $senderResolvedName.",
                        isMine = false,
                        isSystemNotification = true
                    )
                )
            }
            PacketType.PING -> {
                viewModelScope.launch {
                    val pong = P2PPacket(
                        type = PacketType.PONG,
                        senderName = _deviceName.value,
                        senderIp = _localIp.value,
                        content = "PONG"
                    )
                    tcpMessenger.sendPacket(remoteIp, pong)
                }
            }
            PacketType.PONG -> {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _statusMessage.value = "Connected to $senderResolvedName ($senderIp)"
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        tcpMessenger.stop()
        audioStreamer.stop()
        videoStreamer.stop()
    }
}
