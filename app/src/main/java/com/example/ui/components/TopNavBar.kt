package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.ConnectionStatus
import com.example.ui.theme.AccentColorPreset
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TrueBlack

@Composable
fun TopNavBar(
    currentScreen: AppScreen,
    connectionStatus: ConnectionStatus,
    targetIp: String,
    targetName: String = "",
    accentPreset: AccentColorPreset,
    unreadCount: Int = 0,
    onScreenSelected: (AppScreen) -> Unit,
    onOpenSettings: () -> Unit
) {
    val accent = accentPreset.color

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TrueBlack)
    ) {
        // Upper Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Identity & Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status dot
                val dotColor = when (connectionStatus) {
                    ConnectionStatus.CONNECTED -> Color(0xFF00FF66)
                    ConnectionStatus.CONNECTING -> Color(0xFFFFB800)
                    ConnectionStatus.ERROR -> Color(0xFFFF453A)
                    ConnectionStatus.DISCONNECTED -> TextMuted
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                Column {
                    Text(
                        text = "GHOSTLINK",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = PureWhite
                    )
                    val peerLabel = when {
                        targetName.isNotBlank() && targetName != targetIp -> "PEER: $targetName ($targetIp)"
                        targetIp.isNotBlank() -> "PEER: $targetIp"
                        else -> "STANDALONE MESH"
                    }
                    Text(
                        text = peerLabel,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }

            // Right controls: Accent Chip & Settings Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Accent selector chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("accent_color_chip"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = accentPreset.name.take(4),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Screen Segmented Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppScreen.values().forEach { screen ->
                val isSelected = screen == currentScreen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) DarkElevated else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) accent else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onScreenSelected(screen) }
                        .padding(vertical = 7.dp)
                        .testTag("nav_tab_${screen.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = screen.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) accent else TextMuted
                        )
                    }
                }
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DarkBorder)
        )
    }
}
