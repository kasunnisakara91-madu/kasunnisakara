package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Contact
import com.example.data.model.Conversation
import com.example.data.model.DirectoryProfile
import com.example.data.model.UserProfile
import com.example.ui.theme.AvatarPalette
import com.example.ui.theme.CriminalCrimson
import com.example.ui.theme.CriminalCyan
import com.example.ui.theme.CriminalEmerald
import com.example.util.CriminalMediaAndNotificationHelper

@Composable
fun MainChatsScreen(
    currentUserId: String,
    myProfile: UserProfile?,
    conversations: List<Conversation>,
    directoryUsers: List<DirectoryProfile>,
    onConversationClick: (Conversation) -> Unit,
    onOpenContactsScreen: () -> Unit,
    onOpenGroupsScreen: () -> Unit,
    onStartSelfNotesChat: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, DIRECT, GROUPS

    val filteredConversations = remember(conversations, searchQuery, selectedFilter) {
        conversations.filter { conv ->
            val matchesFilter = when (selectedFilter) {
                "DIRECT" -> !conv.isGroup
                "GROUPS" -> conv.isGroup
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                conv.title.contains(searchQuery, ignoreCase = true) ||
                conv.participantNames.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessageText.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search & Quick Actions Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conversations, messages, or members...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search chats") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("chats_search_input")
            )

            // Filter Chips + Quick Contacts/Groups Buttons
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${conversations.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "DIRECT",
                        onClick = { selectedFilter = "DIRECT" },
                        label = { Text("Direct") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "GROUPS",
                        onClick = { selectedFilter = "GROUPS" },
                        label = { Text("Groups (${conversations.count { it.isGroup }})") }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = onOpenContactsScreen,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_contacts_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contacts (${directoryUsers.size})")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onOpenGroupsScreen,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_groups_button")
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Groups Hub")
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Empty Chats",
                                tint = CriminalEmerald,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Active Conversations Yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Start a private 1-to-1 encrypted chat, create a Group with administrators, or open your Personal Vault chat.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onOpenContactsScreen,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CriminalEmerald,
                                        contentColor = Color(0xFF002110)
                                    ),
                                    modifier = Modifier.testTag("empty_find_contacts_button")
                                ) {
                                    Text("Find Contacts")
                                }
                                OutlinedButton(
                                    onClick = onStartSelfNotesChat,
                                    modifier = Modifier.testTag("start_vault_chat_button")
                                ) {
                                    Text("Personal Vault")
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("conversations_list")
                ) {
                    items(filteredConversations, key = { it.id }) { conv ->
                        ConversationRowItem(
                            conversation = conv,
                            currentUserId = currentUserId,
                            directoryUsers = directoryUsers,
                            onClick = { onConversationClick(conv) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenContactsScreen,
            containerColor = CriminalEmerald,
            contentColor = Color(0xFF002110),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("new_chat_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Start New Chat")
        }
    }
}

@Composable
private fun ConversationRowItem(
    conversation: Conversation,
    currentUserId: String,
    directoryUsers: List<DirectoryProfile>,
    onClick: () -> Unit
) {
    val peerUid = remember(conversation, currentUserId) {
        conversation.participantIds.firstOrNull { it != currentUserId } ?: currentUserId
    }
    val peerDir = remember(directoryUsers, peerUid) {
        directoryUsers.firstOrNull { it.userId == peerUid }
    }
    val displayTitle = remember(conversation, peerDir) {
        if (conversation.isGroup) conversation.title
        else peerDir?.displayName?.ifBlank { conversation.title } ?: conversation.title
    }
    val isPeerOnline = !conversation.isGroup && (peerDir?.isOnline == true)
    val isSomeoneTyping = conversation.typingUserId.isNotBlank() && conversation.typingUserId != currentUserId

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
            .testTag("conversation_item_${conversation.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Avatar with Online Dot
            Box(modifier = Modifier.size(54.dp)) {
                val avatarBmp = remember(peerDir?.avatarBase64, conversation.groupIconBase64) {
                    CriminalMediaAndNotificationHelper.decodeBase64ToImageBitmap(
                        if (conversation.isGroup) conversation.groupIconBase64 else peerDir?.avatarBase64
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(CriminalMediaAndNotificationHelper.parseColorSafe(conversation.avatarColor))
                ) {
                    if (avatarBmp != null) {
                        Image(
                            bitmap = avatarBmp,
                            contentDescription = displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (conversation.isGroup) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Group",
                            tint = Color(0xFF002110),
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Text(
                            text = displayTitle.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF002110),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isPeerOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(CriminalEmerald)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conversation.isGroup) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CriminalCyan.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "GROUP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CriminalCyan
                                )
                            }
                        }
                    }

                    Text(
                        text = CriminalMediaAndNotificationHelper.formatMessageTime(conversation.updatedAt ?: conversation.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSomeoneTyping) {
                        Text(
                            text = "${conversation.typingUserName.ifBlank { "Someone" }} is typing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CriminalEmerald,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        if (conversation.lastMessageSenderId == currentUserId) {
                            Icon(
                                imageVector = if (conversation.lastMessageStatus == "SENT") Icons.Default.Check else Icons.Default.DoneAll,
                                contentDescription = "Delivery Status",
                                tint = if (conversation.lastMessageStatus == "READ") CriminalCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = conversation.lastMessageText.ifBlank { "Tap to chat" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (conversation.unreadCount > 0L) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CriminalEmerald)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF002110),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsDirectoryScreen(
    currentUserId: String,
    myProfile: UserProfile?,
    directoryUsers: List<DirectoryProfile>,
    savedContacts: List<Contact>,
    onBack: () -> Unit,
    onAddContact: (
        contactUid: String,
        username: String,
        displayName: String,
        phoneTag: String,
        bio: String,
        avatarColor: String
    ) -> Unit,
    onStartChatWithUser: (peerUid: String, peerName: String, peerColor: String) -> Unit,
    onStartCallWithUser: (peerUid: String, peerName: String, peerColor: String, callType: String) -> Unit,
    onToggleBlockUser: (targetUid: String) -> Unit,
    onReportUser: (targetUid: String, targetUsername: String, reason: String, details: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var reportingTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    val blockedIds = myProfile?.blockedUserIds ?: emptyList()

    val filteredDirectory = remember(directoryUsers, searchQuery) {
        directoryUsers.filter { user ->
            searchQuery.isBlank() ||
                user.username.contains(searchQuery, ignoreCase = true) ||
                user.displayName.contains(searchQuery, ignoreCase = true) ||
                user.phoneTag.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredSaved = remember(savedContacts, searchQuery) {
        savedContacts.filter { c ->
            searchQuery.isBlank() ||
                c.username.contains(searchQuery, ignoreCase = true) ||
                c.displayName.contains(searchQuery, ignoreCase = true) ||
                c.phoneTag.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Contacts & User Directory",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Search by @username or phone number",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                Button(
                    onClick = { showAddManualDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CriminalEmerald,
                        contentColor = Color(0xFF002110)
                    ),
                    modifier = Modifier.testTag("add_new_contact_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onBack) {
                    Text("Done")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search @username or +phone number...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search directory") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contacts_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredSaved.isNotEmpty()) {
                item {
                    Text(
                        text = "MY SAVED CONTACTS (${filteredSaved.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = CriminalEmerald,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(filteredSaved, key = { "saved_${it.id}" }) { contact ->
                    val isBlocked = contact.contactUid in blockedIds
                    ContactCard(
                        uid = contact.contactUid,
                        username = contact.username,
                        displayName = contact.displayName,
                        phoneTag = contact.phoneTag,
                        bio = contact.bio,
                        avatarColor = contact.avatarColor,
                        isOnline = true,
                        isSaved = true,
                        isBlocked = isBlocked,
                        onSaveContact = {},
                        onChat = {
                            onStartChatWithUser(contact.contactUid, contact.displayName, contact.avatarColor)
                        },
                        onVoiceCall = {
                            onStartCallWithUser(contact.contactUid, contact.displayName, contact.avatarColor, "VOICE")
                        },
                        onVideoCall = {
                            onStartCallWithUser(contact.contactUid, contact.displayName, contact.avatarColor, "VIDEO")
                        },
                        onToggleBlock = { onToggleBlockUser(contact.contactUid) },
                        onReport = { reportingTarget = contact.contactUid to contact.username }
                    )
                }
            }

            item {
                Text(
                    text = "GLOBAL CRIMINAL APP DIRECTORY (${filteredDirectory.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = CriminalCyan,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(filteredDirectory, key = { "dir_${it.userId}" }) { user ->
                val isSaved = savedContacts.any { it.contactUid == user.userId }
                val isBlocked = user.userId in blockedIds
                val isSelf = user.userId == currentUserId

                ContactCard(
                    uid = user.userId,
                    username = user.username,
                    displayName = if (isSelf) "${user.displayName} (You)" else user.displayName,
                    phoneTag = user.phoneTag,
                    bio = user.bio,
                    avatarColor = user.avatarColor,
                    isOnline = user.isOnline,
                    isSaved = isSaved || isSelf,
                    isBlocked = isBlocked,
                    onSaveContact = {
                        onAddContact(
                            user.userId,
                            user.username,
                            user.displayName,
                            user.phoneTag,
                            user.bio,
                            user.avatarColor
                        )
                    },
                    onChat = {
                        onStartChatWithUser(user.userId, user.displayName, user.avatarColor)
                    },
                    onVoiceCall = {
                        onStartCallWithUser(user.userId, user.displayName, user.avatarColor, "VOICE")
                    },
                    onVideoCall = {
                        onStartCallWithUser(user.userId, user.displayName, user.avatarColor, "VIDEO")
                    },
                    onToggleBlock = { onToggleBlockUser(user.userId) },
                    onReport = { reportingTarget = user.userId to user.username }
                )
            }
        }
    }

    if (showAddManualDialog) {
        var handleInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }
        var phoneInput by remember { mutableStateOf("+94 ") }
        var bioInput by remember { mutableStateOf("Trusted CRIMINAL APP Contact") }

        AlertDialog(
            onDismissRequest = { showAddManualDialog = false },
            title = { Text("Add New Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = handleInput,
                        onValueChange = { handleInput = it.lowercase().replace(" ", "_") },
                        label = { Text("@username (min 3 chars)") },
                        singleLine = true,
                        modifier = Modifier.testTag("dialog_contact_username")
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.testTag("dialog_contact_name")
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.testTag("dialog_contact_phone")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUser = handleInput.trim().ifBlank { "contact_${(100..999).random()}" }
                        val cleanName = nameInput.trim().ifBlank { cleanUser }
                        onAddContact(
                            "usr_$cleanUser",
                            cleanUser,
                            cleanName,
                            phoneInput.trim(),
                            bioInput,
                            AvatarPalette.random()
                        )
                        showAddManualDialog = false
                    },
                    modifier = Modifier.testTag("confirm_add_contact_button")
                ) {
                    Text("Save Contact")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (reportingTarget != null) {
        val (targetUid, targetUsername) = reportingTarget!!
        var reason by remember { mutableStateOf("Spam or Suspicious Activity") }
        var details by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { reportingTarget = null },
            title = { Text("Report @$targetUsername") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Additional Details") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportUser(targetUid, targetUsername, reason, details)
                        reportingTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CriminalCrimson)
                ) {
                    Text("Submit Report", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportingTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContactCard(
    uid: String,
    username: String,
    displayName: String,
    phoneTag: String,
    bio: String,
    avatarColor: String,
    isOnline: Boolean,
    isSaved: Boolean,
    isBlocked: Boolean,
    onSaveContact: () -> Unit,
    onChat: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onToggleBlock: () -> Unit,
    onReport: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(CriminalMediaAndNotificationHelper.parseColorSafe(avatarColor))
                ) {
                    Text(
                        text = displayName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF002110),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.labelSmall,
                            color = CriminalCyan
                        )
                    }
                    if (phoneTag.isNotBlank()) {
                        Text(
                            text = phoneTag,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (bio.isNotBlank()) {
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onChat, modifier = Modifier.testTag("chat_contact_$uid")) {
                        Icon(Icons.Default.Chat, contentDescription = "Message", tint = CriminalEmerald)
                    }
                    IconButton(onClick = onVoiceCall, modifier = Modifier.testTag("voice_call_$uid")) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = CriminalCyan)
                    }
                    IconButton(onClick = onVideoCall, modifier = Modifier.testTag("video_call_$uid")) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = CriminalCyan)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isSaved) {
                        TextButton(onClick = onSaveContact) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Save", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                    IconButton(onClick = onToggleBlock) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = if (isBlocked) "Unblock" else "Block",
                            tint = if (isBlocked) CriminalCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onReport) {
                        Icon(
                            imageVector = Icons.Default.Report,
                            contentDescription = "Report Abuse",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupsHubScreen(
    currentUserId: String,
    myDisplayName: String,
    conversations: List<Conversation>,
    directoryUsers: List<DirectoryProfile>,
    savedContacts: List<Contact>,
    onBack: () -> Unit,
    onOpenGroupConversation: (Conversation) -> Unit,
    onCreateGroup: (
        title: String,
        description: String,
        avatarColor: String,
        memberUids: List<String>,
        participantNamesSummary: String
    ) -> Unit
) {
    var groupTitle by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(AvatarPalette[1]) }
    val selectedMembers = remember { mutableStateListOf<Pair<String, String>>() }

    val groupConversations = remember(conversations) {
        conversations.filter { it.isGroup }
    }

    val candidateMembers = remember(directoryUsers, savedContacts, currentUserId) {
        val map = linkedMapOf<String, String>()
        savedContacts.forEach { map[it.contactUid] = it.displayName }
        directoryUsers.forEach { if (it.userId != currentUserId) map[it.userId] = it.displayName }
        map.entries.map { it.key to it.value }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Groups & Admin Hub",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Create multi-user groups with admin privileges",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = CriminalEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Group", style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = groupTitle,
                    onValueChange = { groupTitle = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Cyber Operations Squad") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_group_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = groupDescription,
                    onValueChange = { groupDescription = it },
                    label = { Text("Group Description & Rules") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_group_desc_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (candidateMembers.isNotEmpty()) {
                    Text(
                        text = "Select Group Members:",
                        style = MaterialTheme.typography.labelSmall,
                        color = CriminalCyan
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        items(candidateMembers, key = { it.first }) { (uid, name) ->
                            val checked = selectedMembers.any { it.first == uid }
                            FilterChip(
                                selected = checked,
                                onClick = {
                                    if (checked) selectedMembers.removeAll { it.first == uid }
                                    else selectedMembers.add(uid to name)
                                },
                                label = { Text(name) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val title = groupTitle.trim().ifBlank { "CRIMINAL Squad" }
                        val memberUids = selectedMembers.map { it.first }
                        val namesSummary = (listOf("$myDisplayName (Admin)") + selectedMembers.map { it.second }).joinToString(", ")
                        onCreateGroup(
                            title,
                            groupDescription.trim(),
                            selectedColor,
                            memberUids,
                            namesSummary
                        )
                        groupTitle = ""
                        groupDescription = ""
                        selectedMembers.clear()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CriminalEmerald,
                        contentColor = Color(0xFF002110)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_create_group_button")
                ) {
                    Text("Create Group as Administrator", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ACTIVE GROUP CHANNELS (${groupConversations.size})",
            style = MaterialTheme.typography.labelSmall,
            color = CriminalEmerald
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(groupConversations, key = { it.id }) { grp ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenGroupConversation(grp) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CriminalMediaAndNotificationHelper.parseColorSafe(grp.avatarColor))
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF002110))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = grp.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Admins: ${grp.adminIds.size} • Members: ${grp.participantIds.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CriminalCyan
                            )
                            Text(
                                text = grp.participantNames,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
