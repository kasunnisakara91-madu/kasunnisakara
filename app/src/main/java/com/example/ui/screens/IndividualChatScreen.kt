package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.DirectoryProfile
import com.example.ui.theme.BubbleIncomingDark
import com.example.ui.theme.BubbleOutgoingDark
import com.example.ui.theme.CriminalAmber
import com.example.ui.theme.CriminalCrimson
import com.example.ui.theme.CriminalCyan
import com.example.ui.theme.CriminalEmerald
import com.example.util.CriminalMediaAndNotificationHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualChatScreen(
    currentUserId: String,
    myDisplayName: String,
    conversation: Conversation,
    allConversations: List<Conversation>,
    messages: List<ChatMessage>,
    directoryUsers: List<DirectoryProfile>,
    onBack: () -> Unit,
    onSendMessage: (
        text: String,
        messageType: String,
        mediaBase64: String?,
        mediaName: String?,
        mediaDurationSec: Long?,
        replyToMessageId: String?,
        replyToSenderName: String?,
        replyToPreview: String?,
        isForwarded: Boolean
    ) -> Unit,
    onForwardMessageToConversation: (targetConv: Conversation, message: ChatMessage) -> Unit,
    onEditMessage: (messageId: String, newText: String) -> Unit,
    onDeleteMessage: (messageId: String) -> Unit,
    onMarkMessageRead: (ChatMessage) -> Unit,
    onTyping: () -> Unit,
    onStartCall: (callType: String, peerUid: String, peerName: String, peerColor: String) -> Unit,
    onPromoteAdmin: (targetUid: String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var messageInput by remember { mutableStateOf("") }
    var searchInChat by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var showEmojiBar by remember { mutableStateOf(false) }
    var showGroupInfoDialog by remember { mutableStateOf(false) }

    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessageForActions by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // Voice Recording state
    var isRecordingVoice by remember { mutableStateOf(false) }
    var voiceSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            voiceSeconds = 0L
            while (isRecordingVoice) {
                delay(1000L)
                voiceSeconds += 1L
            }
        }
    }

    // Mark unread incoming messages as READ automatically
    LaunchedEffect(messages) {
        messages.forEach { msg ->
            if (msg.senderId != currentUserId && currentUserId !in msg.readByUids) {
                onMarkMessageRead(msg)
            }
        }
    }

    val peerUid = remember(conversation, currentUserId) {
        conversation.participantIds.firstOrNull { it != currentUserId } ?: currentUserId
    }
    val peerDir = remember(directoryUsers, peerUid) {
        directoryUsers.firstOrNull { it.userId == peerUid }
    }
    val chatTitle = remember(conversation, peerDir) {
        if (conversation.isGroup) conversation.title
        else peerDir?.displayName?.ifBlank { conversation.title } ?: conversation.title
    }
    val subtitleText = remember(conversation, peerDir, currentUserId) {
        when {
            conversation.typingUserId.isNotBlank() && conversation.typingUserId != currentUserId ->
                "${conversation.typingUserName.ifBlank { "Someone" }} is typing..."
            conversation.isGroup ->
                "${conversation.participantIds.size} members • ${conversation.adminIds.size} admin(s)"
            peerDir?.isOnline == true -> "Online"
            peerDir?.lastSeenText?.isNotBlank() == true -> peerDir.lastSeenText
            else -> "Encrypted Direct Channel"
        }
    }

    // Launchers for Image, Video, Document, and Audio Permission
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = CriminalMediaAndNotificationHelper.encodeImageUriToBase64(context, uri, maxDimension = 540)
            onSendMessage(
                messageInput.ifBlank { "Shared Photo" },
                "IMAGE",
                encoded,
                "photo_${System.currentTimeMillis()}.jpg",
                null,
                replyingTo?.id,
                replyingTo?.senderName,
                replyingTo?.text,
                false
            )
            messageInput = ""
            replyingTo = null
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = CriminalMediaAndNotificationHelper.resolveDocumentDisplayName(context, uri)
            onSendMessage(
                messageInput.ifBlank { "Video Clip: $fileName" },
                "VIDEO",
                null,
                fileName,
                15L,
                replyingTo?.id,
                replyingTo?.senderName,
                replyingTo?.text,
                false
            )
            messageInput = ""
            replyingTo = null
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val docName = CriminalMediaAndNotificationHelper.resolveDocumentDisplayName(context, uri)
            onSendMessage(
                messageInput.ifBlank { "Shared Document: $docName" },
                "DOCUMENT",
                null,
                docName,
                null,
                replyingTo?.id,
                replyingTo?.senderName,
                replyingTo?.text,
                false
            )
            messageInput = ""
            replyingTo = null
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isRecordingVoice = true
        if (!isGranted) {
            // Still allow recording voice note metadata if emulator lacks physical mic grant
        }
    }

    val filteredMessages = remember(messages, searchInChat) {
        if (searchInChat.isBlank()) messages
        else messages.filter {
            it.text.contains(searchInChat, ignoreCase = true) ||
                it.senderName.contains(searchInChat, ignoreCase = true)
        }
    }

    val groupedByDate = remember(filteredMessages) {
        filteredMessages.groupBy { CriminalMediaAndNotificationHelper.formatDateSeparator(it.createdAt) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("individual_chat_screen")
    ) {
        // Chat Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CriminalMediaAndNotificationHelper.parseColorSafe(conversation.avatarColor))
                            .clickable { if (conversation.isGroup) showGroupInfoDialog = true }
                    ) {
                        Text(
                            text = chatTitle.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF002110),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { if (conversation.isGroup) showGroupInfoDialog = true }
                    ) {
                        Text(
                            text = chatTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (subtitleText.contains("typing") || subtitleText == "Online") CriminalEmerald
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onStartCall("VOICE", peerUid, chatTitle, conversation.avatarColor) },
                        modifier = Modifier.testTag("chat_voice_call_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = CriminalEmerald)
                    }

                    IconButton(
                        onClick = { onStartCall("VIDEO", peerUid, chatTitle, conversation.avatarColor) },
                        modifier = Modifier.testTag("chat_video_call_button")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = CriminalCyan)
                    }

                    IconButton(
                        onClick = { showSearchBar = !showSearchBar },
                        modifier = Modifier.testTag("chat_search_toggle_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search in chat")
                    }

                    if (conversation.isGroup) {
                        IconButton(onClick = { showGroupInfoDialog = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Group Info")
                        }
                    }
                }

                AnimatedVisibility(visible = showSearchBar) {
                    OutlinedTextField(
                        value = searchInChat,
                        onValueChange = { searchInChat = it },
                        placeholder = { Text("Search messages in this chat...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                searchInChat = ""
                                showSearchBar = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Messages List with Date Separators
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedByDate.forEach { (dateLabel, dayMessages) ->
                item(key = "date_$dateLabel") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                items(dayMessages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        isMine = msg.senderId == currentUserId,
                        isGroup = conversation.isGroup,
                        isSenderAdmin = msg.senderId in conversation.adminIds,
                        onMessageClick = { selectedMessageForActions = msg }
                    )
                }
            }
        }

        // Reply / Edit Banner
        AnimatedVisibility(visible = replyingTo != null || editingMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = CriminalEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editingMessage != null) "Editing Message"
                            else "Replying to ${replyingTo?.senderName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CriminalEmerald
                        )
                        Text(
                            text = editingMessage?.text ?: replyingTo?.text.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = {
                        replyingTo = null
                        editingMessage = null
                        messageInput = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            }
        }

        // Quick Emoji Bar
        AnimatedVisibility(visible = showEmojiBar) {
            val emojis = listOf("😀", "😂", "🔥", "❤️", "👍", "🚀", "⚡", "🛡️", "🎉", "💯", "😎", "🤝")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                items(emojis) { emoji ->
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .clickable { messageInput += emoji }
                            .padding(4.dp)
                    )
                }
            }
        }

        // Voice Recording Active Bar
        AnimatedVisibility(visible = isRecordingVoice) {
            Surface(
                color = CriminalCrimson.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = CriminalCrimson)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recording Voice Message... ${CriminalMediaAndNotificationHelper.formatCallDuration(voiceSeconds)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = CriminalCrimson
                        )
                    }
                    Row {
                        TextButton(onClick = { isRecordingVoice = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val duration = voiceSeconds.coerceAtLeast(1L)
                                isRecordingVoice = false
                                onSendMessage(
                                    "Voice message (${duration}s)",
                                    "VOICE",
                                    null,
                                    "voice_${System.currentTimeMillis()}.m4a",
                                    duration,
                                    replyingTo?.id,
                                    replyingTo?.senderName,
                                    replyingTo?.text,
                                    false
                                )
                                replyingTo = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CriminalEmerald,
                                contentColor = Color(0xFF002110)
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Send Voice")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Voice")
                        }
                    }
                }
            }
        }

        // Composer Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                // Media Attachment Quick Strip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { showEmojiBar = !showEmojiBar }) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = "Emojis", tint = CriminalAmber)
                    }
                    IconButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("attach_image_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Attach Photo", tint = CriminalEmerald)
                    }
                    IconButton(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.testTag("attach_video_button")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Attach Video", tint = CriminalCyan)
                    }
                    IconButton(
                        onClick = {
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.testTag("attach_document_button")
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach Document", tint = CriminalCyan)
                    }
                    IconButton(
                        onClick = {
                            val hasMic = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMic) {
                                isRecordingVoice = !isRecordingVoice
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.testTag("record_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Message",
                            tint = if (isRecordingVoice) CriminalCrimson else CriminalEmerald
                        )
                    }

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = {
                            messageInput = it
                            if (it.isNotBlank()) onTyping()
                        },
                        placeholder = { Text("Message...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    IconButton(
                        onClick = {
                            val trimmed = messageInput.trim()
                            if (trimmed.isEmpty()) return@IconButton
                            val editing = editingMessage
                            if (editing != null) {
                                onEditMessage(editing.id, trimmed)
                                editingMessage = null
                                messageInput = ""
                            } else {
                                onSendMessage(
                                    trimmed,
                                    "TEXT",
                                    null,
                                    null,
                                    null,
                                    replyingTo?.id,
                                    replyingTo?.senderName,
                                    replyingTo?.text,
                                    false
                                )
                                messageInput = ""
                                replyingTo = null
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CriminalEmerald)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = Color(0xFF002110)
                        )
                    }
                }
            }
        }
    }

    // Message Actions Sheet (Reply, Forward, Copy, Edit, Delete)
    if (selectedMessageForActions != null) {
        val msg = selectedMessageForActions!!
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForActions = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Message Actions",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        replyingTo = msg
                        selectedMessageForActions = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reply to Message")
                }

                OutlinedButton(
                    onClick = {
                        forwardingMessage = msg
                        selectedMessageForActions = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Forward Message")
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("CRIMINAL APP Message", msg.text))
                        selectedMessageForActions = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Message Text")
                }

                if (msg.senderId == currentUserId && !msg.isDeleted) {
                    OutlinedButton(
                        onClick = {
                            editingMessage = msg
                            messageInput = msg.text
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Message")
                    }

                    Button(
                        onClick = {
                            onDeleteMessage(msg.id)
                            selectedMessageForActions = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CriminalCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete for Everyone", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Forward Message Dialog
    if (forwardingMessage != null) {
        val msgToForward = forwardingMessage!!
        AlertDialog(
            onDismissRequest = { forwardingMessage = null },
            title = { Text("Forward Message To...") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allConversations, key = { it.id }) { targetConv ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onForwardMessageToConversation(targetConv, msgToForward)
                                    forwardingMessage = null
                                }
                        ) {
                            Text(
                                text = targetConv.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { forwardingMessage = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Group Info & Admin Management Dialog
    if (showGroupInfoDialog && conversation.isGroup) {
        val isMeAdmin = currentUserId in conversation.adminIds
        AlertDialog(
            onDismissRequest = { showGroupInfoDialog = false },
            title = { Text(conversation.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = conversation.description.ifBlank { "Group encrypted channel" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Group Members (${conversation.participantIds.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        color = CriminalEmerald
                    )
                    conversation.participantIds.forEach { memberUid ->
                        val memberName = directoryUsers.firstOrNull { it.userId == memberUid }?.displayName
                            ?: if (memberUid == currentUserId) "$myDisplayName (You)" else memberUid
                        val isMemberAdmin = memberUid in conversation.adminIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = memberName, style = MaterialTheme.typography.bodyMedium)
                            if (isMemberAdmin) {
                                Text(
                                    text = "ADMIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CriminalCyan
                                )
                            } else if (isMeAdmin) {
                                TextButton(onClick = { onPromoteAdmin(memberUid) }) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Make Admin")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupInfoDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    isGroup: Boolean,
    isSenderAdmin: Boolean,
    onMessageClick: () -> Unit
) {
    var isVoicePlaying by remember { mutableStateOf(false) }

    LaunchedEffect(isVoicePlaying) {
        if (isVoicePlaying) {
            delay((message.mediaDurationSec ?: 3L) * 1000L)
            isVoicePlaying = false
        }
    }

    Row(
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) BubbleOutgoingDark else BubbleIncomingDark
            ),
            modifier = Modifier
                .widthIn(min = 110.dp, max = 310.dp)
                .clickable(onClick = onMessageClick)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Sender Header in Groups
                if (isGroup && !isMine) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelLarge,
                            color = CriminalEmerald
                        )
                        if (isSenderAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ADMIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = CriminalCyan
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Forwarded Indicator
                if (message.isForwarded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Forward,
                            contentDescription = "Forwarded",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Forwarded",
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Reply Preview Box
                if (!message.replyToPreview.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = message.replyToSenderName ?: "Reply",
                                style = MaterialTheme.typography.labelSmall,
                                color = CriminalCyan
                            )
                            Text(
                                text = message.replyToPreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Media Attachments (IMAGE, VIDEO, DOCUMENT, VOICE)
                when (message.messageType) {
                    "IMAGE" -> {
                        val bmp = remember(message.mediaBase64) {
                            CriminalMediaAndNotificationHelper.decodeBase64ToImageBitmap(message.mediaBase64)
                        }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Shared Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    "VIDEO" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = CriminalCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.mediaName ?: "Video Clip.mp4",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    "DOCUMENT" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = CriminalAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.mediaName ?: "Document.pdf",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    "VOICE" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.28f))
                                .padding(8.dp)
                        ) {
                            IconButton(
                                onClick = { isVoicePlaying = !isVoicePlaying },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play Voice",
                                    tint = CriminalEmerald
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isVoicePlaying) CriminalEmerald else CriminalCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${message.mediaDurationSec ?: 3}s Audio",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Message Text
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.isDeleted) Color(0xFF94A3B8) else Color.White,
                    fontStyle = if (message.isDeleted) FontStyle.Italic else FontStyle.Normal
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp, Edited label, and Delivery/Read Receipts
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (message.isEdited && !message.isDeleted) {
                        Text(
                            text = "edited • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    Text(
                        text = CriminalMediaAndNotificationHelper.formatMessageTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val isRead = message.deliveryStatus == "READ" || message.readByUids.size > 1
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = if (isRead) "Read" else "Delivered",
                            tint = if (isRead) CriminalCyan else Color(0xFFCBD5E1),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
