package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.ui.theme.CriminalCrimson
import com.example.ui.theme.CriminalCyan
import com.example.ui.theme.CriminalEmerald
import com.example.util.CriminalMediaAndNotificationHelper

@Composable
fun ProfileAndSettingsScreen(
    myProfile: UserProfile?,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onSaveProfile: (
        username: String,
        displayName: String,
        bio: String,
        phoneNumber: String,
        avatarColor: String,
        avatarBase64: String?,
        privacyPhoto: String,
        privacyLastSeen: String,
        privacyStatus: String,
        notificationsSound: Boolean,
        notificationsVibrate: Boolean,
        themeMode: String
    ) -> Unit,
    onOpenPrivacyScreen: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    var username by remember(myProfile) { mutableStateOf(myProfile?.username ?: "criminal_agent") }
    var displayName by remember(myProfile) { mutableStateOf(myProfile?.displayName ?: "Agent") }
    var bio by remember(myProfile) { mutableStateOf(myProfile?.bio ?: "Hey there! I am using CRIMINAL APP.") }
    var phoneNumber by remember(myProfile) { mutableStateOf(myProfile?.phoneNumber ?: "") }
    var avatarColor by remember(myProfile) { mutableStateOf(myProfile?.avatarColor ?: "#00E676") }
    var avatarBase64 by remember(myProfile) { mutableStateOf(myProfile?.avatarBase64) }
    var soundEnabled by remember(myProfile) { mutableStateOf(myProfile?.notificationsSound ?: true) }
    var vibrateEnabled by remember(myProfile) { mutableStateOf(myProfile?.notificationsVibrate ?: true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = CriminalMediaAndNotificationHelper.encodeImageUriToBase64(context, uri, maxDimension = 320)
            if (encoded != null) avatarBase64 = encoded
        }
    }

    val decodedAvatar = remember(avatarBase64) {
        CriminalMediaAndNotificationHelper.decodeBase64ToImageBitmap(avatarBase64)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Profile & Settings",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Manage your identity, appearance, and notifications",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .size(92.dp)
                        .clickable {
                            photoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(CriminalMediaAndNotificationHelper.parseColorSafe(avatarColor))
                            .border(2.5.dp, CriminalEmerald, CircleShape)
                    ) {
                        if (decodedAvatar != null) {
                            Image(
                                bitmap = decodedAvatar,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = displayName.take(2).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFF002110),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, CriminalEmerald, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = CriminalEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().replace(" ", "_") },
                    label = { Text("Unique @username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio / About") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onSaveProfile(
                            username,
                            displayName,
                            bio,
                            phoneNumber,
                            avatarColor,
                            avatarBase64,
                            myProfile?.privacyPhoto ?: "EVERYONE",
                            myProfile?.privacyLastSeen ?: "EVERYONE",
                            myProfile?.privacyStatus ?: "EVERYONE",
                            soundEnabled,
                            vibrateEnabled,
                            if (isDarkTheme) "DARK" else "LIGHT"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CriminalEmerald,
                        contentColor = Color(0xFF002110)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_profile_settings_button")
                ) {
                    Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme & Notification Preferences Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = CriminalCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Premium Dark Mode", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Default obsidian theme with light mode support",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { dark ->
                            onToggleTheme(dark)
                        },
                        modifier = Modifier.testTag("theme_mode_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = CriminalEmerald)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Notification Sound", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Play alert tone for new messages & calls",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = CriminalCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Haptic Vibration", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Vibrate on incoming messages and calls",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = vibrateEnabled,
                        onCheckedChange = { vibrateEnabled = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy & Security Navigation Card
        OutlinedButton(
            onClick = onOpenPrivacyScreen,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("open_privacy_security_button")
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = CriminalEmerald)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Privacy & Security Settings")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logout & Delete Account
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out")
            }

            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = CriminalCrimson),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("delete_account_button")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Account", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Developer Credits Footer
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "CRIMINAL APP • Official Release",
                    style = MaterialTheme.typography.labelLarge,
                    color = CriminalEmerald
                )
                Text(
                    text = "Designed & Developed by Madusanka",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Permanently?") },
            text = {
                Text("This will permanently remove your CRIMINAL APP profile and directory handle from Cloud Firestore and sign you out.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CriminalCrimson)
                ) {
                    Text("Confirm Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PrivacyAndSecurityScreen(
    myProfile: UserProfile?,
    onBack: () -> Unit,
    onSavePrivacy: (privacyPhoto: String, privacyLastSeen: String, privacyStatus: String) -> Unit,
    onUnblockUser: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    var photoVisibility by remember(myProfile) { mutableStateOf(myProfile?.privacyPhoto ?: "EVERYONE") }
    var lastSeenVisibility by remember(myProfile) { mutableStateOf(myProfile?.privacyLastSeen ?: "EVERYONE") }
    var statusVisibility by remember(myProfile) { mutableStateOf(myProfile?.privacyStatus ?: "EVERYONE") }
    val blockedUsers = myProfile?.blockedUserIds ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("privacy_security_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Privacy & Security",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Zero-trust access controls & visibility settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Who can see my Profile Photo", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EVERYONE", "CONTACTS", "NOBODY").forEach { option ->
                        FilterChip(
                            selected = photoVisibility == option,
                            onClick = { photoVisibility = option },
                            label = { Text(option) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Who can see my Last Seen & Online", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EVERYONE", "CONTACTS", "NOBODY").forEach { option ->
                        FilterChip(
                            selected = lastSeenVisibility == option,
                            onClick = { lastSeenVisibility = option },
                            label = { Text(option) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Who can see my Bio & Status Updates", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EVERYONE", "CONTACTS", "NOBODY").forEach { option ->
                        FilterChip(
                            selected = statusVisibility == option,
                            onClick = { statusVisibility = option },
                            label = { Text(option) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSavePrivacy(photoVisibility, lastSeenVisibility, statusVisibility)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CriminalEmerald,
                        contentColor = Color(0xFF002110)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_privacy_settings_button")
                ) {
                    Text("Apply Privacy Rules", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Blocked Accounts Section
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Blocked Users (${blockedUsers.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (blockedUsers.isEmpty()) {
                    Text(
                        text = "No blocked accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    blockedUsers.forEach { blockedUid ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = blockedUid, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onUnblockUser(blockedUid) }) {
                                Text("Unblock", color = CriminalEmerald)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Audit Verification Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CriminalEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zero-Trust Firestore Access Control", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Private Conversations: Protected by participantIds verification on every read & write.\n" +
                        "• PII Isolation: Private profile fields are restricted strictly to request.auth.uid == userId.\n" +
                        "• Transport Security: TLS 1.3 + Google Identity Credential Manager token verification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
