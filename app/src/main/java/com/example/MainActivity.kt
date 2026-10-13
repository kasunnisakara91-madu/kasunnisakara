package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WebStories
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.ui.screens.ActiveCallOverlayScreen
import com.example.ui.screens.CallsHistoryScreen
import com.example.ui.screens.ContactsDirectoryScreen
import com.example.ui.screens.CriminalAuthScreen
import com.example.ui.screens.CriminalSplashScreen
import com.example.ui.screens.GroupsHubScreen
import com.example.ui.screens.IndividualChatScreen
import com.example.ui.screens.MainChatsScreen
import com.example.ui.screens.PrivacyAndSecurityScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.StatusStoriesScreen
import com.example.ui.theme.CriminalCyan
import com.example.ui.theme.CriminalEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CriminalAppViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.CriminalMediaAndNotificationHelper
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CriminalMediaAndNotificationHelper.ensureNotificationChannels(this)
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                CriminalRootApp(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = it }
                )
            }
        }
    }
}

@Composable
fun CriminalRootApp(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    if (showSplash) {
        CriminalSplashScreen(onSplashFinished = { showSplash = false })
        return
    }

    val user = currentUser
    if (user == null) {
        CriminalAuthScreen(onAuthSuccess = {})
    } else {
        AuthenticatedCriminalApp(
            currentUserId = user.uid,
            userEmailOrName = user.displayName ?: user.email ?: "Agent",
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme
        )
    }
}

private enum class SubScreen {
    NONE,
    PROFILE_SETUP,
    INDIVIDUAL_CHAT,
    CONTACTS_DIRECTORY,
    GROUPS_HUB,
    PRIVACY_SECURITY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedCriminalApp(
    currentUserId: String,
    userEmailOrName: String,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: CriminalAppViewModel = viewModel(
        key = currentUserId,
        factory = CriminalAppViewModel.factory(currentUserId)
    )

    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val directoryState by viewModel.directoryState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val statusesState by viewModel.statusesState.collectAsStateWithLifecycle()
    val callsState by viewModel.callsState.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val selectedConvId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val activeCallId by viewModel.activeCallSessionId.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var bottomTab by rememberSaveable { mutableIntStateOf(0) } // 0: Chats, 1: Updates, 2: Calls, 3: Settings
    var activeSubScreen by rememberSaveable { mutableStateOf(SubScreen.NONE) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission on Android 13+
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Track online status
    DisposableEffect(currentUserId) {
        viewModel.updatePresence(true)
        onDispose {
            viewModel.updatePresence(false)
        }
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBannerMessage()
        }
    }

    val myProfile = (myProfileState as? UiState.Success)?.data
    val conversations = (conversationsState as? UiState.Success)?.data ?: emptyList()
    val directoryUsers = (directoryState as? UiState.Success)?.data ?: emptyList()
    val savedContacts = (contactsState as? UiState.Success)?.data ?: emptyList()
    val statuses = (statusesState as? UiState.Success)?.data ?: emptyList()
    val calls = (callsState as? UiState.Success)?.data ?: emptyList()
    val activeMessages = (activeMessagesState as? UiState.Success)?.data ?: emptyList()

    // Sync theme preference from profile when loaded
    LaunchedEffect(myProfile?.themeMode) {
        when (myProfile?.themeMode) {
            "LIGHT" -> if (isDarkTheme) onToggleTheme(false)
            "DARK" -> if (!isDarkTheme) onToggleTheme(true)
        }
    }

    // Detect incoming RINGING call or active call session
    val activeCallSession = remember(calls, activeCallId, currentUserId) {
        calls.firstOrNull { it.id == activeCallId && it.status in listOf("RINGING", "CONNECTED") }
            ?: calls.firstOrNull { it.receiverId == currentUserId && it.status == "RINGING" }
    }

    LaunchedEffect(activeCallSession?.id) {
        if (activeCallSession != null && activeCallSession.receiverId == currentUserId && activeCallSession.status == "RINGING") {
            CriminalMediaAndNotificationHelper.showIncomingCallNotification(
                context = context,
                callerName = activeCallSession.callerName,
                callType = activeCallSession.callType,
                enableVibrate = myProfile?.notificationsVibrate ?: true
            )
        }
    }

    // If profile hasn't been created yet, show Profile Setup screen
    if (myProfileState is UiState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CriminalEmerald)
        }
        return
    }

    if (myProfileState is UiState.Success && myProfile == null) {
        ProfileSetupScreen(
            initialEmailOrName = userEmailOrName,
            existingProfile = null,
            isSaving = isSaving,
            onSaveProfile = { username, displayName, bio, phone, avatarColor, avatarBase64 ->
                viewModel.saveUserProfile(
                    username = username,
                    displayName = displayName,
                    bio = bio,
                    phoneNumber = phone,
                    avatarColor = avatarColor,
                    avatarBase64 = avatarBase64
                )
            }
        )
        return
    }

    val myDisplayName = myProfile?.displayName?.ifBlank { userEmailOrName } ?: userEmailOrName
    val myAvatarColor = myProfile?.avatarColor ?: "#00E676"

    // Sub-screens handling with BackHandler
    when (activeSubScreen) {
        SubScreen.INDIVIDUAL_CHAT -> {
            val currentConv = conversations.firstOrNull { it.id == selectedConvId }
            if (currentConv != null) {
                IndividualChatScreen(
                    currentUserId = currentUserId,
                    myDisplayName = myDisplayName,
                    conversation = currentConv,
                    allConversations = conversations,
                    messages = activeMessages,
                    directoryUsers = directoryUsers,
                    onBack = {
                        viewModel.selectConversation(null)
                        activeSubScreen = SubScreen.NONE
                    },
                    onSendMessage = { text, type, mediaB64, mediaName, duration, replyId, replySender, replyPreview, forwarded ->
                        viewModel.sendMessage(
                            conversationId = currentConv.id,
                            senderName = myDisplayName,
                            participantIds = currentConv.participantIds,
                            text = text,
                            messageType = type,
                            mediaBase64 = mediaB64,
                            mediaName = mediaName,
                            mediaDurationSec = duration,
                            replyToMessageId = replyId,
                            replyToSenderName = replySender,
                            replyToPreview = replyPreview,
                            isForwarded = forwarded
                        )
                    },
                    onForwardMessageToConversation = { targetConv, msg ->
                        viewModel.sendMessage(
                            conversationId = targetConv.id,
                            senderName = myDisplayName,
                            participantIds = targetConv.participantIds,
                            text = msg.text,
                            messageType = msg.messageType,
                            mediaBase64 = msg.mediaBase64,
                            mediaName = msg.mediaName,
                            mediaDurationSec = msg.mediaDurationSec,
                            isForwarded = true
                        )
                    },
                    onEditMessage = { msgId, newText ->
                        viewModel.editMessage(currentConv.id, msgId, newText)
                    },
                    onDeleteMessage = { msgId ->
                        viewModel.deleteMessage(currentConv.id, msgId)
                    },
                    onMarkMessageRead = { msg ->
                        viewModel.markMessageAsRead(currentConv.id, msg)
                    },
                    onTyping = {
                        viewModel.onUserTyping(currentConv.id, myDisplayName)
                    },
                    onStartCall = { callType, peerUid, peerName, peerColor ->
                        viewModel.startCall(
                            callerName = myDisplayName,
                            callerAvatarColor = myAvatarColor,
                            receiverId = peerUid,
                            receiverName = peerName,
                            receiverAvatarColor = peerColor,
                            callType = callType,
                            onCallCreated = {}
                        )
                    },
                    onPromoteAdmin = { targetUid ->
                        viewModel.promoteToGroupAdmin(currentConv.id, currentConv.adminIds, targetUid)
                    }
                )
                if (activeCallSession != null) {
                    ActiveCallOverlayWrapper(
                        currentUserId = currentUserId,
                        callSession = activeCallSession,
                        viewModel = viewModel
                    )
                }
                return
            } else {
                activeSubScreen = SubScreen.NONE
            }
        }

        SubScreen.CONTACTS_DIRECTORY -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    ContactsDirectoryScreen(
                        currentUserId = currentUserId,
                        myProfile = myProfile,
                        directoryUsers = directoryUsers,
                        savedContacts = savedContacts,
                        onBack = { activeSubScreen = SubScreen.NONE },
                        onAddContact = { uid, uName, dName, phone, bio, color ->
                            viewModel.addOrSaveContact(uid, uName, dName, phone, bio, color)
                        },
                        onStartChatWithUser = { peerUid, peerName, peerColor ->
                            viewModel.startDirectChat(
                                myDisplayName = myDisplayName,
                                peerUid = peerUid,
                                peerDisplayName = peerName,
                                peerAvatarColor = peerColor,
                                onReady = {
                                    activeSubScreen = SubScreen.INDIVIDUAL_CHAT
                                }
                            )
                        },
                        onStartCallWithUser = { peerUid, peerName, peerColor, callType ->
                            viewModel.startCall(
                                callerName = myDisplayName,
                                callerAvatarColor = myAvatarColor,
                                receiverId = peerUid,
                                receiverName = peerName,
                                receiverAvatarColor = peerColor,
                                callType = callType,
                                onCallCreated = {}
                            )
                        },
                        onToggleBlockUser = { targetUid ->
                            if (myProfile != null) {
                                viewModel.toggleBlockUser(targetUid, myProfile)
                            }
                        },
                        onReportUser = { targetUid, targetUsername, reason, details ->
                            viewModel.reportAbusiveUser(targetUid, targetUsername, reason, details)
                        }
                    )
                }
            }
            if (activeCallSession != null) {
                ActiveCallOverlayWrapper(currentUserId, activeCallSession, viewModel)
            }
            return
        }

        SubScreen.GROUPS_HUB -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    GroupsHubScreen(
                        currentUserId = currentUserId,
                        myDisplayName = myDisplayName,
                        conversations = conversations,
                        directoryUsers = directoryUsers,
                        savedContacts = savedContacts,
                        onBack = { activeSubScreen = SubScreen.NONE },
                        onOpenGroupConversation = { grp ->
                            viewModel.selectConversation(grp.id)
                            activeSubScreen = SubScreen.INDIVIDUAL_CHAT
                        },
                        onCreateGroup = { title, desc, color, memberUids, namesSummary ->
                            viewModel.createGroupChat(
                                groupTitle = title,
                                groupDescription = desc,
                                avatarColor = color,
                                memberUids = memberUids,
                                participantNamesSummary = namesSummary,
                                creatorDisplayName = myDisplayName,
                                onReady = {
                                    activeSubScreen = SubScreen.INDIVIDUAL_CHAT
                                }
                            )
                        }
                    )
                }
            }
            if (activeCallSession != null) {
                ActiveCallOverlayWrapper(currentUserId, activeCallSession, viewModel)
            }
            return
        }

        SubScreen.PRIVACY_SECURITY -> {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    PrivacyAndSecurityScreen(
                        myProfile = myProfile,
                        onBack = { activeSubScreen = SubScreen.NONE },
                        onSavePrivacy = { photoVis, lastSeenVis, statusVis ->
                            if (myProfile != null) {
                                viewModel.saveUserProfile(
                                    username = myProfile.username,
                                    displayName = myProfile.displayName,
                                    bio = myProfile.bio,
                                    phoneNumber = myProfile.phoneNumber,
                                    avatarColor = myProfile.avatarColor,
                                    avatarBase64 = myProfile.avatarBase64,
                                    privacyPhoto = photoVis,
                                    privacyLastSeen = lastSeenVis,
                                    privacyStatus = statusVis,
                                    notificationsSound = myProfile.notificationsSound,
                                    notificationsVibrate = myProfile.notificationsVibrate,
                                    themeMode = myProfile.themeMode,
                                    blockedUserIds = myProfile.blockedUserIds
                                )
                            }
                        },
                        onUnblockUser = { targetUid ->
                            if (myProfile != null) {
                                viewModel.toggleBlockUser(targetUid, myProfile)
                            }
                        }
                    )
                }
            }
            return
        }

        else -> {}
    }

    // Main Scaffold with TopAppBar and Bottom Navigation Bar (Chats, Updates, Calls, Settings)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_criminal_logo_1791649939055),
                            contentDescription = "CRIMINAL APP Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CRIMINAL APP",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "@${myProfile?.username ?: "online"} • By Madusanka",
                                style = MaterialTheme.typography.labelSmall,
                                color = CriminalEmerald
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { activeSubScreen = SubScreen.CONTACTS_DIRECTORY },
                        modifier = Modifier.testTag("top_contacts_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Contacts", tint = CriminalEmerald)
                    }
                    IconButton(
                        onClick = { activeSubScreen = SubScreen.GROUPS_HUB },
                        modifier = Modifier.testTag("top_groups_button")
                    ) {
                        Icon(Icons.Default.Group, contentDescription = "Groups", tint = CriminalCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = bottomTab == 0,
                    onClick = { bottomTab = 0 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                    label = { Text("Chats") },
                    modifier = Modifier.testTag("nav_tab_chats")
                )
                NavigationBarItem(
                    selected = bottomTab == 1,
                    onClick = { bottomTab = 1 },
                    icon = { Icon(Icons.Default.WebStories, contentDescription = "Updates") },
                    label = { Text("Updates") },
                    modifier = Modifier.testTag("nav_tab_updates")
                )
                NavigationBarItem(
                    selected = bottomTab == 2,
                    onClick = { bottomTab = 2 },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                    label = { Text("Calls") },
                    modifier = Modifier.testTag("nav_tab_calls")
                )
                NavigationBarItem(
                    selected = bottomTab == 3,
                    onClick = { bottomTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (bottomTab) {
                0 -> MainChatsScreen(
                    currentUserId = currentUserId,
                    myProfile = myProfile,
                    conversations = conversations,
                    directoryUsers = directoryUsers,
                    onConversationClick = { conv ->
                        viewModel.selectConversation(conv.id)
                        activeSubScreen = SubScreen.INDIVIDUAL_CHAT
                    },
                    onOpenContactsScreen = { activeSubScreen = SubScreen.CONTACTS_DIRECTORY },
                    onOpenGroupsScreen = { activeSubScreen = SubScreen.GROUPS_HUB },
                    onStartSelfNotesChat = {
                        viewModel.startDirectChat(
                            myDisplayName = myDisplayName,
                            peerUid = currentUserId,
                            peerDisplayName = "$myDisplayName (Personal Vault)",
                            peerAvatarColor = myAvatarColor,
                            onReady = {
                                activeSubScreen = SubScreen.INDIVIDUAL_CHAT
                            }
                        )
                    }
                )

                1 -> StatusStoriesScreen(
                    currentUserId = currentUserId,
                    myProfile = myProfile,
                    statuses = statuses,
                    onPostStatus = { mediaType, caption, bgColor, mediaB64, scopeVis ->
                        viewModel.postStatus(
                            username = myProfile?.username ?: "agent",
                            userDisplayName = myDisplayName,
                            userAvatarColor = myAvatarColor,
                            mediaType = mediaType,
                            caption = caption,
                            backgroundColorHex = bgColor,
                            mediaBase64 = mediaB64,
                            visibilityScope = scopeVis
                        )
                    },
                    onMarkStatusViewed = { status ->
                        viewModel.markStatusViewed(status, myDisplayName)
                    },
                    onDeleteStatus = { statusId ->
                        viewModel.deleteStatus(statusId)
                    }
                )

                2 -> CallsHistoryScreen(
                    currentUserId = currentUserId,
                    calls = calls,
                    directoryUsers = directoryUsers,
                    onStartCall = { callType, peerUid, peerName, peerColor ->
                        viewModel.startCall(
                            callerName = myDisplayName,
                            callerAvatarColor = myAvatarColor,
                            receiverId = peerUid,
                            receiverName = peerName,
                            receiverAvatarColor = peerColor,
                            callType = callType,
                            onCallCreated = {}
                        )
                    }
                )

                3 -> ProfileAndSettingsScreen(
                    myProfile = myProfile,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { dark ->
                        onToggleTheme(dark)
                        if (myProfile != null) {
                            viewModel.saveUserProfile(
                                username = myProfile.username,
                                displayName = myProfile.displayName,
                                bio = myProfile.bio,
                                phoneNumber = myProfile.phoneNumber,
                                avatarColor = myProfile.avatarColor,
                                avatarBase64 = myProfile.avatarBase64,
                                privacyPhoto = myProfile.privacyPhoto,
                                privacyLastSeen = myProfile.privacyLastSeen,
                                privacyStatus = myProfile.privacyStatus,
                                notificationsSound = myProfile.notificationsSound,
                                notificationsVibrate = myProfile.notificationsVibrate,
                                themeMode = if (dark) "DARK" else "LIGHT",
                                blockedUserIds = myProfile.blockedUserIds
                            )
                        }
                    },
                    onSaveProfile = { uName, dName, bio, phone, color, b64, pPhoto, pLastSeen, pStatus, nSound, nVib, tMode ->
                        viewModel.saveUserProfile(
                            username = uName,
                            displayName = dName,
                            bio = bio,
                            phoneNumber = phone,
                            avatarColor = color,
                            avatarBase64 = b64,
                            privacyPhoto = pPhoto,
                            privacyLastSeen = pLastSeen,
                            privacyStatus = pStatus,
                            notificationsSound = nSound,
                            notificationsVibrate = nVib,
                            themeMode = tMode,
                            blockedUserIds = myProfile?.blockedUserIds ?: emptyList()
                        )
                    },
                    onOpenPrivacyScreen = { activeSubScreen = SubScreen.PRIVACY_SECURITY },
                    onSignOut = {
                        viewModel.updatePresence(false)
                        signOutUser(
                            credentialManager = credentialManager,
                            onSignOutComplete = {},
                            scope = scope
                        )
                    },
                    onDeleteAccount = {
                        viewModel.deleteAccountData {
                            signOutUser(
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        }
                    }
                )
            }
        }
    }

    if (activeCallSession != null) {
        ActiveCallOverlayWrapper(
            currentUserId = currentUserId,
            callSession = activeCallSession,
            viewModel = viewModel
        )
    }
}

@Composable
private fun ActiveCallOverlayWrapper(
    currentUserId: String,
    callSession: com.example.data.model.CallSession,
    viewModel: CriminalAppViewModel
) {
    ActiveCallOverlayScreen(
        currentUserId = currentUserId,
        callSession = callSession,
        onAcceptCall = {
            val sdpAnswer = CriminalMediaAndNotificationHelper.buildRealisticWebRtcSdpOffer(
                callType = callSession.callType,
                callerUid = currentUserId
            )
            viewModel.setActiveCallSessionId(callSession.id)
            viewModel.updateCallState(
                callId = callSession.id,
                status = "CONNECTED",
                sdpAnswer = sdpAnswer
            )
        },
        onEndOrDeclineCall = { finalStatus, durationSec ->
            viewModel.updateCallState(
                callId = callSession.id,
                status = finalStatus,
                durationSeconds = durationSec
            )
        },
        onToggleMute = { muted ->
            viewModel.updateCallState(
                callId = callSession.id,
                status = callSession.status,
                isMuted = muted
            )
        },
        onToggleCamera = { cameraOn ->
            viewModel.updateCallState(
                callId = callSession.id,
                status = callSession.status,
                isCameraOn = cameraOn
            )
        }
    )
}
