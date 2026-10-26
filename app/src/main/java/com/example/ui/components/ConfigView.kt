package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.ConnectionStatus
import com.example.model.DiscoveredPeer
import com.example.network.NetworkUtils
import com.example.ui.theme.AccentColorPreset
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TrueBlack

@Composable
fun ConfigView(
    localIp: String,
    targetIp: String,
    deviceName: String,
    connectionStatus: ConnectionStatus,
    statusMessage: String,
    accentPreset: AccentColorPreset,
    discoveredPeers: List<DiscoveredPeer>,
    onTargetIpChanged: (String) -> Unit,
    onConnectClicked: () -> Unit,
    onRefreshLocalIp: () -> Unit,
    onScanPeers: () -> Unit,
    onNavigateToChat: () -> Unit,
    onStartCall: () -> Unit,
    onSavePeerAsContact: (DiscoveredPeer) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val accent = accentPreset.color
    var targetInput by remember(targetIp) { mutableStateOf(targetIp) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Local IP Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .testTag("local_ip_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Local Wi-Fi",
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CURRENT DEVICE LOCAL IP",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onRefreshLocalIp,
                            modifier = Modifier.size(28.dp).testTag("refresh_ip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Local IP",
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localIp,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Local IP", localIp)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "IP copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_ip_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PureWhite
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy IP",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "COPY",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkElevated, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PORT: ${NetworkUtils.TCP_PORT} (LISTENING)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = accent
                        )
                        Text(
                            text = "NAME: $deviceName",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Target Device Input Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .testTag("target_ip_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TARGET DEVICE IP ADDRESS",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = {
                            targetInput = it
                            onTargetIpChanged(it)
                        },
                        placeholder = {
                            Text(
                                text = "e.g. 192.168.1.55",
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_ip_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                onConnectClicked()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkElevated,
                            unfocusedContainerColor = DarkElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onConnectClicked()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("connect_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = TrueBlack
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (connectionStatus == ConnectionStatus.CONNECTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = TrueBlack,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PINGING...",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.WifiTethering,
                                    contentDescription = "Connect",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CONNECT",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onStartCall,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_from_config_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PureWhite
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(8.dp),
                            enabled = targetInput.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Start Video Call",
                                tint = if (targetInput.isNotBlank()) accent else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CALL",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status notification banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkElevated)
                            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> Color(0xFF00FF66)
                                ConnectionStatus.CONNECTING -> Color(0xFFFFB800)
                                ConnectionStatus.ERROR -> Color(0xFFFF453A)
                                ConnectionStatus.DISCONNECTED -> TextMuted
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMessage,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (connectionStatus == ConnectionStatus.ERROR) Color(0xFFFF453A) else PureWhite
                            )
                        }
                    }
                }
            }
        }

        // Quick Jump to Chat Button (if configured)
        if (targetInput.isNotBlank()) {
            item {
                Button(
                    onClick = onNavigateToChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jump_to_chat_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkElevated,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Open Chat",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPEN CHAT WITH $targetInput",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Discovered Local Peers Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISCOVERED ON LOCAL WI-FI (${discoveredPeers.size})",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onScanPeers,
                    modifier = Modifier.size(24.dp).testTag("beacon_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Broadcast Beacon",
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (discoveredPeers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO LOCAL PEERS DETECTED YET",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ensure other device is on the same Wi-Fi with GhostLink open, or enter Target IP above.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF666666),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(discoveredPeers) { peer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            targetInput = peer.ip
                            onTargetIpChanged(peer.ip)
                            onConnectClicked()
                        }
                        .padding(14.dp)
                        .testTag("discovered_peer_${peer.ip.replace(".", "_")}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peer.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                        Text(
                            text = peer.ip,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = accent
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onSavePeerAsContact(peer) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accent),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "+ SAVE",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // How it works info
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "HOME NETWORK ONLY // ARCHITECTURE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Text(
                        text = "GhostLink uses direct socket listeners on this device. Text messages and call signals use TCP port 8888. Live video streams over TCP port 8890, and audio streams over UDP port 8892. Zero external dependencies.",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
