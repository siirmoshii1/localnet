package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppScreen
import com.example.model.CallState
import com.example.ui.P2PViewModel
import com.example.ui.components.ChatView
import com.example.ui.components.ConfigView
import com.example.ui.components.ContactsView
import com.example.ui.components.IncomingCallDialog
import com.example.ui.components.SettingsModal
import com.example.ui.components.TopNavBar
import com.example.ui.components.VideoCallView
import com.example.ui.theme.GhostLinkTheme
import com.example.ui.theme.TrueBlack

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: P2PViewModel = viewModel()
            val accentPreset by viewModel.accentPreset.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val localIp by viewModel.localIp.collectAsStateWithLifecycle()
            val targetIp by viewModel.targetIp.collectAsStateWithLifecycle()
            val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
            val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
            val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
            val messages by viewModel.messages.collectAsStateWithLifecycle()
            val contacts by viewModel.contacts.collectAsStateWithLifecycle()
            val discoveredPeers by viewModel.discoveredPeers.collectAsStateWithLifecycle()
            val callState by viewModel.callState.collectAsStateWithLifecycle()
            val remoteVideoFrame by viewModel.remoteVideoFrame.collectAsStateWithLifecycle()
            val callDurationSeconds by viewModel.callDurationSeconds.collectAsStateWithLifecycle()

            var isSettingsOpen by remember { mutableStateOf(false) }

            // Permission handling
            var hasCameraPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                )
            }
            var hasAudioPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                hasCameraPermission = permissions[Manifest.permission.CAMERA] == true ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            }

            LaunchedEffect(Unit) {
                if (!hasCameraPermission || !hasAudioPermission) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                }
            }

            // Back handler logic
            BackHandler(enabled = currentScreen != AppScreen.CONTACTS || callState !is CallState.Idle) {
                if (callState is CallState.InCall) {
                    viewModel.endCall()
                } else if (callState is CallState.OutgoingCalling) {
                    viewModel.endCall()
                } else if (currentScreen != AppScreen.CONTACTS) {
                    viewModel.setScreen(AppScreen.CONTACTS)
                }
            }

            val targetResolvedName = remember(targetIp, contacts) {
                viewModel.resolveName(targetIp, "")
            }

            GhostLinkTheme(accentPreset = accentPreset) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TrueBlack)
                        .safeDrawingPadding(),
                    containerColor = TrueBlack,
                    topBar = {
                        TopNavBar(
                            currentScreen = currentScreen,
                            connectionStatus = connectionStatus,
                            targetIp = targetIp,
                            targetName = targetResolvedName,
                            accentPreset = accentPreset,
                            onScreenSelected = { screen -> viewModel.setScreen(screen) },
                            onOpenSettings = { isSettingsOpen = true }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(TrueBlack)
                    ) {
                        when (currentScreen) {
                            AppScreen.CONTACTS -> {
                                ContactsView(
                                    contacts = contacts,
                                    discoveredPeers = discoveredPeers,
                                    accentPreset = accentPreset,
                                    onSelectContactForChat = { contact ->
                                        viewModel.selectContactForChat(contact)
                                    },
                                    onSelectContactForCall = { contact ->
                                        viewModel.selectContactForCall(contact)
                                    },
                                    onSaveContact = { name, ip, note ->
                                        viewModel.saveContact(name, ip, note)
                                    },
                                    onDeleteContact = { id ->
                                        viewModel.deleteContact(id)
                                    }
                                )
                            }

                            AppScreen.CONFIG -> {
                                ConfigView(
                                    localIp = localIp,
                                    targetIp = targetIp,
                                    deviceName = deviceName,
                                    connectionStatus = connectionStatus,
                                    statusMessage = statusMessage,
                                    accentPreset = accentPreset,
                                    discoveredPeers = discoveredPeers,
                                    onTargetIpChanged = { viewModel.setTargetIp(it) },
                                    onConnectClicked = { viewModel.testConnection() },
                                    onRefreshLocalIp = { viewModel.refreshLocalIp() },
                                    onScanPeers = { viewModel.broadcastPresence() },
                                    onNavigateToChat = { viewModel.setScreen(AppScreen.CHAT) },
                                    onStartCall = { viewModel.startCall() },
                                    onSavePeerAsContact = { peer ->
                                        viewModel.saveDiscoveredPeerAsContact(peer)
                                    }
                                )
                            }

                            AppScreen.CHAT -> {
                                ChatView(
                                    messages = messages,
                                    targetIp = targetIp,
                                    accentPreset = accentPreset,
                                    onSendMessage = { viewModel.sendChatMessage(it) },
                                    onStartCall = { viewModel.startCall() },
                                    onClearChat = { viewModel.clearChat() },
                                    onOpenConfig = { viewModel.setScreen(AppScreen.CONFIG) }
                                )
                            }

                            AppScreen.CALL -> {
                                VideoCallView(
                                    callState = callState,
                                    targetIp = targetIp,
                                    remoteFrame = remoteVideoFrame,
                                    callDurationSeconds = callDurationSeconds,
                                    accentPreset = accentPreset,
                                    hasCameraPermission = hasCameraPermission,
                                    hasAudioPermission = hasAudioPermission,
                                    onRequestPermissions = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.CAMERA,
                                                Manifest.permission.RECORD_AUDIO
                                            )
                                        )
                                    },
                                    onStartCall = { viewModel.startCall() },
                                    onEndCall = { viewModel.endCall() },
                                    onToggleMute = { viewModel.toggleMute() },
                                    onToggleVideo = { viewModel.toggleVideo() },
                                    onToggleCameraLens = { viewModel.toggleCameraLens() },
                                    onFrameCaptured = { imageProxy ->
                                        viewModel.processCameraFrame(imageProxy)
                                    }
                                )
                            }
                        }

                        // Incoming Call Dialog Modal
                        (callState as? CallState.IncomingCall)?.let { incoming ->
                            IncomingCallDialog(
                                callerName = incoming.callerName,
                                callerIp = incoming.callerIp,
                                accentColor = accentPreset.color,
                                onAccept = { viewModel.acceptIncomingCall() },
                                onDecline = { viewModel.declineIncomingCall() }
                            )
                        }

                        // Settings Bottom Sheet
                        if (isSettingsOpen) {
                            SettingsModal(
                                currentPreset = accentPreset,
                                deviceName = deviceName,
                                onPresetSelected = { viewModel.setAccentPreset(it) },
                                onDeviceNameChanged = { viewModel.setDeviceName(it) },
                                onDismiss = { isSettingsOpen = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
