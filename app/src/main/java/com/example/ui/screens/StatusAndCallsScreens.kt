package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.CallSession
import com.example.data.model.DirectoryProfile
import com.example.data.model.StatusUpdate
import com.example.data.model.UserProfile
import com.example.ui.theme.AvatarPalette
import com.example.ui.theme.CriminalCrimson
import com.example.ui.theme.CriminalCyan
import com.example.ui.theme.CriminalEmerald
import com.example.ui.theme.ObsidianBg
import com.example.util.CriminalMediaAndNotificationHelper
import kotlinx.coroutines.delay

@Composable
fun StatusStoriesScreen(
    currentUserId: String,
    myProfile: UserProfile?,
    statuses: List<StatusUpdate>,
    onPostStatus: (
        mediaType: String,
        caption: String,
        bgColorHex: String,
        mediaBase64: String?,
        visibilityScope: String
    ) -> Unit,
    onMarkStatusViewed: (StatusUpdate) -> Unit,
    onDeleteStatus: (String) -> Unit
) {
    val context = LocalContext.current
    var showCreateStatusDialog by remember { mutableStateOf(false) }
    var viewingStatus by remember { mutableStateOf<StatusUpdate?>(null) }

    val myStatuses = remember(statuses, currentUserId) {
        statuses.filter { it.userId == currentUserId }
    }
    val contactStatuses = remember(statuses, currentUserId) {
        statuses.filter { it.userId != currentUserId }
    }

    val photoStatusLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = CriminalMediaAndNotificationHelper.encodeImageUriToBase64(context, uri, maxDimension = 640)
            onPostStatus(
                "IMAGE",
                "Photo Status Update",
                "#0B0F19",
                encoded,
                "PUBLIC"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("status_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Updates & 24h Stories",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Disappears automatically after 24 hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        photoStatusLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("upload_photo_status_button")
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Photo")
                }
                Button(
                    onClick = { showCreateStatusDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CriminalEmerald,
                        contentColor = Color(0xFF002110)
                    ),
                    modifier = Modifier.testTag("create_text_status_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Post")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // My Status Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (myStatuses.isNotEmpty()) {
                        viewingStatus = myStatuses.first()
                    } else {
                        showCreateStatusDialog = true
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, CriminalEmerald, CircleShape)
                        .background(
                            CriminalMediaAndNotificationHelper.parseColorSafe(
                                myProfile?.avatarColor ?: "#00E676"
                            )
                        )
                ) {
                    Text(
                        text = (myProfile?.displayName ?: "Me").take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF002110),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (myStatuses.isEmpty()) "Tap to add a 24-hour status update"
                        else "${myStatuses.size} active story • ${myStatuses.first().viewerUids.size} viewer(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "RECENT UPDATES (${statuses.size})",
            style = MaterialTheme.typography.labelSmall,
            color = CriminalCyan
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (statuses.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp)
            ) {
                Text(
                    text = "No active statuses in the last 24 hours. Post the first story!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(statuses, key = { it.id }) { status ->
                    val hoursRemaining = ((status.expiresAtMillis - System.currentTimeMillis()) / (1000 * 60 * 60)).coerceAtLeast(1)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMarkStatusViewed(status)
                                viewingStatus = status
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, CriminalCyan, CircleShape)
                                    .background(CriminalMediaAndNotificationHelper.parseColorSafe(status.userAvatarColor))
                            ) {
                                Text(
                                    text = status.userDisplayName.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF002110),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = status.userDisplayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = status.caption.ifBlank { status.mediaType },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Expires in ${hoursRemaining}h • ${status.viewerUids.size} views",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CriminalEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateStatusDialog) {
        var caption by remember { mutableStateOf("") }
        var selectedBg by remember { mutableStateOf("#0B0F19") }
        var visibility by remember { mutableStateOf("PUBLIC") }

        AlertDialog(
            onDismissRequest = { showCreateStatusDialog = false },
            title = { Text("Post 24-Hour Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("What's happening?") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("status_caption_input")
                    )

                    Text("Background Theme:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("#0B0F19", "#004D40", "#0D47A1", "#4A148C", "#880E4F")) { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(CriminalMediaAndNotificationHelper.parseColorSafe(hex))
                                    .border(
                                        width = if (selectedBg == hex) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedBg = hex }
                            )
                        }
                    }

                    Text("Privacy:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PUBLIC", "CONTACTS").forEach { scope ->
                            FilterChip(
                                selected = visibility == scope,
                                onClick = { visibility = scope },
                                label = { Text(scope) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (caption.isNotBlank()) {
                            onPostStatus("TEXT", caption.trim(), selectedBg, null, visibility)
                            showCreateStatusDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_status_button")
                ) {
                    Text("Publish Story")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateStatusDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full-Screen Story Viewer
    if (viewingStatus != null) {
        val story = viewingStatus!!
        var progress by remember(story.id) { mutableFloatStateOf(0f) }

        LaunchedEffect(story.id) {
            progress = 0f
            for (step in 1..50) {
                delay(100L)
                progress = step / 50f
            }
            viewingStatus = null
        }

        Dialog(
            onDismissRequest = { viewingStatus = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CriminalMediaAndNotificationHelper.parseColorSafe(story.backgroundColorHex))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = CriminalEmerald,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = story.userDisplayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "@${story.username} • ${CriminalMediaAndNotificationHelper.formatMessageTime(story.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CriminalEmerald
                            )
                        }
                        Row {
                            if (story.userId == currentUserId) {
                                IconButton(onClick = {
                                    onDeleteStatus(story.id)
                                    viewingStatus = null
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Story", tint = CriminalCrimson)
                                }
                            }
                            IconButton(onClick = { viewingStatus = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Story", tint = Color.White)
                            }
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val bmp = remember(story.mediaBase64) {
                            CriminalMediaAndNotificationHelper.decodeBase64ToImageBitmap(story.mediaBase64)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Story Image",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(340.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            Text(
                                text = story.caption,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Viewers List Footer
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(Icons.Default.RemoveRedEye, contentDescription = "Viewers", tint = CriminalEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Viewed by (${story.viewerNames.size}): ${story.viewerNames.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallsHistoryScreen(
    currentUserId: String,
    calls: List<CallSession>,
    directoryUsers: List<DirectoryProfile>,
    onStartCall: (callType: String, peerUid: String, peerName: String, peerColor: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("calls_screen")
    ) {
        Text(
            text = "Voice & Video Calls",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "WebRTC STUN/TURN real-time call signaling & call logs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Dial Directory Strip
        if (directoryUsers.isNotEmpty()) {
            Text(
                text = "QUICK CALL CONTACTS",
                style = MaterialTheme.typography.labelSmall,
                color = CriminalEmerald
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(directoryUsers, key = { it.userId }) { user ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = user.displayName, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onStartCall("VOICE", user.userId, user.displayName, user.avatarColor) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = CriminalEmerald)
                            }
                            IconButton(
                                onClick = { onStartCall("VIDEO", user.userId, user.displayName, user.avatarColor) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = CriminalCyan)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "CALL HISTORY (${calls.size})",
            style = MaterialTheme.typography.labelSmall,
            color = CriminalCyan
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (calls.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Text(
                    text = "No recent voice or video calls yet. Tap a contact above to start a real-time call!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(calls, key = { it.id }) { call ->
                    val isOutgoing = call.callerId == currentUserId
                    val peerName = if (isOutgoing) call.receiverName else call.callerName
                    val peerUid = if (isOutgoing) call.receiverId else call.callerId
                    val peerColor = if (isOutgoing) call.receiverAvatarColor else call.callerAvatarColor
                    val isMissed = call.status in listOf("MISSED", "DECLINED")

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(CriminalMediaAndNotificationHelper.parseColorSafe(peerColor))
                            ) {
                                Text(
                                    text = peerName.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF002110),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = peerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isMissed) CriminalCrimson else MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when {
                                            isMissed -> Icons.AutoMirrored.Filled.CallMissed
                                            isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                                            else -> Icons.AutoMirrored.Filled.CallReceived
                                        },
                                        contentDescription = null,
                                        tint = if (isMissed) CriminalCrimson else CriminalEmerald,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${call.callType} • ${call.status} (${CriminalMediaAndNotificationHelper.formatCallDuration(call.durationSeconds)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = CriminalMediaAndNotificationHelper.formatMessageTime(call.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onStartCall(call.callType, peerUid, peerName, peerColor) }
                            ) {
                                Icon(
                                    imageVector = if (call.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = "Redial",
                                    tint = CriminalEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveCallOverlayScreen(
    currentUserId: String,
    callSession: CallSession,
    onAcceptCall: () -> Unit,
    onEndOrDeclineCall: (finalStatus: String, durationSec: Long) -> Unit,
    onToggleMute: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val isIncomingRinging = callSession.receiverId == currentUserId && callSession.status == "RINGING"
    val peerName = if (callSession.callerId == currentUserId) callSession.receiverName else callSession.callerName
    val peerColor = if (callSession.callerId == currentUserId) callSession.receiverAvatarColor else callSession.callerAvatarColor

    var elapsedSeconds by remember(callSession.id) { mutableLongStateOf(callSession.durationSeconds) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var showSdpDetails by remember { mutableStateOf(false) }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(callSession.id) {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (callSession.callType == "VIDEO") perms.add(Manifest.permission.CAMERA)
        val missing = perms.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing) {
            mediaPermissionLauncher.launch(perms.toTypedArray())
        }
    }

    LaunchedEffect(callSession.status) {
        while (true) {
            delay(1000L)
            if (callSession.status == "CONNECTED" || callSession.callerId == currentUserId) {
                elapsedSeconds += 1L
            }
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(ObsidianBg, Color(0xFF0E1D34), ObsidianBg)
                    )
                )
                .padding(24.dp)
                .testTag("active_call_overlay")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CRIMINAL APP • ${callSession.callType} CALL",
                        style = MaterialTheme.typography.labelSmall,
                        color = CriminalEmerald
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isIncomingRinging) "Incoming ${callSession.callType} Call..."
                        else "${callSession.status} • ${CriminalMediaAndNotificationHelper.formatCallDuration(elapsedSeconds)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = CriminalCyan
                    )
                }

                // Avatar / Video Feed Preview
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(if (callSession.callType == "VIDEO" && callSession.isCameraOn) 210.dp else 140.dp)
                            .clip(if (callSession.callType == "VIDEO") RoundedCornerShape(28.dp) else CircleShape)
                            .border(3.dp, CriminalEmerald, if (callSession.callType == "VIDEO") RoundedCornerShape(28.dp) else CircleShape)
                            .background(CriminalMediaAndNotificationHelper.parseColorSafe(peerColor))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = peerName.take(2).uppercase(),
                                style = MaterialTheme.typography.displayLarge,
                                color = Color(0xFF002110),
                                fontWeight = FontWeight.Bold
                            )
                            if (callSession.callType == "VIDEO" && callSession.isCameraOn) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "LIVE HD STREAM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF002110)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(onClick = { showSdpDetails = !showSdpDetails }) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = CriminalCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showSdpDetails) "Hide WebRTC Signaling" else "Inspect WebRTC SDP & STUN",
                            color = CriminalCyan
                        )
                    }

                    AnimatedVisibility(visible = showSdpDetails) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Text(
                                text = callSession.sdpOffer,
                                style = MaterialTheme.typography.labelSmall,
                                color = CriminalEmerald,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Call Controls
                if (isIncomingRinging) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Button(
                            onClick = { onEndOrDeclineCall("DECLINED", 0L) },
                            colors = ButtonDefaults.buttonColors(containerColor = CriminalCrimson),
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White)
                        }

                        Button(
                            onClick = onAcceptCall,
                            colors = ButtonDefaults.buttonColors(containerColor = CriminalEmerald),
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color(0xFF002110))
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        IconButton(
                            onClick = { onToggleMute(!callSession.isMuted) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (callSession.isMuted) CriminalCrimson else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (callSession.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Toggle Mute",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { isSpeakerOn = !isSpeakerOn },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isSpeakerOn) CriminalCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speakerphone",
                                tint = Color.White
                            )
                        }

                        if (callSession.callType == "VIDEO") {
                            IconButton(
                                onClick = { onToggleCamera(!callSession.isCameraOn) },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (!callSession.isCameraOn) CriminalCrimson else Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = if (callSession.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Toggle Camera",
                                    tint = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = { onEndOrDeclineCall("ENDED", elapsedSeconds) },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CriminalCrimson)
                                .testTag("end_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
