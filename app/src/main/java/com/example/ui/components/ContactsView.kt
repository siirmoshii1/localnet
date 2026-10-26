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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DeviceContact
import com.example.model.DiscoveredPeer
import com.example.ui.theme.AccentColorPreset
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TrueBlack

@Composable
fun ContactsView(
    contacts: List<DeviceContact>,
    discoveredPeers: List<DiscoveredPeer>,
    accentPreset: AccentColorPreset,
    onSelectContactForChat: (DeviceContact) -> Unit,
    onSelectContactForCall: (DeviceContact) -> Unit,
    onSaveContact: (name: String, ip: String, note: String) -> Unit,
    onDeleteContact: (String) -> Unit
) {
    val accent = accentPreset.color
    var searchQuery by remember { mutableStateOf("") }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var prefilledIp by remember { mutableStateOf("") }
    var prefilledName by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.ip.contains(searchQuery, ignoreCase = true) ||
                    it.note.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(16.dp)
    ) {
        // Search & Add Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search by device name or IP...",
                        fontSize = 13.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_contacts_input"),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Button(
                onClick = {
                    prefilledIp = ""
                    prefilledName = ""
                    isAddDialogOpen = true
                },
                modifier = Modifier
                    .height(52.dp)
                    .testTag("add_contact_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = TrueBlack
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Contact",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ADD",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Device Contacts List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVED DEVICES (${filteredContacts.size})",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "FAST 1-TAP CONNECT",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = accent
                    )
                }
            }

            if (filteredContacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(10.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = "No Devices",
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "NO SAVED DEVICE CONTACTS",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ ADD' or save discovered devices below for instant calling and chat identification.",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextDim,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredContacts, key = { it.id }) { contact ->
                    ContactCardItem(
                        contact = contact,
                        accent = accent,
                        onChat = { onSelectContactForChat(contact) },
                        onCall = { onSelectContactForCall(contact) },
                        onDelete = { onDeleteContact(contact.id) }
                    )
                }
            }

            // Discovered Peers Section
            if (discoveredPeers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "DISCOVERED ON LOCAL WI-FI (AUTO-DETECTED)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(discoveredPeers) { peer ->
                    val isAlreadySaved = contacts.any { it.ip == peer.ip }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = peer.name,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Text(
                                text = peer.ip,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = accent
                            )
                        }

                        if (!isAlreadySaved) {
                            OutlinedButton(
                                onClick = {
                                    prefilledIp = peer.ip
                                    prefilledName = peer.name
                                    isAddDialogOpen = true
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "Save Contact",
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SAVE",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "SAVED ✓",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Contact Dialog Modal
    if (isAddDialogOpen) {
        AddContactDialog(
            initialName = prefilledName,
            initialIp = prefilledIp,
            accent = accent,
            onDismiss = { isAddDialogOpen = false },
            onSave = { name, ip, note ->
                onSaveContact(name, ip, note)
                isAddDialogOpen = false
            }
        )
    }
}

@Composable
private fun ContactCardItem(
    contact: DeviceContact,
    accent: Color,
    onChat: () -> Unit,
    onCall: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .testTag("contact_item_${contact.ip.replace(".", "_")}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Initial Circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DarkElevated)
                    .border(1.5.dp, accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = PureWhite
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Text(
                    text = contact.ip,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = accent,
                    fontWeight = FontWeight.Medium
                )
                if (contact.note.isNotBlank()) {
                    Text(
                        text = contact.note,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // One-tap Chat button
                IconButton(
                    onClick = onChat,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .testTag("contact_chat_${contact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat with ${contact.name}",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // One-tap Call button
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent)
                        .testTag("contact_call_${contact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call ${contact.name}",
                        tint = TrueBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("contact_delete_${contact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete contact",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    initialName: String,
    initialIp: String,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (name: String, ip: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var ip by remember { mutableStateOf(initialIp) }
    var note by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, accent, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "SAVE DEVICE CONTACT",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Name Input
                Text(
                    text = "DEVICE NAME",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = {
                        Text("e.g. Living Room TV, Alice Phone", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_name_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkElevated,
                        unfocusedContainerColor = DarkElevated
                    ),
                    shape = RoundedCornerShape(6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // IP Input
                Text(
                    text = "DEVICE IP ADDRESS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    placeholder = {
                        Text("e.g. 192.168.1.42", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_ip_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkElevated,
                        unfocusedContainerColor = DarkElevated
                    ),
                    shape = RoundedCornerShape(6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note Input
                Text(
                    text = "NOTE / LOCATION (OPTIONAL)",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = {
                        Text("e.g. Upstairs Bedroom", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_note_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkElevated,
                        unfocusedContainerColor = DarkElevated
                    ),
                    shape = RoundedCornerShape(6.dp)
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        color = Color(0xFFFF453A),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text("CANCEL", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorText = "Please enter device name"
                                return@Button
                            }
                            if (ip.isBlank()) {
                                errorText = "Please enter IP address"
                                return@Button
                            }
                            onSave(name, ip, note)
                        },
                        modifier = Modifier.weight(1f).testTag("save_contact_submit_button"),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = TrueBlack
                        )
                    ) {
                        Text("SAVE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
