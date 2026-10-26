package com.example.ui.components

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.ui.theme.AccentColorPreset
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TrueBlack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatView(
    messages: List<ChatMessage>,
    targetIp: String,
    accentPreset: AccentColorPreset,
    onSendMessage: (String) -> Unit,
    onStartCall: () -> Unit,
    onClearChat: () -> Unit,
    onOpenConfig: () -> Unit
) {
    val accent = accentPreset.color
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .imePadding()
    ) {
        // Chat Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (targetIp.isNotBlank()) "TARGET: $targetIp" else "NO TARGET IP",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (targetIp.isNotBlank()) accent else Color(0xFFFF453A)
                )
                Text(
                    text = "P2P DIRECT TCP SOCKET [8888]",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (targetIp.isNotBlank()) {
                    IconButton(
                        onClick = onStartCall,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("chat_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Start Video Call",
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DarkBorder)
        )

        // If no target IP set, warn user
        if (targetIp.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF221111))
                    .border(1.dp, Color(0xFF442222))
                    .clickable { onOpenConfig() }
                    .padding(12.dp)
            ) {
                Text(
                    text = "! Target IP not set. Tap here to configure Target IP first.",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFF7777),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "P2P TEXT STREAM READY",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "All text travels directly over local Wi-Fi via TCP.",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextDim
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatMessageItem(message = message, accentPreset = accentPreset)
            }
        }

        // Quick suggestions row
        val quickPrompts = listOf("Ping!", "Online on Wi-Fi?", "Start video call?", "Got your IP")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            if (targetIp.isNotBlank()) {
                                onSendMessage(prompt)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PureWhite
                    )
                }
            }
        }

        // Message Input Field Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = if (targetIp.isNotBlank()) "Type message for $targetIp..." else "Set Target IP in Config...",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput)
                            textInput = ""
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkElevated,
                    unfocusedContainerColor = DarkElevated
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_message_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = TrueBlack
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                enabled = textInput.isNotBlank() && targetIp.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    accentPreset: AccentColorPreset
) {
    val accent = accentPreset.color
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    if (message.isSystemNotification) {
        // System banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkElevated)
                    .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "[SYS] ${message.content}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    val isMine = message.isMine
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        // Sender and Timestamp Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isMine) "YOU" else message.senderName.uppercase(),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isMine) accent else PureWhite
            )
            Text(
                text = formattedTime,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextDim
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Bubble
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (isMine) 8.dp else 2.dp,
                        bottomEnd = if (isMine) 2.dp else 8.dp
                    )
                )
                .background(if (isMine) DarkElevated else DarkSurface)
                .border(
                    width = 1.dp,
                    color = if (isMine) accent.copy(alpha = 0.6f) else DarkBorder,
                    shape = RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (isMine) 8.dp else 2.dp,
                        bottomEnd = if (isMine) 2.dp else 8.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.content,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = PureWhite,
                lineHeight = 18.sp
            )
        }
    }
}
