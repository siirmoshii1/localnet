package com.example.ui.components

import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model.CallState
import com.example.ui.theme.AccentColorPreset
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TrueBlack
import java.util.concurrent.Executors

@Composable
fun VideoCallView(
    callState: CallState,
    targetIp: String,
    remoteFrame: Bitmap?,
    callDurationSeconds: Long,
    accentPreset: AccentColorPreset,
    hasCameraPermission: Boolean,
    hasAudioPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onStartCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleCameraLens: () -> Unit,
    onFrameCaptured: (ImageProxy) -> Unit
) {
    val accent = accentPreset.color
    var isFrontCamera by remember { mutableStateOf(true) }
    var isLocalPipExpanded by remember { mutableStateOf(false) }

    val formattedDuration = remember(callDurationSeconds) {
        val minutes = callDurationSeconds / 60
        val seconds = callDurationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        when (callState) {
            is CallState.Idle -> {
                // Idle Screen - Call launcher & camera test
                IdleCallPane(
                    targetIp = targetIp,
                    accent = accent,
                    hasCameraPermission = hasCameraPermission,
                    hasAudioPermission = hasAudioPermission,
                    isFrontCamera = isFrontCamera,
                    onRequestPermissions = onRequestPermissions,
                    onStartCall = onStartCall,
                    onToggleCamera = { isFrontCamera = !isFrontCamera },
                    onFrameCaptured = onFrameCaptured
                )
            }

            is CallState.OutgoingCalling -> {
                // Outgoing Calling Screen
                CallingOverlay(
                    status = "DIALING PEER...",
                    peerText = callState.targetName,
                    accent = accent,
                    onCancel = onEndCall
                )
            }

            is CallState.IncomingCall -> {
                // Incoming Call Overlay
                CallingOverlay(
                    status = "INCOMING CALL...",
                    peerText = "${callState.callerName} (${callState.callerIp})",
                    accent = accent,
                    onCancel = onEndCall
                )
            }

            is CallState.InCall -> {
                // Active Call View (Remote Video + Local Camera PiP + Call HUD)
                ActiveCallLayout(
                    inCall = callState,
                    remoteFrame = remoteFrame,
                    accent = accent,
                    formattedDuration = formattedDuration,
                    hasCameraPermission = hasCameraPermission,
                    isLocalPipExpanded = isLocalPipExpanded,
                    onTogglePipExpanded = { isLocalPipExpanded = !isLocalPipExpanded },
                    onEndCall = onEndCall,
                    onToggleMute = onToggleMute,
                    onToggleVideo = onToggleVideo,
                    onToggleCameraLens = {
                        isFrontCamera = !isFrontCamera
                        onToggleCameraLens()
                    },
                    onFrameCaptured = onFrameCaptured
                )
            }
        }
    }
}

@Composable
private fun ActiveCallLayout(
    inCall: CallState.InCall,
    remoteFrame: Bitmap?,
    accent: Color,
    formattedDuration: String,
    hasCameraPermission: Boolean,
    isLocalPipExpanded: Boolean,
    onTogglePipExpanded: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleCameraLens: () -> Unit,
    onFrameCaptured: (ImageProxy) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Remote Video Feed (Main Canvas)
        if (remoteFrame != null) {
            Image(
                bitmap = remoteFrame.asImageBitmap(),
                contentDescription = "Remote Video Stream",
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("remote_video_display"),
                contentScale = ContentScale.Crop
            )
        } else {
            // Waiting for Remote Video Stream Placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070707)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val transition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = pulseAlpha * 0.3f))
                            .border(2.dp, accent.copy(alpha = pulseAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Connecting Video",
                            tint = accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "LIVE AUDIO CONNECTED",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Receiving stream from ${inCall.peerIp}...",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }
        }

        // Top Header info HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Peer info pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TrueBlack.copy(alpha = 0.8f))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FF66))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${inCall.peerName} [${inCall.peerIp}]",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }

            // Duration Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TrueBlack.copy(alpha = 0.8f))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = formattedDuration,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }

        // 2. Local Camera Preview (PiP overlay in bottom-right)
        if (hasCameraPermission && inCall.isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 16.dp)
                    .size(if (isLocalPipExpanded) 180.dp else 120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(2.dp, accent, RoundedCornerShape(12.dp))
                    .clickable { onTogglePipExpanded() }
                    .testTag("local_camera_pip")
            ) {
                CameraPreviewSurface(
                    isFrontCamera = inCall.isFrontCamera,
                    onFrameCaptured = onFrameCaptured
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TrueBlack.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "YOU",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Floating Bottom Control HUD Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(TrueBlack.copy(alpha = 0.9f))
                .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute / Unmute
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (inCall.isMuted) Color(0xFFFF453A) else DarkElevated)
                    .testTag("toggle_mute_button")
            ) {
                Icon(
                    imageVector = if (inCall.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Toggle Mute",
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Video On / Off
            IconButton(
                onClick = onToggleVideo,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (!inCall.isVideoEnabled) Color(0xFFFF453A) else DarkElevated)
                    .testTag("toggle_video_button")
            ) {
                Icon(
                    imageVector = if (inCall.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Toggle Video",
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Switch Camera Front / Back
            IconButton(
                onClick = onToggleCameraLens,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkElevated)
                    .testTag("switch_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Floating END CALL button
            Button(
                onClick = onEndCall,
                modifier = Modifier
                    .height(44.dp)
                    .testTag("end_call_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF453A),
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "END",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun IdleCallPane(
    targetIp: String,
    accent: Color,
    hasCameraPermission: Boolean,
    hasAudioPermission: Boolean,
    isFrontCamera: Boolean,
    onRequestPermissions: () -> Unit,
    onStartCall: () -> Unit,
    onToggleCamera: () -> Unit,
    onFrameCaptured: (ImageProxy) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Camera Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    CameraPreviewSurface(
                        isFrontCamera = isFrontCamera,
                        onFrameCaptured = onFrameCaptured
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CAMERA PERMISSION REQUIRED",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onRequestPermissions,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = TrueBlack
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("GRANT PERMISSIONS", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (hasCameraPermission) {
                    IconButton(
                        onClick = onToggleCamera,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TrueBlack.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PEER-TO-PEER VIDEO & AUDIO",
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (targetIp.isNotBlank()) "Target: $targetIp:8888" else "Set Target IP in Config first",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = if (targetIp.isNotBlank()) accent else Color(0xFFFF453A)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartCall,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(48.dp)
                .testTag("start_call_big_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = TrueBlack
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = targetIp.isNotBlank() && hasCameraPermission && hasAudioPermission
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Start Video Call",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "START P2P CALL",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        if (!hasCameraPermission || !hasAudioPermission) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "! Camera and Audio permissions are required to place calls.",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun CallingOverlay(
    status: String,
    peerText: String,
    accent: Color,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val transition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = pulseAlpha * 0.25f))
                    .border(2.dp, accent.copy(alpha = pulseAlpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Calling",
                    tint = accent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = status,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = peerText,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = accent
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF453A),
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("cancel_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Cancel",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CANCEL",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * CameraX Preview Surface using AndroidView and ProcessCameraProvider
 */
@Composable
fun CameraPreviewSurface(
    isFrontCamera: Boolean,
    onFrameCaptured: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(cameraExecutor) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(320, 240))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    onFrameCaptured(imageProxy)
                }

                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    // Fallback to back camera if front camera fails
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (ignored: Exception) {}
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        update = { previewView ->
            // Update camera selector on lens flip
            val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(320, 240))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    onFrameCaptured(imageProxy)
                }

                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (ignored: Exception) {}
            }, ContextCompat.getMainExecutor(previewView.context))
        },
        modifier = Modifier.fillMaxSize()
    )
}
